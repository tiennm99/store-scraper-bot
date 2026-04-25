package apple

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/miti99/store-scraper-bot-go/internal/api/apple/request"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"go.uber.org/zap"
)

// BaseURL mirrors Java AppStoreScraper (api/apple/AppStoreScraper.java).
const BaseURL = "https://store-scraper.vercel.app/apple"

type AppleScraper struct {
	repo   *repository.AppleAppRepository
	cfg    *config.Config
	client *http.Client
	logger *zap.Logger
}

func NewAppleScraper(repo *repository.AppleAppRepository, cfg *config.Config) *AppleScraper {
	return &AppleScraper{
		repo:   repo,
		cfg:    cfg,
		client: &http.Client{Timeout: 30 * time.Second},
		logger: cfg.Logger,
	}
}

// RawApp posts the request and returns the raw JSON body.
func (s *AppleScraper) RawApp(req request.AppleAppRequest) (string, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return "", fmt.Errorf("marshal apple request: %w", err)
	}
	httpReq, err := http.NewRequestWithContext(context.Background(), http.MethodPost, BaseURL+"/app", bytes.NewReader(body))
	if err != nil {
		return "", fmt.Errorf("build apple request: %w", err)
	}
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := s.client.Do(httpReq)
	if err != nil {
		return "", fmt.Errorf("apple HTTP error: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("apple HTTP status %d", resp.StatusCode)
	}
	raw, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("read apple body: %w", err)
	}
	return string(raw), nil
}

// App posts the request and decodes the response.
func (s *AppleScraper) App(req request.AppleAppRequest) (*model.AppleAppResponse, error) {
	raw, err := s.RawApp(req)
	if err != nil {
		return nil, err
	}
	out := &model.AppleAppResponse{}
	if err := json.Unmarshal([]byte(raw), out); err != nil {
		return nil, fmt.Errorf("decode apple response: %w", err)
	}
	return out, nil
}

// GetApp returns a cached response (if fresh) or fetches by bundleId and caches.
func (s *AppleScraper) GetApp(appID, country string) (*model.AppleAppResponse, error) {
	if cached, _ := s.repo.GetCached(appID); cached != nil {
		return &cached.App, nil
	}
	resp, err := s.App(request.ByBundleID(appID, country))
	if err != nil {
		return nil, err
	}
	s.cache(resp)
	return resp, nil
}

// FetchAndCache fetches by an arbitrary request (track ID or bundle ID).
func (s *AppleScraper) FetchAndCache(req request.AppleAppRequest) (*model.AppleAppResponse, error) {
	resp, err := s.App(req)
	if err != nil {
		return nil, err
	}
	s.cache(resp)
	return resp, nil
}

func (s *AppleScraper) cache(resp *model.AppleAppResponse) {
	if resp == nil || resp.AppID == "" {
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	entry := model.NewAppleApp(resp.AppID, *resp, time.Now().UnixMilli())
	if err := s.repo.Save(ctx, entry); err != nil {
		s.logger.Warn("failed to cache apple app", zap.String("appId", resp.AppID), zap.Error(err))
	}
}
