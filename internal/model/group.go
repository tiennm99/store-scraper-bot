package model

type AppInfo struct {
	AppID   string `bson:"appId" json:"appId"`
	Country string `bson:"country" json:"country"`
}

type Group struct {
	Key        int64     `bson:"_id" json:"key"`
	AppleApps  []AppInfo `bson:"appleApps" json:"appleApps"`
	GoogleApps []AppInfo `bson:"googleApps" json:"googleApps"`
}

func NewGroup(groupID int64) *Group {
	return &Group{
		Key:        groupID,
		AppleApps:  make([]AppInfo, 0),
		GoogleApps: make([]AppInfo, 0),
	}
}

func (g *Group) AddAppleApp(appID, country string) bool {
	for _, app := range g.AppleApps {
		if app.AppID == appID && app.Country == country {
			return false // Already exists
		}
	}
	g.AppleApps = append(g.AppleApps, AppInfo{AppID: appID, Country: country})
	return true
}

func (g *Group) RemoveAppleApp(appID string) bool {
	for i, app := range g.AppleApps {
		if app.AppID == appID {
			g.AppleApps = append(g.AppleApps[:i], g.AppleApps[i+1:]...)
			return true
		}
	}
	return false
}

func (g *Group) AddGoogleApp(appID, country string) bool {
	for _, app := range g.GoogleApps {
		if app.AppID == appID && app.Country == country {
			return false // Already exists
		}
	}
	g.GoogleApps = append(g.GoogleApps, AppInfo{AppID: appID, Country: country})
	return true
}

func (g *Group) RemoveGoogleApp(appID string) bool {
	for i, app := range g.GoogleApps {
		if app.AppID == appID {
			g.GoogleApps = append(g.GoogleApps[:i], g.GoogleApps[i+1:]...)
			return true
		}
	}
	return false
}
