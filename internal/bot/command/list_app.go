package command

import (
	"context"
	"fmt"
	"strings"
	"time"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

type ListAppCommand struct {
	BaseCommand
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewListAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *ListAppCommand {
	return &ListAppCommand{
		BaseCommand: BaseCommand{cfg: cfg},
		adminRepo:   adminRepo,
		groupRepo:   groupRepo,
	}
}

func (c *ListAppCommand) Execute(message *tgbotapi.Message) string {
	if !c.requireAdmin(message) {
		return "You are not authorized to use this command."
	}

	groupID := message.Chat.ID
	hasGroup, err := c.adminRepo.HasGroup(groupID)
	if err != nil {
		return fmt.Sprintf("Failed to check group: %v", err)
	}
	if !hasGroup {
		return "This group is not registered."
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	group, err := c.groupRepo.Get(ctx, groupID)
	if err != nil {
		return fmt.Sprintf("Failed to get group: %v", err)
	}

	var sb strings.Builder
	sb.WriteString("*Apps in this group:*\n\n")

	if len(group.AppleApps) > 0 {
		sb.WriteString("*Apple Apps:*\n")
		for i, app := range group.AppleApps {
			sb.WriteString(fmt.Sprintf("%d. %s (%s)\n", i+1, app.AppID, app.Country))
		}
		sb.WriteString("\n")
	}

	if len(group.GoogleApps) > 0 {
		sb.WriteString("*Google Apps:*\n")
		for i, app := range group.GoogleApps {
			sb.WriteString(fmt.Sprintf("%d. %s (%s)\n", i+1, app.AppID, app.Country))
		}
	}

	if len(group.AppleApps) == 0 && len(group.GoogleApps) == 0 {
		return "No apps in this group."
	}

	return sb.String()
}
