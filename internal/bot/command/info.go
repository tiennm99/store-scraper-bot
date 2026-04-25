package command

import (
	"fmt"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
)

// /info — Java InfoCommand. Reports the chat (group) ID.
type InfoCommand struct{ cfg *config.Config }

func NewInfoCommand(cfg *config.Config) *InfoCommand { return &InfoCommand{cfg: cfg} }

func (c *InfoCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	args := splitArgs(msg.CommandArguments())
	if len(args) != 0 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}
	_ = sender.SendMessage(msg.Chat.ID, fmt.Sprintf("Id của nhóm là <code>%d</code>\n", msg.Chat.ID))
}
