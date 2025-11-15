package command

import (
	"fmt"
	"strings"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

type AddAppleAppCommand struct {
	BaseCommand
	adminRepo    *repository.AdminRepository
	groupRepo    *repository.GroupRepository
	appleScraper *apple.AppleScraper
}

func NewAddAppleAppCommand(
	cfg *config.Config,
	adminRepo *repository.AdminRepository,
	groupRepo *repository.GroupRepository,
	appleScraper *apple.AppleScraper,
) *AddAppleAppCommand {
	return &AddAppleAppCommand{
		BaseCommand:  BaseCommand{cfg: cfg},
		adminRepo:    adminRepo,
		groupRepo:    groupRepo,
		appleScraper: appleScraper,
	}
}

func (c *AddAppleAppCommand) Execute(message *tgbotapi.Message) string {
	if !c.requireAdmin(message) {
		return "You are not authorized to use this command."
	}

	groupID := message.Chat.ID
	hasGroup, err := c.adminRepo.HasGroup(groupID)
	if err != nil {
		return fmt.Sprintf("Failed to check group: %v", err)
	}
	if !hasGroup {
		return "This group is not registered. Please use /addgroup first."
	}

	args := strings.Fields(message.CommandArguments())
	if len(args) == 0 {
		return "Usage: /addapple <appId> [country]\nExample: /addapple com.example.app vn"
	}

	appID := args[0]
	country := "vn"
	if len(args) > 1 {
		country = args[1]
	}

	// Verify app exists
	app, err := c.appleScraper.GetApp(appID, country)
	if err != nil {
		return fmt.Sprintf("Failed to fetch app from store: %v", err)
	}

	if err := c.groupRepo.AddAppleApp(groupID, appID, country); err != nil {
		return fmt.Sprintf("Failed to add app: %v", err)
	}

	return fmt.Sprintf("Apple app added successfully:\n*%s*\nApp ID: %s\nCountry: %s\nScore: %.1f", app.Title, appID, country, app.Score)
}
