package command

import (
	"strconv"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// /delgroup [groupId] — Java DeleteGroupCommand. Admin-only.
type DeleteGroupCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewDeleteGroupCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *DeleteGroupCommand {
	return &DeleteGroupCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo}
}

func (c *DeleteGroupCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !requireAdminUser(msg.From.ID, msg.Chat.ID, c.cfg, sender) {
		return
	}
	args := splitArgs(msg.CommandArguments())
	if len(args) > 1 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	groupID := msg.Chat.ID
	if len(args) == 1 {
		parsed, err := strconv.ParseInt(args[0], 10, 64)
		if err != nil {
			_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
			return
		}
		groupID = parsed
	}
	removed, err := c.adminRepo.RemoveGroup(groupID)
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}
	if !removed {
		_ = sender.SendMessage(msg.Chat.ID, "Group is not added")
		return
	}
	_ = sender.SendMessage(msg.Chat.ID, "Group deleted successfully")
}
