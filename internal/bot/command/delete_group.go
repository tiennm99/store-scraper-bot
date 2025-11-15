package command

import (
	"context"
	"fmt"
	"time"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

type DeleteGroupCommand struct {
	BaseCommand
	adminRepo *repository.AdminRepository
	groupRepo *repository.GroupRepository
}

func NewDeleteGroupCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository) *DeleteGroupCommand {
	return &DeleteGroupCommand{
		BaseCommand: BaseCommand{cfg: cfg},
		adminRepo:   adminRepo,
		groupRepo:   groupRepo,
	}
}

func (c *DeleteGroupCommand) Execute(message *tgbotapi.Message) string {
	if !c.requireAdmin(message) {
		return "You are not authorized to use this command."
	}

	groupID := message.Chat.ID

	if err := c.adminRepo.RemoveGroup(groupID); err != nil {
		return fmt.Sprintf("Failed to remove group: %v", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := c.groupRepo.Delete(ctx, groupID); err != nil {
		return fmt.Sprintf("Group removed from admin but failed to delete group data: %v", err)
	}

	return fmt.Sprintf("Group %d has been deleted successfully.", groupID)
}
