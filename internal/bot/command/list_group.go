package command

import (
	"fmt"
	"strings"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

type ListGroupCommand struct {
	BaseCommand
	adminRepo *repository.AdminRepository
}

func NewListGroupCommand(cfg *config.Config, adminRepo *repository.AdminRepository) *ListGroupCommand {
	return &ListGroupCommand{
		BaseCommand: BaseCommand{cfg: cfg},
		adminRepo:   adminRepo,
	}
}

func (c *ListGroupCommand) Execute(message *tgbotapi.Message) string {
	if !c.requireAdmin(message) {
		return "You are not authorized to use this command."
	}

	groups, err := c.adminRepo.GetAllGroups()
	if err != nil {
		return fmt.Sprintf("Failed to get groups: %v", err)
	}

	if len(groups) == 0 {
		return "No groups found."
	}

	var sb strings.Builder
	sb.WriteString(fmt.Sprintf("*Total groups: %d*\n\n", len(groups)))
	for i, groupID := range groups {
		sb.WriteString(fmt.Sprintf("%d. %d\n", i+1, groupID))
	}

	return sb.String()
}
