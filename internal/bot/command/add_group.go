package command

import (
	"fmt"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

type AddGroupCommand struct {
	BaseCommand
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewAddGroupCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *AddGroupCommand {
	return &AddGroupCommand{
		BaseCommand: BaseCommand{cfg: cfg},
		adminRepo:   adminRepo,
		groupRepo:   groupRepo,
	}
}

func (c *AddGroupCommand) Execute(message *tgbotapi.Message) string {
	if !c.requireAdmin(message) {
		return "You are not authorized to use this command."
	}

	groupID := message.Chat.ID
	if err := c.adminRepo.AddGroup(groupID); err != nil {
		return fmt.Sprintf("Failed to add group: %v", err)
	}

	return fmt.Sprintf("Group %d has been added successfully.", groupID)
}
