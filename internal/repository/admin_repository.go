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

// AdminRepository persists the singleton Admin document.
// Java equivalent stores it in the "common" collection at _id="admin".
type AdminRepository struct {
	collection *mongo.Collection
}

func NewAdminRepository() *AdminRepository {
	return &AdminRepository{collection: GetCollection("common")}
}

// Init creates the singleton document if it does not yet exist.
func (r *AdminRepository) Init() error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	count, err := r.collection.CountDocuments(ctx, bson.M{"_id": model.AdminID})
	if err != nil {
		return fmt.Errorf("failed to count admin: %w", err)
	}
	if count > 0 {
		return nil
	}
	return r.Save(ctx, model.NewAdmin())
}

func (r *AdminRepository) Get(ctx context.Context) (*model.Admin, error) {
	admin := &model.Admin{}
	err := r.collection.FindOne(ctx, bson.M{"_id": model.AdminID}).Decode(admin)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return model.NewAdmin(), nil
		}
		return nil, fmt.Errorf("failed to get admin: %w", err)
	}
	return admin, nil
}

func (r *AdminRepository) Save(ctx context.Context, admin *model.Admin) error {
	opts := options.Replace().SetUpsert(true)
	_, err := r.collection.ReplaceOne(ctx, bson.M{"_id": model.AdminID}, admin, opts)
	if err != nil {
		return fmt.Errorf("failed to save admin: %w", err)
	}
	return nil
}

func (r *AdminRepository) AddGroup(groupID int64) (added bool, err error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	admin, err := r.Get(ctx)
	if err != nil {
		return false, err
	}
	if !admin.AddGroup(groupID) {
		return false, nil
	}
	return true, r.Save(ctx, admin)
}

func (r *AdminRepository) RemoveGroup(groupID int64) (removed bool, err error) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	admin, err := r.Get(ctx)
	if err != nil {
		return false, err
	}
	if !admin.RemoveGroup(groupID) {
		return false, nil
	}
	return true, r.Save(ctx, admin)
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
