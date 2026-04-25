package model

import "strconv"

// AppInfo mirrors Java AppleAppInfo / GoogleAppInfo records.
type AppInfo struct {
	AppID   string `bson:"appId" json:"appId"`
	Country string `bson:"country" json:"country"`
}

type Group struct {
	AbstractModel `bson:",inline"`
	AppleApps     []AppInfo `bson:"appleApps" json:"appleApps"`
	GoogleApps    []AppInfo `bson:"googleApps" json:"googleApps"`
}

// GroupIDToKey converts a Telegram chat ID to the string _id used by Java.
func GroupIDToKey(groupID int64) string {
	return strconv.FormatInt(groupID, 10)
}

// GroupKeyToID parses a stored _id back to int64.
func GroupKeyToID(key string) (int64, error) {
	return strconv.ParseInt(key, 10, 64)
}

func NewGroup(groupID int64) *Group {
	return &Group{
		AbstractModel: AbstractModel{ID: GroupIDToKey(groupID), Class: "Group"},
		AppleApps:     []AppInfo{},
		GoogleApps:    []AppInfo{},
	}
}

// GroupID returns the int64 chat ID parsed from the stored string _id.
// Returns 0 if parsing fails (matches Java behaviour where _id always parses).
func (g *Group) GroupID() int64 {
	id, _ := GroupKeyToID(g.ID)
	return id
}

func (g *Group) AddAppleApp(appID, country string) bool {
	for _, app := range g.AppleApps {
		if app.AppID == appID {
			return false
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
		if app.AppID == appID {
			return false
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
