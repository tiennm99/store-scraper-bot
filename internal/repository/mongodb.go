package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/miti99/store-scraper-bot-go/internal/config"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.uber.org/zap"
)

var (
	client   *mongo.Client
	database *mongo.Database
)

func InitMongoDB(cfg *config.Config) error {
	ctx, cancel := context.WithTimeout(context.Background(), cfg.MongoTimeout)
	defer cancel()

	clientOptions := options.Client().ApplyURI(cfg.MongoURI)

	var err error
	client, err = mongo.Connect(ctx, clientOptions)
	if err != nil {
		return fmt.Errorf("failed to connect to MongoDB: %w", err)
	}

	// Ping to verify connection
	if err := client.Ping(ctx, nil); err != nil {
		return fmt.Errorf("failed to ping MongoDB: %w", err)
	}

	database = client.Database(cfg.MongoDatabase)
	cfg.Logger.Info("Connected to MongoDB",
		zap.String("database", cfg.MongoDatabase),
		zap.String("uri", cfg.MongoURI))

	return nil
}

func Close() error {
	if client != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		return client.Disconnect(ctx)
	}
	return nil
}

func GetDatabase() *mongo.Database {
	return database
}

func GetCollection(name string) *mongo.Collection {
	return database.Collection(name)
}
