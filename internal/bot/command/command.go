package command

import (
	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
)

type Command interface {
	Execute(message *tgbotapi.Message) string
}

type BaseCommand struct {
	cfg *config.Config
}

func (c *BaseCommand) isAdmin(userID int64) bool {
	return c.cfg.IsAdmin(userID)
}

func (c *BaseCommand) requireAdmin(message *tgbotapi.Message) bool {
	if !c.isAdmin(message.From.ID) {
		return false
	}
	return true
}
