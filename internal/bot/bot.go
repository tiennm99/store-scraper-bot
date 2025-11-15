package bot

import (
	"fmt"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/bot/command"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"go.uber.org/zap"
)

type Bot struct {
	api           *tgbotapi.BotAPI
	cfg           *config.Config
	adminRepo     *repository.AdminRepository
	groupRepo     *repository.GroupRepository
	appleScraper  *apple.AppleScraper
	googleScraper *google.GoogleScraper
	commands      map[string]command.Command
	logger        *zap.Logger
}

func NewBot(
	cfg *config.Config,
	adminRepo *repository.AdminRepository,
	groupRepo *repository.GroupRepository,
	appleScraper *apple.AppleScraper,
	googleScraper *google.GoogleScraper,
) (*Bot, error) {
	bot, err := tgbotapi.NewBotAPI(cfg.TelegramBotToken)
	if err != nil {
		return nil, fmt.Errorf("failed to create telegram bot: %w", err)
	}

	bot.Debug = cfg.Env == config.Development

	cfg.Logger.Info("Authorized on account", zap.String("username", bot.Self.UserName))

	b := &Bot{
		api:           bot,
		cfg:           cfg,
		adminRepo:     adminRepo,
		groupRepo:     groupRepo,
		appleScraper:  appleScraper,
		googleScraper: googleScraper,
		commands:      make(map[string]command.Command),
		logger:        cfg.Logger,
	}

	b.registerCommands()
	return b, nil
}

func (b *Bot) registerCommands() {
	b.commands["addgroup"] = command.NewAddGroupCommand(b.cfg, b.adminRepo, b.groupRepo)
	b.commands["deletegroup"] = command.NewDeleteGroupCommand(b.cfg, b.adminRepo, b.groupRepo)
	b.commands["listgroup"] = command.NewListGroupCommand(b.cfg, b.adminRepo)
	b.commands["addapple"] = command.NewAddAppleAppCommand(b.cfg, b.adminRepo, b.groupRepo, b.appleScraper)
	b.commands["deleteapple"] = command.NewDeleteAppleAppCommand(b.cfg, b.adminRepo, b.groupRepo)
	b.commands["addgoogle"] = command.NewAddGoogleAppCommand(b.cfg, b.adminRepo, b.groupRepo, b.googleScraper)
	b.commands["deletegoogle"] = command.NewDeleteGoogleAppCommand(b.cfg, b.adminRepo, b.groupRepo)
	b.commands["listapp"] = command.NewListAppCommand(b.cfg, b.adminRepo, b.groupRepo)
	b.commands["checkapp"] = command.NewCheckAppCommand(b.cfg, b.adminRepo, b.groupRepo, b.appleScraper, b.googleScraper)
	b.commands["checkappscores"] = command.NewCheckAppScoresCommand(b.cfg, b.adminRepo, b.groupRepo, b.appleScraper, b.googleScraper)
	b.commands["rawapple"] = command.NewRawAppleAppCommand(b.cfg, b.appleScraper)
	b.commands["rawgoogle"] = command.NewRawGoogleAppCommand(b.cfg, b.googleScraper)
	b.commands["info"] = command.NewInfoCommand(b.cfg)
}

func (b *Bot) Start() {
	u := tgbotapi.NewUpdate(0)
	u.Timeout = 60

	updates := b.api.GetUpdatesChan(u)

	for update := range updates {
		if update.Message == nil {
			continue
		}

		if !update.Message.IsCommand() {
			continue
		}

		go b.handleCommand(update.Message)
	}
}

func (b *Bot) handleCommand(message *tgbotapi.Message) {
	commandName := message.Command()
	cmd, exists := b.commands[commandName]

	if !exists {
		b.logger.Debug("Unknown command", zap.String("command", commandName))
		return
	}

	b.logger.Info("Executing command",
		zap.String("command", commandName),
		zap.Int64("userId", message.From.ID),
		zap.Int64("chatId", message.Chat.ID))

	response := cmd.Execute(message)
	if response != "" {
		msg := tgbotapi.NewMessage(message.Chat.ID, response)
		msg.ParseMode = "Markdown"
		msg.DisableWebPagePreview = true

		if _, err := b.api.Send(msg); err != nil {
			b.logger.Error("Failed to send message", zap.Error(err))
		}
	}
}

func (b *Bot) SendMessage(chatID int64, text string) error {
	msg := tgbotapi.NewMessage(chatID, text)
	msg.ParseMode = "Markdown"
	msg.DisableWebPagePreview = true
	msg.DisableNotification = false

	_, err := b.api.Send(msg)
	return err
}

func (b *Bot) SendMessageSilent(chatID int64, text string) error {
	msg := tgbotapi.NewMessage(chatID, text)
	msg.ParseMode = "Markdown"
	msg.DisableWebPagePreview = true
	msg.DisableNotification = true

	_, err := b.api.Send(msg)
	return err
}
