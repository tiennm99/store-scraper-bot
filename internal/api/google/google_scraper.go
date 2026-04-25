package google

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/miti99/store-scraper-bot-go/internal/api/google/request"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"go.uber.org/zap"
)

// BaseURL mirrors Java GooglePlayScraper (api/google/GooglePlayScraper.java).
const BaseURL = "https://store-scraper.vercel.app/google"

type GoogleScraper struct {
	repo   *repository.GoogleAppRepository
	cfg    *config.Config
	client *http.Client
	logger *zap.Logger
}

func NewGoogleScraper(repo *repository.GoogleAppRepository, cfg *config.Config) *GoogleScraper {
	return &GoogleScraper{
		repo:   repo,
		cfg:    cfg,
		client: &http.Client{Timeout: 30 * time.Second},
		logger: cfg.Logger,
	}
}

func (s *GoogleScraper) RawApp(req request.GoogleAppRequest) (string, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return "", fmt.Errorf("marshal google request: %w", err)
	}
	httpReq, err := http.NewRequestWithContext(context.Background(), http.MethodPost, BaseURL+"/app", bytes.NewReader(body))
	if err != nil {
		return "", fmt.Errorf("build google request: %w", err)
	}
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := s.client.Do(httpReq)
	if err != nil {
		return "", fmt.Errorf("google HTTP error: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("google HTTP status %d", resp.StatusCode)
	}
	raw, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("read google body: %w", err)
	}
	return string(raw), nil
}

func (s *GoogleScraper) App(req request.GoogleAppRequest) (*model.GoogleAppResponse, error) {
	raw, err := s.RawApp(req)
	if err != nil {
		return nil, err
	}
	out := &model.GoogleAppResponse{}
	if err := json.Unmarshal([]byte(raw), out); err != nil {
		return nil, fmt.Errorf("decode google response: %w", err)
	}
	return out, nil
}

func (s *GoogleScraper) GetApp(appID, country string) (*model.GoogleAppResponse, error) {
	if cached, _ := s.repo.GetCached(appID); cached != nil {
		return &cached.App, nil
	}
	resp, err := s.App(request.New(appID, country))
	if err != nil {
		return nil, err
	}
	s.cache(resp, appID)
	return resp, nil
}

func (s *GoogleScraper) FetchAndCache(req request.GoogleAppRequest) (*model.GoogleAppResponse, error) {
	resp, err := s.App(req)
	if err != nil {
		return nil, err
	}
	s.cache(resp, req.AppID)
	return resp, nil
}

func (s *GoogleScraper) cache(resp *model.GoogleAppResponse, fallbackID string) {
	if resp == nil {
		return
	}
	id := resp.AppID
	if id == "" {
		id = fallbackID
	}
	if id == "" {
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	entry := model.NewGoogleApp(id, *resp, time.Now().UnixMilli())
	if err := s.repo.Save(ctx, entry); err != nil {
		s.logger.Warn("failed to cache google app", zap.String("appId", id), zap.Error(err))
	}
}
