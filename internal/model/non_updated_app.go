package model

// NonUpdatedApp is a transient (not persisted) struct used by the daily check
// job to report apps not updated in N days.
type NonUpdatedApp struct {
	AppID   string
	Title   string
	Days    int
	Updated string
	Score   float64
	Reviews int64
	Ratings int64
	IsApple bool
}
