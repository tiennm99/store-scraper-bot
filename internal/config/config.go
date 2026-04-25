package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	"go.mongodb.org/mongo-driver/mongo/options"
	"go.uber.org/zap"
)

type Environment string

const (
	Development Environment = "DEVELOPMENT"
	Production  Environment = "PRODUCTION"
)

const DefaultDatabaseName = "store-scraper-bot"

type Config struct {
	// Telegram
	TelegramBotToken    string
	TelegramBotUsername string

	// MongoDB
	MongoURI      string
	MongoDatabase string
	MongoTimeout  time.Duration

	// Application
	Env          Environment
	AdminIDs     []int64
	CreatorID    int64
	SourceCommit string

	// Constants
	AppCacheSeconds          int
	NumDaysWarningNotUpdated int
	ScheduleCheckAppTime     string
	VietnamLocation          *time.Location

	// Logger
	Logger *zap.Logger
}

var GlobalConfig *Config

func Load() (*Config, error) {
	cfg := &Config{}

	cfg.TelegramBotToken = getEnv("TELEGRAM_BOT_TOKEN", "")
	if cfg.TelegramBotToken == "" {
		return nil, fmt.Errorf("TELEGRAM_BOT_TOKEN is required")
	}
	cfg.TelegramBotUsername = getEnv("TELEGRAM_BOT_USERNAME", "")
	if cfg.TelegramBotUsername == "" {
		return nil, fmt.Errorf("TELEGRAM_BOT_USERNAME is required")
	}

	// Java parity: prefer MONGODB_CONNECTION_STRING. Fall back to MONGO_URI.
	cfg.MongoURI = getEnv("MONGODB_CONNECTION_STRING", getEnv("MONGO_URI", "mongodb://localhost:27017"))
	cfg.MongoDatabase = getEnv("MONGO_DATABASE", "")
	if cfg.MongoDatabase == "" {
		cfg.MongoDatabase = databaseFromURI(cfg.MongoURI)
	}
	cfg.MongoTimeout = time.Duration(getEnvInt("MONGO_TIMEOUT_SECONDS", 10)) * time.Second

	envStr := getEnv("ENV", "DEVELOPMENT")
	if envStr == "PRODUCTION" {
		cfg.Env = Production
	} else {
		cfg.Env = Development
	}

	adminIDsStr := getEnv("ADMIN_IDS", "")
	if adminIDsStr == "" {
		return nil, fmt.Errorf("ADMIN_IDS is required")
	}
	cfg.AdminIDs = parseAdminIDs(adminIDsStr)
	if len(cfg.AdminIDs) == 0 {
		return nil, fmt.Errorf("at least one admin ID is required")
	}
	cfg.CreatorID = cfg.AdminIDs[0]

	cfg.SourceCommit = getEnv("SOURCE_COMMIT", "unknown")

	cfg.AppCacheSeconds = getEnvInt("APP_CACHE_SECONDS", 600)
	cfg.NumDaysWarningNotUpdated = getEnvInt("NUM_DAYS_WARNING_NOT_UPDATED", 30)
	cfg.ScheduleCheckAppTime = getEnv("SCHEDULE_CHECK_APP_TIME", "0 7 * * *")

	loc, err := time.LoadLocation("Asia/Ho_Chi_Minh")
	if err != nil {
		return nil, fmt.Errorf("failed to load Vietnam timezone: %w", err)
	}
	cfg.VietnamLocation = loc

	var logger *zap.Logger
	if cfg.Env == Production {
		logger, err = zap.NewProduction()
	} else {
		logger, err = zap.NewDevelopment()
	}
	if err != nil {
		return nil, fmt.Errorf("failed to initialize logger: %w", err)
	}
	cfg.Logger = logger

	GlobalConfig = cfg
	return cfg, nil
}

// databaseFromURI extracts the database name from a Mongo connection string,
// falling back to DefaultDatabaseName (Java behavior).
func databaseFromURI(uri string) string {
	opts := options.Client().ApplyURI(uri)
	if opts != nil && opts.Auth != nil && opts.Auth.AuthSource != "" {
		// AuthSource is not the data DB; ignore it.
		_ = opts
	}
	// Manual parse: scheme://...host[:port]/<dbname>?<params>
	rest := uri
	if idx := strings.Index(rest, "://"); idx >= 0 {
		rest = rest[idx+3:]
	}
	slash := strings.Index(rest, "/")
	if slash < 0 {
		return DefaultDatabaseName
	}
	tail := rest[slash+1:]
	if q := strings.Index(tail, "?"); q >= 0 {
		tail = tail[:q]
	}
	tail = strings.TrimSpace(tail)
	if tail == "" {
		return DefaultDatabaseName
	}
	return tail
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getEnvInt(key string, defaultValue int) int {
	if value := os.Getenv(key); value != "" {
		if intVal, err := strconv.Atoi(value); err == nil {
			return intVal
		}
	}
	return defaultValue
}

func parseAdminIDs(adminIDsStr string) []int64 {
	parts := strings.Split(adminIDsStr, ",")
	adminIDs := make([]int64, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if id, err := strconv.ParseInt(part, 10, 64); err == nil {
			adminIDs = append(adminIDs, id)
		}
	}
	return adminIDs
}

func (c *Config) IsAdmin(userID int64) bool {
	for _, adminID := range c.AdminIDs {
		if adminID == userID {
			return true
		}
	}
	return false
}
