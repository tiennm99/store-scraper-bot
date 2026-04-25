package command

import (
	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
)

// Sender is what bot.Bot exposes to commands. HTML parse mode (Java parity).
type Sender interface {
	SendMessage(chatID int64, html string) error
	SendMessageSilent(chatID int64, html string) error
	SendDocument(chatID int64, filename, body string) error
}

// Command is the unit registered on the bot dispatcher.
type Command interface {
	Execute(msg *tgbotapi.Message, sender Sender)
}

// authorizeGroup verifies the chat is in the admin's authorized group list.
// Mirrors Java's per-command "Group is not allowed to use bot" gate.
func authorizeGroup(chatID int64, adminRepo *repository.AdminRepository, sender Sender) bool {
	ok, err := adminRepo.HasGroup(chatID)
	if err != nil || !ok {
		_ = sender.SendMessage(chatID, "Group is not allowed to use bot")
		return false
	}
	return true
}

// requireAdminUser checks the user is in Environment.ADMIN_IDS.
func requireAdminUser(userID, chatID int64, cfg *config.Config, sender Sender) bool {
	if !cfg.IsAdmin(userID) {
		_ = sender.SendMessage(chatID, "You are not authorized to use this command")
		return false
	}
	return true
}
