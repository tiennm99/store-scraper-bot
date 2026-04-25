package command

import (
	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// /delgoogle <appId> — Java DeleteGoogleAppCommand.
type DeleteGoogleAppCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewDeleteGoogleAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *DeleteGoogleAppCommand {
	return &DeleteGoogleAppCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo}
}

func (c *DeleteGoogleAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !authorizeGroup(msg.Chat.ID, c.adminRepo, sender) {
		return
	}
	args := splitArgs(msg.CommandArguments())
	if len(args) != 1 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	removed, err := c.groupRepo.RemoveGoogleApp(msg.Chat.ID, args[0])
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}
	if !removed {
		_ = sender.SendMessage(msg.Chat.ID, "Google app is not added")
		return
	}
	_ = sender.SendMessage(msg.Chat.ID, "Google app deleted successfully")
}
