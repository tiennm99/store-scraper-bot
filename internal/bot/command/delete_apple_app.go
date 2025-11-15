package command

import (
	"fmt"
	"strings"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

type DeleteAppleAppCommand struct {
	BaseCommand
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewDeleteAppleAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *DeleteAppleAppCommand {
	return &DeleteAppleAppCommand{
		BaseCommand: BaseCommand{cfg: cfg},
		adminRepo:   adminRepo,
		groupRepo:   groupRepo,
	}
}

func (c *DeleteAppleAppCommand) Execute(message *tgbotapi.Message) string {
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

	args := strings.Fields(message.CommandArguments())
	if len(args) == 0 {
		return "Usage: /deleteapple <appId>\nExample: /deleteapple com.example.app"
	}

	appID := args[0]

	if err := c.groupRepo.RemoveAppleApp(groupID, appID); err != nil {
		return fmt.Sprintf("Failed to remove app: %v", err)
	}

	return fmt.Sprintf("Apple app %s has been removed successfully.", appID)
}
