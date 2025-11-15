package command

import (
	"fmt"
	"strings"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

type DeleteGoogleAppCommand struct {
	BaseCommand
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewDeleteGoogleAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *DeleteGoogleAppCommand {
	return &DeleteGoogleAppCommand{
		BaseCommand: BaseCommand{cfg: cfg},
		adminRepo:   adminRepo,
		groupRepo:   groupRepo,
	}
}

func (c *DeleteGoogleAppCommand) Execute(message *tgbotapi.Message) string {
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
		return "Usage: /deletegoogle <appId>\nExample: /deletegoogle com.example.app"
	}

	appID := args[0]

	if err := c.groupRepo.RemoveGoogleApp(groupID, appID); err != nil {
		return fmt.Sprintf("Failed to remove app: %v", err)
	}

	return fmt.Sprintf("Google app %s has been removed successfully.", appID)
}
