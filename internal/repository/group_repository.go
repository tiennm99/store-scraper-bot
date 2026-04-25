package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/miti99/store-scraper-bot-go/internal/model"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// GroupRepository persists Group documents in the "group" collection.
// Java schema stores _id as the string form of the Telegram chat ID.
type GroupRepository struct {
	collection *mongo.Collection
}

func NewGroupRepository() *GroupRepository {
	return &GroupRepository{collection: GetCollection("group")}
}

// Init creates an empty Group if not present.
func (r *GroupRepository) Init(ctx context.Context, groupID int64) error {
	exists, err := r.Exists(ctx, groupID)
	if err != nil {
		return err
	}
	if exists {
		return nil
	}
	return r.Save(ctx, model.NewGroup(groupID))
}

func (r *GroupRepository) Exists(ctx context.Context, groupID int64) (bool, error) {
	count, err := r.collection.CountDocuments(ctx, bson.M{"_id": model.GroupIDToKey(groupID)})
	if err != nil {
		return false, fmt.Errorf("failed to count group: %w", err)
	}
	return count > 0, nil
}

func (r *GroupRepository) Get(ctx context.Context, groupID int64) (*model.Group, error) {
	group := &model.Group{}
	err := r.collection.FindOne(ctx, bson.M{"_id": model.GroupIDToKey(groupID)}).Decode(group)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return model.NewGroup(groupID), nil
		}
		return nil, fmt.Errorf("failed to get group: %w", err)
	}
	return group, nil
}

func (r *GroupRepository) Save(ctx context.Context, group *model.Group) error {
	opts := options.Replace().SetUpsert(true)
	_, err := r.collection.ReplaceOne(ctx, bson.M{"_id": group.ID}, group, opts)
	if err != nil {
		return fmt.Errorf("failed to save group: %w", err)
	}
	return nil
}

func (r *GroupRepository) Delete(ctx context.Context, groupID int64) error {
	_, err := r.collection.DeleteOne(ctx, bson.M{"_id": model.GroupIDToKey(groupID)})
	if err != nil {
		return fmt.Errorf("failed to delete group: %w", err)
	}
	return nil
}

func (r *GroupRepository) shortCtx() (context.Context, context.CancelFunc) {
	return context.WithTimeout(context.Background(), 5*time.Second)
}

func (r *GroupRepository) AddAppleApp(groupID int64, appID, country string) (added bool, err error) {
	ctx, cancel := r.shortCtx()
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return false, err
	}
	if !group.AddAppleApp(appID, country) {
		return false, nil
	}
	return true, r.Save(ctx, group)
}

func (r *GroupRepository) RemoveAppleApp(groupID int64, appID string) (removed bool, err error) {
	ctx, cancel := r.shortCtx()
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return false, err
	}
	if !group.RemoveAppleApp(appID) {
		return false, nil
	}
	return true, r.Save(ctx, group)
}

func (r *GroupRepository) AddGoogleApp(groupID int64, appID, country string) (added bool, err error) {
	ctx, cancel := r.shortCtx()
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return false, err
	}
	if !group.AddGoogleApp(appID, country) {
		return false, nil
	}
	return true, r.Save(ctx, group)
}

func (r *GroupRepository) RemoveGoogleApp(groupID int64, appID string) (removed bool, err error) {
	ctx, cancel := r.shortCtx()
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return false, err
	}
	if !group.RemoveGoogleApp(appID) {
		return false, nil
	}
	return true, r.Save(ctx, group)
}
