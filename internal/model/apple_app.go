package model

// AppleAppResponse mirrors Java AppleAppResponse record (api/apple/response).
type AppleAppResponse struct {
	ID                    int64            `bson:"id" json:"id"`
	AppID                 string           `bson:"appId" json:"appId"`
	Title                 string           `bson:"title" json:"title"`
	URL                   string           `bson:"url" json:"url"`
	Description           string           `bson:"description" json:"description"`
	Icon                  string           `bson:"icon" json:"icon"`
	Genres                []string         `bson:"genres" json:"genres"`
	GenreIDs              []string         `bson:"genreIds" json:"genreIds"`
	PrimaryGenre          string           `bson:"primaryGenre" json:"primaryGenre"`
	PrimaryGenreID        int              `bson:"primaryGenreId" json:"primaryGenreId"`
	ContentRating         string           `bson:"contentRating" json:"contentRating"`
	Languages             []string         `bson:"languages" json:"languages"`
	Size                  string           `bson:"size" json:"size"`
	RequiredOsVersion     string           `bson:"requiredOsVersion" json:"requiredOsVersion"`
	Released              string           `bson:"released" json:"released"`
	Updated               string           `bson:"updated" json:"updated"` // ISO 8601
	ReleaseNotes          string           `bson:"releaseNotes" json:"releaseNotes"`
	Version               string           `bson:"version" json:"version"`
	Price                 float64          `bson:"price" json:"price"`
	Currency              string           `bson:"currency" json:"currency"`
	Free                  bool             `bson:"free" json:"free"`
	DeveloperID           int64            `bson:"developerId" json:"developerId"`
	Developer             string           `bson:"developer" json:"developer"`
	DeveloperURL          string           `bson:"developerUrl" json:"developerUrl"`
	DeveloperWebsite      string           `bson:"developerWebsite" json:"developerWebsite"`
	Score                 float64          `bson:"score" json:"score"`
	Reviews               int              `bson:"reviews" json:"reviews"`
	CurrentVersionScore   float64          `bson:"currentVersionScore" json:"currentVersionScore"`
	CurrentVersionReviews int              `bson:"currentVersionReviews" json:"currentVersionReviews"`
	Screenshots           []string         `bson:"screenshots" json:"screenshots"`
	IpadScreenshots       []string         `bson:"ipadScreenshots" json:"ipadScreenshots"`
	AppletvScreenshots    []string         `bson:"appletvScreenshots" json:"appletvScreenshots"`
	SupportedDevices      []string         `bson:"supportedDevices" json:"supportedDevices"`
	Ratings               int64            `bson:"ratings" json:"ratings"`
	Histogram             map[string]int64 `bson:"histogram" json:"histogram"`
}

type AppleApp struct {
	AbstractModel `bson:",inline"`
	App           AppleAppResponse `bson:"app" json:"app"`
	Millis        int64            `bson:"millis" json:"millis"` // cache timestamp (ms since epoch)
}

func NewAppleApp(appID string, response AppleAppResponse, millis int64) *AppleApp {
	return &AppleApp{
		AbstractModel: AbstractModel{ID: appID, Class: "AppleApp"},
		App:           response,
		Millis:        millis,
	}
}

// IsExpired reports whether the cache entry is older than cacheMillis.
func (a *AppleApp) IsExpired(nowMillis, cacheMillis int64) bool {
	return nowMillis-a.Millis > cacheMillis
}
