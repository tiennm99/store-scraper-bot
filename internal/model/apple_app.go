package model

import "time"

type AppleApp struct {
	Key       string            `bson:"_id" json:"key"`
	App       AppleAppResponse  `bson:"app" json:"app"`
	UpdatedAt time.Time         `bson:"updatedAt" json:"updatedAt"`
}

type AppleAppResponse struct {
	ID                 int64             `json:"id"`
	AppID              string            `json:"appId"`
	Title              string            `json:"title"`
	URL                string            `json:"url"`
	Description        string            `json:"description"`
	Icon               string            `json:"icon"`
	Genres             []string          `json:"genres"`
	PrimaryGenre       string            `json:"primaryGenre"`
	ContentRating      string            `json:"contentRating"`
	Size               string            `json:"size"`
	RequiredOsVersion  string            `json:"requiredOsVersion"`
	Released           string            `json:"released"`
	Updated            string            `json:"updated"` // ISO 8601 timestamp
	Version            string            `json:"version"`
	Price              float64           `json:"price"`
	Currency           string            `json:"currency"`
	Free               bool              `json:"free"`
	DeveloperID        int64             `json:"developerId"`
	Developer          string            `json:"developer"`
	DeveloperURL       string            `json:"developerUrl"`
	Score              float64           `json:"score"`
	Reviews            int               `json:"reviews"`
	Ratings            int64             `json:"ratings"`
	Screenshots        []string          `json:"screenshots"`
	Histogram          map[string]int64  `json:"histogram"`
}

func NewAppleApp(appID string, response AppleAppResponse) *AppleApp {
	return &AppleApp{
		Key:       appID,
		App:       response,
		UpdatedAt: time.Now(),
	}
}

func (a *AppleApp) IsExpired(cacheSeconds int) bool {
	return time.Since(a.UpdatedAt).Seconds() > float64(cacheSeconds)
}
