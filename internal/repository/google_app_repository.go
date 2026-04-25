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

// GoogleAppRepository caches Google Play responses in the "google_app" collection.
type GoogleAppRepository struct {
	collection *mongo.Collection
}

func NewGoogleAppRepository() *GoogleAppRepository {
	return &GoogleAppRepository{collection: GetCollection("google_app")}
}

func (r *GoogleAppRepository) Get(ctx context.Context, appID string) (*model.GoogleApp, error) {
	app := &model.GoogleApp{}
	err := r.collection.FindOne(ctx, bson.M{"_id": appID}).Decode(app)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return nil, nil
		}
		return nil, fmt.Errorf("failed to get google app: %w", err)
	}
	return app, nil
}

func (r *GoogleAppRepository) Save(ctx context.Context, app *model.GoogleApp) error {
	opts := options.Replace().SetUpsert(true)
	_, err := r.collection.ReplaceOne(ctx, bson.M{"_id": app.ID}, app, opts)
	if err != nil {
		return fmt.Errorf("failed to save google app: %w", err)
	}
	return nil
}

func (r *GoogleAppRepository) GetCached(appID string) (*model.GoogleApp, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	app, err := r.Get(ctx, appID)
	if err != nil || app == nil {
		return nil, err
	}
	cacheMillis := int64(config.GlobalConfig.AppCacheSeconds) * 1000
	if app.IsExpired(time.Now().UnixMilli(), cacheMillis) {
		return nil, nil
	}
	return app, nil
}
