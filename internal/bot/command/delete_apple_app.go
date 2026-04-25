package command

import (
	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// /delapple <appId> — Java DeleteAppleAppCommand.
type DeleteAppleAppCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewDeleteAppleAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *DeleteAppleAppCommand {
	return &DeleteAppleAppCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo}
}

func (c *DeleteAppleAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !authorizeGroup(msg.Chat.ID, c.adminRepo, sender) {
		return
	}
	args := splitArgs(msg.CommandArguments())
	if len(args) != 1 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	removed, err := c.groupRepo.RemoveAppleApp(msg.Chat.ID, args[0])
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}
	if !removed {
		_ = sender.SendMessage(msg.Chat.ID, "Apple app is not added")
		return
	}
	_ = sender.SendMessage(msg.Chat.ID, "Apple app deleted successfully")
}
