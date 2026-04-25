package command

import (
	"fmt"
	"strings"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// /listgroup — Java ListGroupCommand. Admin-only. Lists authorized groups.
type ListGroupCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
}

func NewListGroupCommand(cfg *config.Config, adminRepo *repository.AdminRepository) *ListGroupCommand {
	return &ListGroupCommand{cfg: cfg, adminRepo: adminRepo}
}

func (c *ListGroupCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !requireAdminUser(msg.From.ID, msg.Chat.ID, c.cfg, sender) {
		return
	}
	if len(splitArgs(msg.CommandArguments())) != 0 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	groups, err := c.adminRepo.GetAllGroups()
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}
	if len(groups) == 0 {
		_ = sender.SendMessage(msg.Chat.ID, "No groups found")
		return
	}
	var sb strings.Builder
	sb.WriteString(fmt.Sprintf("<b>Authorized groups (%d):</b>\n", len(groups)))
	for i, gid := range groups {
		sb.WriteString(fmt.Sprintf("%d. <code>%d</code>\n", i+1, gid))
	}
	_ = sender.SendMessage(msg.Chat.ID, sb.String())
}
