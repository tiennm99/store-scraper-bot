package scheduler

import (
	"context"
	"fmt"
	"time"

	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/bot"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"github.com/miti99/store-scraper-bot-go/internal/util"
	"github.com/robfig/cron/v3"
	"go.uber.org/zap"
)

type Scheduler struct {
	cron          *cron.Cron
	cfg           *config.Config
	bot           *bot.Bot
	adminRepo     *repository.AdminRepository
	groupRepo     *repository.GroupRepository
	appleScraper  *apple.AppleScraper
	googleScraper *google.GoogleScraper
	logger        *zap.Logger
}

func NewScheduler(
	cfg *config.Config,
	bot *bot.Bot,
	adminRepo *repository.AdminRepository,
	groupRepo *repository.GroupRepository,
	appleScraper *apple.AppleScraper,
	googleScraper *google.GoogleScraper,
) *Scheduler {
	// Create cron with Vietnam timezone
	c := cron.New(cron.WithLocation(cfg.VietnamLocation))

	return &Scheduler{
		cron:          c,
		cfg:           cfg,
		bot:           bot,
		adminRepo:     adminRepo,
		groupRepo:     groupRepo,
		appleScraper:  appleScraper,
		googleScraper: googleScraper,
		logger:        cfg.Logger,
	}
}

func (s *Scheduler) Start() error {
	// Schedule daily check at configured time (default: 7:00 AM Vietnam time)
	_, err := s.cron.AddFunc(s.cfg.ScheduleCheckAppTime, s.runDailyCheck)
	if err != nil {
		return fmt.Errorf("failed to schedule daily check: %w", err)
	}

	s.logger.Info("Scheduler started",
		zap.String("schedule", s.cfg.ScheduleCheckAppTime),
		zap.String("timezone", s.cfg.VietnamLocation.String()))

	s.cron.Start()
	return nil
}

func (s *Scheduler) Stop() {
	s.cron.Stop()
	s.logger.Info("Scheduler stopped")
}

func (s *Scheduler) runDailyCheck() {
	s.logger.Info("Running daily check job")

	now := time.Now().In(s.cfg.VietnamLocation)

	// Check if today is weekend (Saturday or Sunday)
	isWeekend := now.Weekday() == time.Saturday || now.Weekday() == time.Sunday

	groups, err := s.adminRepo.GetAllGroups()
	if err != nil {
		s.logger.Error("Failed to get groups for daily check", zap.Error(err))
		return
	}

	for _, groupID := range groups {
		s.checkGroup(groupID, isWeekend)
	}

	s.logger.Info("Daily check job completed", zap.Int("groupsChecked", len(groups)))
}

func (s *Scheduler) checkGroup(groupID int64, isWeekend bool) {
	ctx, cancel := context.WithTimeout(context.Background(), 120*time.Second)
	defer cancel()

	group, err := s.groupRepo.Get(ctx, groupID)
	if err != nil {
		s.logger.Error("Failed to get group", zap.Int64("groupId", groupID), zap.Error(err))
		return
	}

	if len(group.AppleApps) == 0 && len(group.GoogleApps) == 0 {
		s.logger.Info("Group has no apps, skipping", zap.Int64("groupId", groupID))
		return
	}

	nonUpdatedApps := make([]model.NonUpdatedApp, 0)
	now := time.Now().In(s.cfg.VietnamLocation)

	// Check Apple apps
	for _, appInfo := range group.AppleApps {
		app, err := s.appleScraper.GetApp(appInfo.AppID, appInfo.Country)
		if err != nil {
			s.logger.Error("Failed to fetch Apple app",
				zap.Int64("groupId", groupID),
				zap.String("appId", appInfo.AppID),
				zap.Error(err))
			continue
		}

		updatedTime, err := time.Parse(time.RFC3339, app.Updated)
		if err != nil {
			s.logger.Error("Failed to parse update time",
				zap.Int64("groupId", groupID),
				zap.String("appId", appInfo.AppID),
				zap.String("updated", app.Updated),
				zap.Error(err))
			continue
		}

		daysSinceUpdate := int(now.Sub(updatedTime).Hours() / 24)
		if daysSinceUpdate > s.cfg.NumDaysWarningNotUpdated {
			nonUpdatedApps = append(nonUpdatedApps, model.NonUpdatedApp{
				AppID:   appInfo.AppID,
				Title:   app.Title,
				Days:    daysSinceUpdate,
				Updated: app.Updated[:10],
				Score:   app.Score,
				Reviews: app.Reviews,
				Ratings: app.Ratings,
				IsApple: true,
			})
		}
	}

	// Check Google apps
	for _, appInfo := range group.GoogleApps {
		app, err := s.googleScraper.GetApp(appInfo.AppID, appInfo.Country)
		if err != nil {
			s.logger.Error("Failed to fetch Google app",
				zap.Int64("groupId", groupID),
				zap.String("appId", appInfo.AppID),
				zap.Error(err))
			continue
		}

		updatedTime := time.UnixMilli(app.Updated)
		daysSinceUpdate := int(now.Sub(updatedTime).Hours() / 24)

		if daysSinceUpdate > s.cfg.NumDaysWarningNotUpdated {
			nonUpdatedApps = append(nonUpdatedApps, model.NonUpdatedApp{
				AppID:   appInfo.AppID,
				Title:   app.Title,
				Days:    daysSinceUpdate,
				Updated: updatedTime.Format("2006-01-02"),
				Score:   app.Score,
				Reviews: app.Reviews,
				Ratings: app.Ratings,
				IsApple: false,
			})
		}
	}

	// Send report
	if len(nonUpdatedApps) == 0 {
		s.logger.Info("No non-updated apps found for group", zap.Int64("groupId", groupID))
		return
	}

	message := s.buildReport(groupID, nonUpdatedApps)

	var err2 error
	if isWeekend {
		err2 = s.bot.SendMessageSilent(groupID, message)
	} else {
		err2 = s.bot.SendMessage(groupID, message)
	}

	if err2 != nil {
		s.logger.Error("Failed to send daily check report",
			zap.Int64("groupId", groupID),
			zap.Error(err2))
	} else {
		s.logger.Info("Daily check report sent",
			zap.Int64("groupId", groupID),
			zap.Int("nonUpdatedApps", len(nonUpdatedApps)),
			zap.Bool("silent", isWeekend))
	}
}

func (s *Scheduler) buildReport(groupID int64, nonUpdatedApps []model.NonUpdatedApp) string {
	var rows [][]string
	for _, app := range nonUpdatedApps {
		store := "Google"
		if app.IsApple {
			store = "Apple"
		}

		rows = append(rows, []string{
			util.TruncateString(app.Title, 30),
			store,
			fmt.Sprintf("%d", app.Days),
			app.Updated,
			fmt.Sprintf("%.1f", app.Score),
			fmt.Sprintf("%v", app.Reviews),
			util.FormatNumber(app.Ratings),
		})
	}

	headers := []string{"App", "Store", "Days", "Updated", "Score", "Reviews", "Ratings"}
	table := util.BuildTable(headers, rows)

	now := time.Now().In(s.cfg.VietnamLocation)
	return fmt.Sprintf("*Daily App Check Report*\nDate: %s\nGroup: %d\nApps not updated in >%d days: *%d*\n\n%s",
		now.Format("2006-01-02 15:04"),
		groupID,
		s.cfg.NumDaysWarningNotUpdated,
		len(nonUpdatedApps),
		table)
}
