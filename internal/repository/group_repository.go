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

type GroupRepository struct {
	collection *mongo.Collection
}

func NewGroupRepository() *GroupRepository {
	return &GroupRepository{
		collection: GetCollection("group"),
	}
}

func (r *GroupRepository) Get(ctx context.Context, groupID int64) (*model.Group, error) {
	group := &model.Group{}
	err := r.collection.FindOne(ctx, bson.M{"_id": groupID}).Decode(group)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			// Return new group if not found
			return model.NewGroup(groupID), nil
		}
		return nil, fmt.Errorf("failed to get group: %w", err)
	}
	return group, nil
}

func (r *GroupRepository) Save(ctx context.Context, group *model.Group) error {
	opts := options.Replace().SetUpsert(true)
	_, err := r.collection.ReplaceOne(ctx, bson.M{"_id": group.Key}, group, opts)
	if err != nil {
		return fmt.Errorf("failed to save group: %w", err)
	}
	return nil
}

func (r *GroupRepository) Delete(ctx context.Context, groupID int64) error {
	_, err := r.collection.DeleteOne(ctx, bson.M{"_id": groupID})
	if err != nil {
		return fmt.Errorf("failed to delete group: %w", err)
	}
	return nil
}

func (r *GroupRepository) AddAppleApp(groupID int64, appID, country string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return err
	}

	if !group.AddAppleApp(appID, country) {
		return fmt.Errorf("apple app already exists in group")
	}

	return r.Save(ctx, group)
}

func (r *GroupRepository) RemoveAppleApp(groupID int64, appID string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return err
	}

	if !group.RemoveAppleApp(appID) {
		return fmt.Errorf("apple app not found in group")
	}

	return r.Save(ctx, group)
}

func (r *GroupRepository) AddGoogleApp(groupID int64, appID, country string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return err
	}

	if !group.AddGoogleApp(appID, country) {
		return fmt.Errorf("google app already exists in group")
	}

	return r.Save(ctx, group)
}

func (r *GroupRepository) RemoveGoogleApp(groupID int64, appID string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	group, err := r.Get(ctx, groupID)
	if err != nil {
		return err
	}

	if !group.RemoveGoogleApp(appID) {
		return fmt.Errorf("google app not found in group")
	}

	return r.Save(ctx, group)
}
