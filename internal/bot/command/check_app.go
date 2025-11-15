package command

import (
	"context"
	"fmt"
	"strings"
	"time"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"github.com/miti99/store-scraper-bot-go/internal/util"
	"go.uber.org/zap"
)

type CheckAppCommand struct {
	BaseCommand
	adminRepo     *repository.AdminRepository
	groupRepo     *repository.GroupRepository
	appleScraper  *apple.AppleScraper
	googleScraper *google.GoogleScraper
}

func NewCheckAppCommand(
	cfg *config.Config,
	adminRepo *repository.AdminRepository,
	groupRepo *repository.GroupRepository,
	appleScraper *apple.AppleScraper,
	googleScraper *google.GoogleScraper,
) *CheckAppCommand {
	return &CheckAppCommand{
		BaseCommand:   BaseCommand{cfg: cfg},
		adminRepo:     adminRepo,
		groupRepo:     groupRepo,
		appleScraper:  appleScraper,
		googleScraper: googleScraper,
	}
}

func (c *CheckAppCommand) Execute(message *tgbotapi.Message) string {
	if !c.requireAdmin(message) {
		return "You are not authorized to use this command."
	}

	groupID := message.Chat.ID
	hasGroup, err := c.adminRepo.HasGroup(groupID)
	if err != nil {
		return fmt.Sprintf("Failed to check group: %v", err)
	}
	if !hasGroup {
		return "This group is not registered."
	}

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	group, err := c.groupRepo.Get(ctx, groupID)
	if err != nil {
		return fmt.Sprintf("Failed to get group: %v", err)
	}

	if len(group.AppleApps) == 0 && len(group.GoogleApps) == 0 {
		return "No apps in this group."
	}

	nonUpdatedApps := make([]model.NonUpdatedApp, 0)
	now := time.Now().In(c.cfg.VietnamLocation)

	// Check Apple apps
	for _, appInfo := range group.AppleApps {
		app, err := c.appleScraper.GetApp(appInfo.AppID, appInfo.Country)
		if err != nil {
			c.cfg.Logger.Error("Failed to fetch Apple app",
				zap.String("appId", appInfo.AppID),
				zap.Error(err))
			continue
		}

		updatedTime, err := time.Parse(time.RFC3339, app.Updated)
		if err != nil {
			c.cfg.Logger.Error("Failed to parse update time",
				zap.String("appId", appInfo.AppID),
				zap.String("updated", app.Updated),
				zap.Error(err))
			continue
		}

		daysSinceUpdate := int(now.Sub(updatedTime).Hours() / 24)
		if daysSinceUpdate > c.cfg.NumDaysWarningNotUpdated {
			nonUpdatedApps = append(nonUpdatedApps, model.NonUpdatedApp{
				AppID:   appInfo.AppID,
				Title:   app.Title,
				Days:    daysSinceUpdate,
				Updated: app.Updated[:10], // Just the date part
				Score:   app.Score,
				Reviews: app.Reviews,
				Ratings: app.Ratings,
				IsApple: true,
			})
		}
	}

	// Check Google apps
	for _, appInfo := range group.GoogleApps {
		app, err := c.googleScraper.GetApp(appInfo.AppID, appInfo.Country)
		if err != nil {
			c.cfg.Logger.Error("Failed to fetch Google app",
				zap.String("appId", appInfo.AppID),
				zap.Error(err))
			continue
		}

		updatedTime := time.UnixMilli(app.Updated)
		daysSinceUpdate := int(now.Sub(updatedTime).Hours() / 24)

		if daysSinceUpdate > c.cfg.NumDaysWarningNotUpdated {
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

	if len(nonUpdatedApps) == 0 {
		return fmt.Sprintf("All apps are up to date (checked within %d days).", c.cfg.NumDaysWarningNotUpdated)
	}

	// Build table
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

	var sb strings.Builder
	sb.WriteString(fmt.Sprintf("*Non-Updated Apps Report*\nGroup: %d\nApps not updated in >%d days: *%d*\n\n",
		groupID, c.cfg.NumDaysWarningNotUpdated, len(nonUpdatedApps)))
	sb.WriteString(table)

	return sb.String()
}
