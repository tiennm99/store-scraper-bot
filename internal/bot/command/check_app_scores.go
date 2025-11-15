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
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"github.com/miti99/store-scraper-bot-go/internal/util"
)

type CheckAppScoresCommand struct {
	BaseCommand
	adminRepo     *repository.AdminRepository
	groupRepo     *repository.GroupRepository
	appleScraper  *apple.AppleScraper
	googleScraper *google.GoogleScraper
}

func NewCheckAppScoresCommand(
	cfg *config.Config,
	adminRepo *repository.AdminRepository,
	groupRepo *repository.GroupRepository,
	appleScraper *apple.AppleScraper,
	googleScraper *google.GoogleScraper,
) *CheckAppScoresCommand {
	return &CheckAppScoresCommand{
		BaseCommand:   BaseCommand{cfg: cfg},
		adminRepo:     adminRepo,
		groupRepo:     groupRepo,
		appleScraper:  appleScraper,
		googleScraper: googleScraper,
	}
}

func (c *CheckAppScoresCommand) Execute(message *tgbotapi.Message) string {
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

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	group, err := c.groupRepo.Get(ctx, groupID)
	if err != nil {
		return fmt.Sprintf("Failed to get group: %v", err)
	}

	if len(group.AppleApps) == 0 && len(group.GoogleApps) == 0 {
		return "No apps in this group."
	}

	var rows [][]string

	// Check Apple apps
	for _, appInfo := range group.AppleApps {
		app, err := c.appleScraper.GetApp(appInfo.AppID, appInfo.Country)
		if err != nil {
			rows = append(rows, []string{
				util.TruncateString(appInfo.AppID, 30),
				"Apple",
				"Error",
				"0",
				"0",
			})
			continue
		}

		rows = append(rows, []string{
			util.TruncateString(app.Title, 30),
			"Apple",
			fmt.Sprintf("%.1f", app.Score),
			fmt.Sprintf("%d", app.Reviews),
			util.FormatNumber(app.Ratings),
		})
	}

	// Check Google apps
	for _, appInfo := range group.GoogleApps {
		app, err := c.googleScraper.GetApp(appInfo.AppID, appInfo.Country)
		if err != nil {
			rows = append(rows, []string{
				util.TruncateString(appInfo.AppID, 30),
				"Google",
				"Error",
				"0",
				"0",
			})
			continue
		}

		rows = append(rows, []string{
			util.TruncateString(app.Title, 30),
			"Google",
			fmt.Sprintf("%.1f", app.Score),
			fmt.Sprintf("%d", app.Reviews),
			util.FormatNumber(app.Ratings),
		})
	}

	headers := []string{"App", "Store", "Score", "Reviews", "Ratings"}
	table := util.BuildTable(headers, rows)

	var sb strings.Builder
	sb.WriteString(fmt.Sprintf("*App Scores Report*\nGroup: %d\n\n", groupID))
	sb.WriteString(table)

	return sb.String()
}
