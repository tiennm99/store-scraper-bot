package command

import (
	"encoding/json"
	"fmt"
	"strings"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/config"
)

type RawGoogleAppCommand struct {
	BaseCommand
	googleScraper *google.GoogleScraper
}

func NewRawGoogleAppCommand(cfg *config.Config, googleScraper *google.GoogleScraper) *RawGoogleAppCommand {
	return &RawGoogleAppCommand{
		BaseCommand:   BaseCommand{cfg: cfg},
		googleScraper: googleScraper,
	}
}

func (c *RawGoogleAppCommand) Execute(message *tgbotapi.Message) string {
	if !c.requireAdmin(message) {
		return "You are not authorized to use this command."
	}

	args := strings.Fields(message.CommandArguments())
	if len(args) == 0 {
		return "Usage: /rawgoogle <appId> [country]\nExample: /rawgoogle com.example.app vn"
	}

	appID := args[0]
	country := "vn"
	if len(args) > 1 {
		country = args[1]
	}

	app, err := c.googleScraper.GetApp(appID, country)
	if err != nil {
		return fmt.Sprintf("Failed to fetch app: %v", err)
	}

	jsonData, err := json.MarshalIndent(app, "", "  ")
	if err != nil {
		return fmt.Sprintf("Failed to marshal JSON: %v", err)
	}

	// Telegram has a message size limit, so we might need to truncate
	jsonStr := string(jsonData)
	if len(jsonStr) > 4000 {
		jsonStr = jsonStr[:4000] + "\n...(truncated)"
	}

	return fmt.Sprintf("```json\n%s\n```", jsonStr)
}
