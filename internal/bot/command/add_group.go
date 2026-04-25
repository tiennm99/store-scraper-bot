package command

import (
	"context"
	"strconv"
	"time"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// /addgroup [groupId] — Java AddGroupCommand. Admin-only.
type AddGroupCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewAddGroupCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *AddGroupCommand {
	return &AddGroupCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo}
}

func (c *AddGroupCommand) Execute(msg *tgbotapi.Message, sender Sender) {
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
	added, err := c.adminRepo.AddGroup(groupID)
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}
	if !added {
		_ = sender.SendMessage(msg.Chat.ID, "Group is already added")
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = c.groupRepo.Init(ctx, groupID)
	_ = sender.SendMessage(msg.Chat.ID, "Group added successfully")
}
