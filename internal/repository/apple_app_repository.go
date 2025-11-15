package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type AppleAppRepository struct {
	collection *mongo.Collection
}

func NewAppleAppRepository() *AppleAppRepository {
	return &AppleAppRepository{
		collection: GetCollection("apple_app"),
	}
}

func (r *AppleAppRepository) Get(ctx context.Context, appID string) (*model.AppleApp, error) {
	app := &model.AppleApp{}
	err := r.collection.FindOne(ctx, bson.M{"_id": appID}).Decode(app)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return nil, nil // Not found
		}
		return nil, fmt.Errorf("failed to get apple app: %w", err)
	}
	return app, nil
}

func (r *AppleAppRepository) Save(ctx context.Context, app *model.AppleApp) error {
	app.UpdatedAt = time.Now()
	opts := options.Replace().SetUpsert(true)
	_, err := r.collection.ReplaceOne(ctx, bson.M{"_id": app.Key}, app, opts)
	if err != nil {
		return fmt.Errorf("failed to save apple app: %w", err)
	}
	return nil
}

func (r *AppleAppRepository) GetCached(appID string) (*model.AppleApp, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	app, err := r.Get(ctx, appID)
	if err != nil {
		return nil, err
	}

	if app != nil && !app.IsExpired(config.GlobalConfig.AppCacheSeconds) {
		return app, nil
	}

	return nil, nil // Cache expired or not found
}
