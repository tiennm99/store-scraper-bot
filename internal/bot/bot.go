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

// parseMode mirrors Java StoreScrapeBotTelegramClient: HTML for all messages.
const parseMode = "HTML"

type Bot struct {
	api      *tgbotapi.BotAPI
	cfg      *config.Config
	commands map[string]command.Command
	logger   *zap.Logger
}

func NewBot(
	cfg *config.Config,
	adminRepo *repository.AdminRepository,
	groupRepo *repository.GroupRepository,
	appleScraper *apple.AppleScraper,
	googleScraper *google.GoogleScraper,
) (*Bot, error) {
	api, err := tgbotapi.NewBotAPI(cfg.TelegramBotToken)
	if err != nil {
		return nil, fmt.Errorf("failed to create telegram bot: %w", err)
	}
	api.Debug = cfg.Env == config.Development
	cfg.Logger.Info("Authorized on account", zap.String("username", api.Self.UserName))

	b := &Bot{api: api, cfg: cfg, commands: map[string]command.Command{}, logger: cfg.Logger}

	// Java command identifiers (StoreScrapeBot constructor) — keep these strings
	// matching exactly so existing users' muscle memory still works.
	b.commands["info"] = command.NewInfoCommand(cfg)
	b.commands["addgroup"] = command.NewAddGroupCommand(cfg, adminRepo, groupRepo)
	b.commands["delgroup"] = command.NewDeleteGroupCommand(cfg, adminRepo, groupRepo)
	b.commands["listgroup"] = command.NewListGroupCommand(cfg, adminRepo)
	b.commands["addapple"] = command.NewAddAppleAppCommand(cfg, adminRepo, groupRepo, appleScraper)
	b.commands["delapple"] = command.NewDeleteAppleAppCommand(cfg, adminRepo, groupRepo)
	b.commands["addgoogle"] = command.NewAddGoogleAppCommand(cfg, adminRepo, groupRepo, googleScraper)
	b.commands["delgoogle"] = command.NewDeleteGoogleAppCommand(cfg, adminRepo, groupRepo)
	b.commands["listapp"] = command.NewListAppCommand(cfg, adminRepo, groupRepo)
	b.commands["checkapp"] = command.NewCheckAppCommand(cfg, adminRepo, groupRepo, appleScraper, googleScraper)
	b.commands["checkappscore"] = command.NewCheckAppScoresCommand(cfg, adminRepo, groupRepo, appleScraper, googleScraper)
	b.commands["rawappleapp"] = command.NewRawAppleAppCommand(cfg, appleScraper)
	b.commands["rawgoogleapp"] = command.NewRawGoogleAppCommand(cfg, googleScraper)

	return b, nil
}

func (b *Bot) Start() {
	u := tgbotapi.NewUpdate(0)
	u.Timeout = 60
	updates := b.api.GetUpdatesChan(u)
	for update := range updates {
		if update.Message == nil || !update.Message.IsCommand() {
			continue
		}
		go b.handleCommand(update.Message)
	}
}

func (b *Bot) handleCommand(message *tgbotapi.Message) {
	defer func() {
		if r := recover(); r != nil {
			b.logger.Error("panic in command", zap.Any("panic", r))
			_ = b.SendMessage(message.Chat.ID, "Internal server error")
		}
	}()
	name := message.Command()
	cmd, ok := b.commands[name]
	if !ok {
		b.logger.Debug("Unknown command", zap.String("command", name))
		return
	}
	b.logger.Info("Executing command",
		zap.String("command", name),
		zap.Int64("userId", message.From.ID),
		zap.Int64("chatId", message.Chat.ID))
	cmd.Execute(message, b)
}

// SendMessage sends an HTML-parsed message (Java parity).
func (b *Bot) SendMessage(chatID int64, html string) error {
	msg := tgbotapi.NewMessage(chatID, html)
	msg.ParseMode = parseMode
	msg.DisableWebPagePreview = true
	_, err := b.api.Send(msg)
	if err != nil {
		b.logger.Warn("send message failed", zap.Int64("chatId", chatID), zap.Error(err))
	}
	return err
}

// SendMessageSilent sends an HTML message with notifications muted (weekend behavior).
func (b *Bot) SendMessageSilent(chatID int64, html string) error {
	msg := tgbotapi.NewMessage(chatID, html)
	msg.ParseMode = parseMode
	msg.DisableWebPagePreview = true
	msg.DisableNotification = true
	_, err := b.api.Send(msg)
	if err != nil {
		b.logger.Warn("send silent message failed", zap.Int64("chatId", chatID), zap.Error(err))
	}
	return err
}

// SendDocument sends body as a file attachment with the given filename
// (used by /rawappleapp and /rawgoogleapp).
func (b *Bot) SendDocument(chatID int64, filename, body string) error {
	file := tgbotapi.FileBytes{Name: filename, Bytes: []byte(body)}
	doc := tgbotapi.NewDocument(chatID, file)
	_, err := b.api.Send(doc)
	if err != nil {
		b.logger.Warn("send document failed", zap.Int64("chatId", chatID), zap.Error(err))
	}
	return err
}
