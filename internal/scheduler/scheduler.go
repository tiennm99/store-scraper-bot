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
	if _, err := s.cron.AddFunc(s.cfg.ScheduleCheckAppTime, s.runDailyCheck); err != nil {
		return fmt.Errorf("schedule daily check: %w", err)
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
	now := time.Now().In(s.cfg.VietnamLocation)
	silent := now.Weekday() == time.Saturday || now.Weekday() == time.Sunday
	s.logger.Info("Running daily check job", zap.Bool("silent", silent))

	groups, err := s.adminRepo.GetAllGroups()
	if err != nil {
		s.logger.Error("Failed to get groups", zap.Error(err))
		return
	}
	for _, gid := range groups {
		s.checkGroup(gid, silent, now)
	}
	s.logger.Info("Daily check job completed", zap.Int("groupsChecked", len(groups)))
}

func (s *Scheduler) checkGroup(groupID int64, silent bool, now time.Time) {
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

	threshold := s.cfg.NumDaysWarningNotUpdated
	stale := make([]model.NonUpdatedApp, 0)

	for _, info := range group.AppleApps {
		app, err := s.appleScraper.GetApp(info.AppID, info.Country)
		if err != nil || app == nil {
			s.logger.Error("Apple fetch failed", zap.String("appId", info.AppID), zap.Error(err))
			continue
		}
		updatedTime, err := time.Parse(time.RFC3339, app.Updated)
		if err != nil {
			continue
		}
		days := int(now.Sub(updatedTime).Hours() / 24)
		if days > threshold {
			stale = append(stale, model.NonUpdatedApp{
				AppID:   info.AppID,
				Title:   app.Title,
				Days:    days,
				Updated: updatedTime.Format("2006-01-02"),
				Score:   app.Score,
				Reviews: int64(app.Reviews),
				Ratings: app.Ratings,
				IsApple: true,
			})
		}
	}

	for _, info := range group.GoogleApps {
		app, err := s.googleScraper.GetApp(info.AppID, info.Country)
		if err != nil || app == nil {
			s.logger.Error("Google fetch failed", zap.String("appId", info.AppID), zap.Error(err))
			continue
		}
		updatedTime := time.UnixMilli(app.Updated)
		days := int(now.Sub(updatedTime).Hours() / 24)
		if days > threshold {
			stale = append(stale, model.NonUpdatedApp{
				AppID:   info.AppID,
				Title:   app.Title,
				Days:    days,
				Updated: updatedTime.Format("2006-01-02"),
				Score:   app.Score,
				Reviews: app.Reviews,
				Ratings: app.Ratings,
				IsApple: false,
			})
		}
	}

	if len(stale) == 0 {
		s.logger.Info("All apps up-to-date", zap.Int64("groupId", groupID))
		return
	}
	message := s.buildReport(groupID, stale, now)
	var sendErr error
	if silent {
		sendErr = s.bot.SendMessageSilent(groupID, message)
	} else {
		sendErr = s.bot.SendMessage(groupID, message)
	}
	if sendErr != nil {
		s.logger.Error("Send daily report failed", zap.Int64("groupId", groupID), zap.Error(sendErr))
	}
}

func (s *Scheduler) buildReport(groupID int64, apps []model.NonUpdatedApp, now time.Time) string {
	headers := []string{"App", "Store", "Days", "Updated", "Score", "Reviews", "Ratings"}
	rows := make([][]string, 0, len(apps))
	for _, a := range apps {
		store := "Google"
		if a.IsApple {
			store = "Apple"
		}
		rows = append(rows, []string{
			util.TruncateString(a.Title, 30),
			store,
			fmt.Sprintf("%d", a.Days),
			a.Updated,
			fmt.Sprintf("%.1f", a.Score),
			fmt.Sprintf("%d", a.Reviews),
			util.FormatNumber(a.Ratings),
		})
	}
	return fmt.Sprintf(
		"<b>Daily App Check Report</b>\nDate: %s\nGroup: <code>%d</code>\nApps not updated in &gt;%d days: <b>%d</b>\n\n<pre>%s</pre>",
		now.Format("2006-01-02 15:04"),
		groupID,
		s.cfg.NumDaysWarningNotUpdated,
		len(apps),
		util.BuildTable(headers, rows),
	)
}
