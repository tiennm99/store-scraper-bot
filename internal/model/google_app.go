package model

import "time"

type GoogleApp struct {
	Key       string             `bson:"_id" json:"key"`
	App       GoogleAppResponse  `bson:"app" json:"app"`
	UpdatedAt time.Time          `bson:"updatedAt" json:"updatedAt"`
}

type GoogleAppResponse struct {
	Title          string            `json:"title"`
	Description    string            `json:"description"`
	Installs       string            `json:"installs"`
	MinInstalls    int64             `json:"minInstalls"`
	MaxInstalls    int64             `json:"maxInstalls"`
	Score          float64           `json:"score"`
	ScoreText      string            `json:"scoreText"`
	Ratings        int64             `json:"ratings"`
	Reviews        int64             `json:"reviews"`
	Histogram      map[string]int64  `json:"histogram"`
	Price          float64           `json:"price"`
	Free           bool              `json:"free"`
	Currency       string            `json:"currency"`
	Developer      string            `json:"developer"`
	Genre          string            `json:"genre"`
	Icon           string            `json:"icon"`
	HeaderImage    string            `json:"headerImage"`
	Screenshots    []string          `json:"screenshots"`
	ContentRating  string            `json:"contentRating"`
	AdSupported    bool              `json:"adSupported"`
	Updated        int64             `json:"updated"` // Milliseconds since epoch
	Version        string            `json:"version"`
	AppID          string            `json:"appId"`
	URL            string            `json:"url"`
}

func NewGoogleApp(appID string, response GoogleAppResponse) *GoogleApp {
	return &GoogleApp{
		Key:       appID,
		App:       response,
		UpdatedAt: time.Now(),
	}
}

func (g *GoogleApp) IsExpired(cacheSeconds int) bool {
	return time.Since(g.UpdatedAt).Seconds() > float64(cacheSeconds)
}
