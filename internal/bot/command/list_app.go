package command

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"github.com/miti99/store-scraper-bot-go/internal/util"
)

// /listapp — Java ListAppCommand. Two tables (Apple / Google) of tracked apps.
type ListAppCommand struct {
	cfg       *config.Config
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewListAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *ListAppCommand {
	return &ListAppCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo}
}

func (c *ListAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !authorizeGroup(msg.Chat.ID, c.adminRepo, sender) {
		return
	}
	if len(splitArgs(msg.CommandArguments())) != 0 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	group, err := c.groupRepo.Get(ctx, msg.Chat.ID)
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}

	var sb strings.Builder
	sb.WriteString("<b>Apple Apps</b>\n")
	sb.WriteString(formatAppTable(group.AppleApps))
	sb.WriteString("\n<b>Google Apps</b>\n")
	sb.WriteString(formatAppTable(group.GoogleApps))
	_ = sender.SendMessage(msg.Chat.ID, sb.String())
}

func formatAppTable(apps []model.AppInfo) string {
	if len(apps) == 0 {
		return "<i>(none)</i>\n"
	}
	rows := make([][]string, 0, len(apps))
	for i, a := range apps {
		rows = append(rows, []string{strconv.Itoa(i + 1), a.AppID, a.Country})
	}
	return fmt.Sprintf("<pre>%s</pre>\n", util.BuildTable([]string{"#", "AppId", "Country"}, rows))
}
