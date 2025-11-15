package apple

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

const appleAPIURL = "https://store-scraper.vercel.app/apple/app"

type AppleAppRequest struct {
	ID      *int64  `json:"id,omitempty"`
	AppID   *string `json:"appId,omitempty"`
	Country string  `json:"country"`
	Ratings bool    `json:"ratings"`
}

type AppleScraper struct {
	httpClient *http.Client
	appRepo    *repository.AppleAppRepository
	logger     *zap.Logger
}

func NewAppleScraper(appRepo *repository.AppleAppRepository, cfg *config.Config) *AppleScraper {
	return &AppleScraper{
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
		appRepo: appRepo,
		logger:  cfg.Logger,
	}
}

func (s *AppleScraper) GetApp(appID, country string) (*model.AppleAppResponse, error) {
	// Check cache first
	cachedApp, err := s.appRepo.GetCached(appID)
	if err != nil {
		s.logger.Error("Failed to get cached apple app", zap.Error(err), zap.String("appId", appID))
	}
	if cachedApp != nil {
		s.logger.Debug("Returning cached apple app", zap.String("appId", appID))
		return &cachedApp.App, nil
	}

	// Fetch from API
	s.logger.Info("Fetching apple app from API", zap.String("appId", appID), zap.String("country", country))
	response, err := s.fetchFromAPI(appID, country)
	if err != nil {
		return nil, err
	}

	// Save to cache
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	appleApp := model.NewAppleApp(appID, *response)
	if err := s.appRepo.Save(ctx, appleApp); err != nil {
		s.logger.Error("Failed to save apple app to cache", zap.Error(err), zap.String("appId", appID))
	}

	return response, nil
}

func (s *AppleScraper) fetchFromAPI(appID, country string) (*model.AppleAppResponse, error) {
	request := AppleAppRequest{
		AppID:   &appID,
		Country: country,
		Ratings: true,
	}

	requestBody, err := json.Marshal(request)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	req, err := http.NewRequest("POST", appleAPIURL, bytes.NewBuffer(requestBody))
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

	var response model.AppleAppResponse
	if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &response, nil
}

func (s *AppleScraper) GetAppUpdated(appID, country string) (string, error) {
	app, err := s.GetApp(appID, country)
	if err != nil {
		return "", err
	}
	return app.Updated, nil
}
