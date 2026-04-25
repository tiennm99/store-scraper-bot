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

// AppleAppRepository caches Apple app responses in the "apple_app" collection.
// Java schema stores _id=appId, app=AppleAppResponse, millis=cache timestamp.
type AppleAppRepository struct {
	collection *mongo.Collection
}

func NewAppleAppRepository() *AppleAppRepository {
	return &AppleAppRepository{collection: GetCollection("apple_app")}
}

func (r *AppleAppRepository) Get(ctx context.Context, appID string) (*model.AppleApp, error) {
	app := &model.AppleApp{}
	err := r.collection.FindOne(ctx, bson.M{"_id": appID}).Decode(app)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return nil, nil
		}
		return nil, fmt.Errorf("failed to get apple app: %w", err)
	}
	return app, nil
}

func (r *AppleAppRepository) Save(ctx context.Context, app *model.AppleApp) error {
	opts := options.Replace().SetUpsert(true)
	_, err := r.collection.ReplaceOne(ctx, bson.M{"_id": app.ID}, app, opts)
	if err != nil {
		return fmt.Errorf("failed to save apple app: %w", err)
	}
	return nil
}

// GetCached returns a cached entry if it exists and has not expired (per
// AppCacheSeconds). Returns (nil, nil) on cache miss.
func (r *AppleAppRepository) GetCached(appID string) (*model.AppleApp, error) {
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
