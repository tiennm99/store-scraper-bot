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

type AdminRepository struct {
	collection *mongo.Collection
}

func NewAdminRepository() *AdminRepository {
	return &AdminRepository{
		collection: GetCollection("admin"),
	}
}

func (r *AdminRepository) Get(ctx context.Context) (*model.Admin, error) {
	admin := &model.Admin{}
	err := r.collection.FindOne(ctx, bson.M{"_id": "admin"}).Decode(admin)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			// Return new admin if not found
			return model.NewAdmin(), nil
		}
		return nil, fmt.Errorf("failed to get admin: %w", err)
	}
	return admin, nil
}

func (r *AdminRepository) Save(ctx context.Context, admin *model.Admin) error {
	opts := options.Replace().SetUpsert(true)
	_, err := r.collection.ReplaceOne(ctx, bson.M{"_id": "admin"}, admin, opts)
	if err != nil {
		return fmt.Errorf("failed to save admin: %w", err)
	}
	return nil
}

func (r *AdminRepository) AddGroup(groupID int64) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	admin, err := r.Get(ctx)
	if err != nil {
		return err
	}

	if !admin.AddGroup(groupID) {
		return fmt.Errorf("group already exists")
	}

	return r.Save(ctx, admin)
}

func (r *AdminRepository) RemoveGroup(groupID int64) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	admin, err := r.Get(ctx)
	if err != nil {
		return err
	}

	if !admin.RemoveGroup(groupID) {
		return fmt.Errorf("group not found")
	}

	return r.Save(ctx, admin)
}

func (r *AdminRepository) HasGroup(groupID int64) (bool, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	admin, err := r.Get(ctx)
	if err != nil {
		return false, err
	}

	return admin.HasGroup(groupID), nil
}

func (r *AdminRepository) GetAllGroups() ([]int64, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	admin, err := r.Get(ctx)
	if err != nil {
		return nil, err
	}

	return admin.Groups, nil
}
