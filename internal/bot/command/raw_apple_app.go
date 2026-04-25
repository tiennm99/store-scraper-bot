package command

import (
	"fmt"
	"strconv"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple/request"
	"github.com/miti99/store-scraper-bot-go/internal/config"
)

// /rawappleapp <id|appId> [country=vn] — Java RawAppleAppCommand.
// Sends the raw upstream JSON as a Telegram document attachment.
type RawAppleAppCommand struct {
	cfg     *config.Config
	scraper *apple.AppleScraper
}

func NewRawAppleAppCommand(cfg *config.Config, scraper *apple.AppleScraper) *RawAppleAppCommand {
	return &RawAppleAppCommand{cfg: cfg, scraper: scraper}
}

func (c *RawAppleAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	args := splitArgs(msg.CommandArguments())
	if len(args) < 1 || len(args) > 2 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	country := "vn"
	if len(args) == 2 {
		country = args[1]
	}

	var req request.AppleAppRequest
	if trackID, err := strconv.ParseInt(args[0], 10, 64); err == nil {
		req = request.ByTrackID(trackID, country)
	} else {
		req = request.ByBundleID(args[0], country)
	}

	raw, err := c.scraper.RawApp(req)
	if err != nil || raw == "" {
		_ = sender.SendMessage(msg.Chat.ID, "Error when request app info")
		return
	}
	_ = sender.SendDocument(msg.Chat.ID, fmt.Sprintf("%s.json", args[0]), raw)
}
