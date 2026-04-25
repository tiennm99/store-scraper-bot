package model

// Category mirrors Java GoogleAppResponse.Category nested record.
type Category struct {
	Name string `bson:"name" json:"name"`
	ID   string `bson:"id" json:"id"`
}

// Feature mirrors Java GoogleAppResponse.Feature nested record.
type Feature struct {
	Title       string `bson:"title" json:"title"`
	Description string `bson:"description" json:"description"`
}

// GoogleAppResponse mirrors Java GoogleAppResponse record (api/google/response).
type GoogleAppResponse struct {
	Title                    string           `bson:"title" json:"title"`
	Description              string           `bson:"description" json:"description"`
	DescriptionHTML          string           `bson:"descriptionHTML" json:"descriptionHTML"`
	Summary                  string           `bson:"summary" json:"summary"`
	Installs                 string           `bson:"installs" json:"installs"`
	MinInstalls              int64            `bson:"minInstalls" json:"minInstalls"`
	MaxInstalls              int64            `bson:"maxInstalls" json:"maxInstalls"`
	Score                    float64          `bson:"score" json:"score"`
	ScoreText                string           `bson:"scoreText" json:"scoreText"`
	Ratings                  int64            `bson:"ratings" json:"ratings"`
	Reviews                  int64            `bson:"reviews" json:"reviews"`
	Histogram                map[string]int64 `bson:"histogram" json:"histogram"`
	Price                    float64          `bson:"price" json:"price"`
	Free                     bool             `bson:"free" json:"free"`
	Currency                 string           `bson:"currency" json:"currency"`
	PriceText                string           `bson:"priceText" json:"priceText"`
	OffersIAP                bool             `bson:"offersIAP" json:"offersIAP"`
	IAPRange                 string           `bson:"IAPRange" json:"IAPRange"`
	AndroidVersion           string           `bson:"androidVersion" json:"androidVersion"`
	AndroidVersionText       string           `bson:"androidVersionText" json:"androidVersionText"`
	AndroidMaxVersion        string           `bson:"androidMaxVersion" json:"androidMaxVersion"`
	Developer                string           `bson:"developer" json:"developer"`
	DeveloperID              string           `bson:"developerId" json:"developerId"`
	DeveloperEmail           string           `bson:"developerEmail" json:"developerEmail"`
	DeveloperWebsite         string           `bson:"developerWebsite" json:"developerWebsite"`
	DeveloperAddress         string           `bson:"developerAddress" json:"developerAddress"`
	DeveloperLegalName       string           `bson:"developerLegalName" json:"developerLegalName"`
	DeveloperLegalEmail      string           `bson:"developerLegalEmail" json:"developerLegalEmail"`
	DeveloperLegalAddress    string           `bson:"developerLegalAddress" json:"developerLegalAddress"`
	DeveloperLegalPhoneNumber string          `bson:"developerLegalPhoneNumber" json:"developerLegalPhoneNumber"`
	PrivacyPolicy            string           `bson:"privacyPolicy" json:"privacyPolicy"`
	DeveloperInternalID      string           `bson:"developerInternalID" json:"developerInternalID"`
	Genre                    string           `bson:"genre" json:"genre"`
	GenreID                  string           `bson:"genreId" json:"genreId"`
	Categories               []Category       `bson:"categories" json:"categories"`
	Icon                     string           `bson:"icon" json:"icon"`
	HeaderImage              string           `bson:"headerImage" json:"headerImage"`
	Screenshots              []string         `bson:"screenshots" json:"screenshots"`
	Video                    string           `bson:"video" json:"video"`
	VideoImage               string           `bson:"videoImage" json:"videoImage"`
	PreviewVideo             string           `bson:"previewVideo" json:"previewVideo"`
	ContentRating            string           `bson:"contentRating" json:"contentRating"`
	ContentRatingDescription string           `bson:"contentRatingDescription" json:"contentRatingDescription"`
	AdSupported              bool             `bson:"adSupported" json:"adSupported"`
	Released                 string           `bson:"released" json:"released"`
	Updated                  int64            `bson:"updated" json:"updated"` // ms since epoch
	Version                  string           `bson:"version" json:"version"`
	RecentChanges            string           `bson:"recentChanges" json:"recentChanges"`
	Comments                 []string         `bson:"comments" json:"comments"`
	Preregister              bool             `bson:"preregister" json:"preregister"`
	EarlyAccessEnabled       bool             `bson:"earlyAccessEnabled" json:"earlyAccessEnabled"`
	IsAvailableInPlayPass    bool             `bson:"isAvailableInPlayPass" json:"isAvailableInPlayPass"`
	EditorsChoice            bool             `bson:"editorsChoice" json:"editorsChoice"`
	Features                 []Feature        `bson:"features" json:"features"`
	AppID                    string           `bson:"appId" json:"appId"`
	URL                      string           `bson:"url" json:"url"`
}

type GoogleApp struct {
	AbstractModel `bson:",inline"`
	App           GoogleAppResponse `bson:"app" json:"app"`
	Millis        int64             `bson:"millis" json:"millis"`
}

func NewGoogleApp(appID string, response GoogleAppResponse, millis int64) *GoogleApp {
	return &GoogleApp{
		AbstractModel: AbstractModel{ID: appID, Class: "GoogleApp"},
		App:           response,
		Millis:        millis,
	}
}

func (g *GoogleApp) IsExpired(nowMillis, cacheMillis int64) bool {
	return nowMillis-g.Millis > cacheMillis
}
