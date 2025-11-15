package main

import (
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/bot"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"github.com/miti99/store-scraper-bot-go/internal/scheduler"
	"go.uber.org/zap"
)

func main() {
	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}
	defer cfg.Logger.Sync()

	cfg.Logger.Info("Starting Store Scraper Bot",
		zap.String("env", string(cfg.Env)),
		zap.String("commit", cfg.SourceCommit))

	// Initialize MongoDB
	if err := repository.InitMongoDB(cfg); err != nil {
		cfg.Logger.Fatal("Failed to initialize MongoDB", zap.Error(err))
	}
	defer repository.Close()

	// Initialize repositories
	adminRepo := repository.NewAdminRepository()
	groupRepo := repository.NewGroupRepository()
	appleAppRepo := repository.NewAppleAppRepository()
	googleAppRepo := repository.NewGoogleAppRepository()

	// Initialize scrapers
	appleScraper := apple.NewAppleScraper(appleAppRepo, cfg)
	googleScraper := google.NewGoogleScraper(googleAppRepo, cfg)

	// Initialize bot
	telegramBot, err := bot.NewBot(cfg, adminRepo, groupRepo, appleScraper, googleScraper)
	if err != nil {
		cfg.Logger.Fatal("Failed to initialize bot", zap.Error(err))
	}

	// Initialize and start scheduler
	sched := scheduler.NewScheduler(cfg, telegramBot, adminRepo, groupRepo, appleScraper, googleScraper)
	if err := sched.Start(); err != nil {
		cfg.Logger.Fatal("Failed to start scheduler", zap.Error(err))
	}
	defer sched.Stop()

	// Start bot in a goroutine
	go func() {
		cfg.Logger.Info("Starting Telegram bot polling")
		telegramBot.Start()
	}()

	// Wait for interrupt signal
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)

	<-sigChan
	cfg.Logger.Info("Received shutdown signal, stopping bot...")
}
