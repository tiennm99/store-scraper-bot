package command

import (
	"fmt"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/api/google/request"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// /addgoogle <appId> [country=vn] — Java AddGoogleAppCommand.
type AddGoogleAppCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
	scraper   *google.GoogleScraper
}

func NewAddGoogleAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository, scraper *google.GoogleScraper) *AddGoogleAppCommand {
	return &AddGoogleAppCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo, scraper: scraper}
}

func (c *AddGoogleAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !authorizeGroup(msg.Chat.ID, c.adminRepo, sender) {
		return
	}
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
	resp, err := c.scraper.FetchAndCache(request.New(appID, country))
	if err != nil || resp == nil {
		_ = sender.SendMessage(msg.Chat.ID, "Error when request app info")
		return
	}
	added, err := c.groupRepo.AddGoogleApp(msg.Chat.ID, appID, country)
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}
	if !added {
		_ = sender.SendMessage(msg.Chat.ID, fmt.Sprintf("Google app <code>%s</code> is already added", appID))
		return
	}
	_ = sender.SendMessage(msg.Chat.ID, fmt.Sprintf("Google app <code>%s</code>, country <b>%s</b> added successfully", appID, country))
}
