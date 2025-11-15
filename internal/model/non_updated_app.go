package model

type NonUpdatedApp struct {
	AppID   string
	Title   string
	Days    int
	Updated string
	Score   float64
	Reviews interface{} // Can be int or string
	Ratings int64
	IsApple bool
}
