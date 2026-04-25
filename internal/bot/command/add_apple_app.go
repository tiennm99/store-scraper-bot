package command

import (
	"fmt"
	"strconv"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple/request"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// /addapple <id|appId> [country=vn] — Java AddAppleAppCommand.
type AddAppleAppCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
	scraper   *apple.AppleScraper
}

func NewAddAppleAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository, scraper *apple.AppleScraper) *AddAppleAppCommand {
	return &AddAppleAppCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo, scraper: scraper}
}

func (c *AddAppleAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !authorizeGroup(msg.Chat.ID, c.adminRepo, sender) {
		return
	}
	args := splitArgs(msg.CommandArguments())
	if len(args) < 1 || len(args) > 2 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	country := "vn"
	if len(args) == 2 {
		country = args[1]
	}

	// Java: try parsing arg[0] as Long (trackId); else treat as bundleId.
	var req request.AppleAppRequest
	if trackID, err := strconv.ParseInt(args[0], 10, 64); err == nil {
		req = request.ByTrackID(trackID, country)
	} else {
		req = request.ByBundleID(args[0], country)
	}

	resp, err := c.scraper.FetchAndCache(req)
	if err != nil || resp == nil || resp.AppID == "" {
		_ = sender.SendMessage(msg.Chat.ID, "Error when request app info")
		return
	}

	added, err := c.groupRepo.AddAppleApp(msg.Chat.ID, resp.AppID, country)
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}
	if !added {
		_ = sender.SendMessage(msg.Chat.ID, fmt.Sprintf("Apple app <code>%s</code> is already added", resp.AppID))
		return
	}
	_ = sender.SendMessage(msg.Chat.ID, fmt.Sprintf("Apple app <code>%s</code>, country <b>%s</b> added successfully", resp.AppID, country))
}
