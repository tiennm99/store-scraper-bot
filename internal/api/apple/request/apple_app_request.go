package request

// AppleAppRequest mirrors Java AppleAppRequest record. Either ID (iTunes
// trackId) or AppID (bundleId) is set; the other is omitted from JSON.
type AppleAppRequest struct {
	ID      *int64  `json:"id,omitempty"`
	AppID   *string `json:"appId,omitempty"`
	Country string  `json:"country"`
	Ratings bool    `json:"ratings"`
}

func ByTrackID(id int64, country string) AppleAppRequest {
	return AppleAppRequest{ID: &id, Country: country, Ratings: true}
}

func ByBundleID(appID, country string) AppleAppRequest {
	return AppleAppRequest{AppID: &appID, Country: country, Ratings: true}
}
