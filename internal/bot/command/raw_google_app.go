package command

import (
	"fmt"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/api/google/request"
	"github.com/miti99/store-scraper-bot-go/internal/config"
)

// /rawgoogleapp <appId> [country=vn] — Java RawGoogleAppCommand.
// Sends raw upstream JSON as a Telegram document.
type RawGoogleAppCommand struct {
	cfg     *config.Config
	scraper *google.GoogleScraper
}

func NewRawGoogleAppCommand(cfg *config.Config, scraper *google.GoogleScraper) *RawGoogleAppCommand {
	return &RawGoogleAppCommand{cfg: cfg, scraper: scraper}
}

func (c *RawGoogleAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	args := splitArgs(msg.CommandArguments())
	if len(args) < 1 || len(args) > 2 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	appID := args[0]
	country := "vn"
	if len(args) == 2 {
		country = args[1]
	}
	raw, err := c.scraper.RawApp(request.New(appID, country))
	if err != nil || raw == "" {
		_ = sender.SendMessage(msg.Chat.ID, "Error when request app info")
		return
	}
	_ = sender.SendDocument(msg.Chat.ID, fmt.Sprintf("%s.json", appID), raw)
}
