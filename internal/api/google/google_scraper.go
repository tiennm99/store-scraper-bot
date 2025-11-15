package google

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"go.uber.org/zap"
)

const googleAPIURL = "https://store-scraper.vercel.app/google/app"

type GoogleAppRequest struct {
	AppID   string `json:"appId"`
	Country string `json:"country"`
}

type GoogleScraper struct {
	httpClient *http.Client
	appRepo    *repository.GoogleAppRepository
	logger     *zap.Logger
}

func NewGoogleScraper(appRepo *repository.GoogleAppRepository, cfg *config.Config) *GoogleScraper {
	return &GoogleScraper{
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
		appRepo: appRepo,
		logger:  cfg.Logger,
	}
}

func (s *GoogleScraper) GetApp(appID, country string) (*model.GoogleAppResponse, error) {
	// Check cache first
	cachedApp, err := s.appRepo.GetCached(appID)
	if err != nil {
		s.logger.Error("Failed to get cached google app", zap.Error(err), zap.String("appId", appID))
	}
	if cachedApp != nil {
		s.logger.Debug("Returning cached google app", zap.String("appId", appID))
		return &cachedApp.App, nil
	}

	// Fetch from API
	s.logger.Info("Fetching google app from API", zap.String("appId", appID), zap.String("country", country))
	response, err := s.fetchFromAPI(appID, country)
	if err != nil {
		return nil, err
	}

	// Save to cache
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	googleApp := model.NewGoogleApp(appID, *response)
	if err := s.appRepo.Save(ctx, googleApp); err != nil {
		s.logger.Error("Failed to save google app to cache", zap.Error(err), zap.String("appId", appID))
	}

	return response, nil
}

func (s *GoogleScraper) fetchFromAPI(appID, country string) (*model.GoogleAppResponse, error) {
	request := GoogleAppRequest{
		AppID:   appID,
		Country: country,
	}

	requestBody, err := json.Marshal(request)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	req, err := http.NewRequest("POST", googleAPIURL, bytes.NewBuffer(requestBody))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("API returned status code: %d", resp.StatusCode)
	}

	var response model.GoogleAppResponse
	if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &response, nil
}

func (s *GoogleScraper) GetLastUpdate(appID, country string) (int64, error) {
	app, err := s.GetApp(appID, country)
	if err != nil {
		return 0, err
	}
	return app.Updated, nil
}
