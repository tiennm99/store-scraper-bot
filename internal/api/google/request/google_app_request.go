package request

// GoogleAppRequest mirrors Java GoogleAppRequest record. Country defaults to "vn".
type GoogleAppRequest struct {
	AppID   string `json:"appId"`
	Country string `json:"country"`
}

func New(appID, country string) GoogleAppRequest {
	if country == "" {
		country = "vn"
	}
	return GoogleAppRequest{AppID: appID, Country: country}
}
