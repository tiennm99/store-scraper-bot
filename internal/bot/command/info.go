package command

import (
	"fmt"
	"runtime"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/config"
)

type InfoCommand struct {
	BaseCommand
}

func NewInfoCommand(cfg *config.Config) *InfoCommand {
	return &InfoCommand{
		BaseCommand: BaseCommand{cfg: cfg},
	}
}

func (c *InfoCommand) Execute(message *tgbotapi.Message) string {
	return fmt.Sprintf(`*Store Scraper Bot - Go Edition*

*Version:* 1.0.0
*Environment:* %s
*Source Commit:* %s
*Go Version:* %s
*Bot Username:* @%s

*Commands:*
/addgroup - Add current group to monitoring
/deletegroup - Remove current group
/listgroup - List all monitored groups
/addapple <appId> [country] - Add Apple app
/deleteapple <appId> - Remove Apple app
/addgoogle <appId> [country] - Add Google app
/deletegoogle <appId> - Remove Google app
/listapp - List apps in current group
/checkapp - Check for non-updated apps
/checkappscores - Check app scores
/rawapple <appId> [country] - Get raw Apple data
/rawgoogle <appId> [country] - Get raw Google data
/info - Show this info`,
		c.cfg.Env,
		c.cfg.SourceCommit,
		runtime.Version(),
		c.cfg.TelegramBotUsername,
	)
}
