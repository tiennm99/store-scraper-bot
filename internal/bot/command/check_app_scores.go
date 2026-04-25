package command

import (
	"context"
	"fmt"
	"math"
	"strings"
	"time"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
	"github.com/miti99/store-scraper-bot-go/internal/api/apple"
	"github.com/miti99/store-scraper-bot-go/internal/api/google"
	"github.com/miti99/store-scraper-bot-go/internal/config"
	"github.com/miti99/store-scraper-bot-go/internal/model"
	"github.com/miti99/store-scraper-bot-go/internal/repository"
	"github.com/miti99/store-scraper-bot-go/internal/util"
)

// /checkappscore — Java CheckAppScoreCommand. Reports score + ratings count.
// Score is rounded to 1 decimal (Java Precision.round(score, 1) parity).
type CheckAppScoresCommand struct {
	cfg           *config.Config
	adminRepo     *repository.AdminRepository
	groupRepo     *repository.GroupRepository
	appleScraper  *apple.AppleScraper
	googleScraper *google.GoogleScraper
}

func NewCheckAppScoresCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository, a *apple.AppleScraper, g *google.GoogleScraper) *CheckAppScoresCommand {
	return &CheckAppScoresCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo, appleScraper: a, googleScraper: g}
}

func (c *CheckAppScoresCommand) Execute(msg *tgbotapi.Message, sender Sender) {
	if !authorizeGroup(msg.Chat.ID, c.adminRepo, sender) {
		return
	}
	if len(splitArgs(msg.CommandArguments())) != 0 {
		_ = sender.SendMessage(msg.Chat.ID, "Invalid arguments")
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), 90*time.Second)
	defer cancel()
	group, err := c.groupRepo.Get(ctx, msg.Chat.ID)
	if err != nil {
		_ = sender.SendMessage(msg.Chat.ID, "Internal server error")
		return
	}

	headers := []string{"AppId", "Score", "Ratings"}
	appleRows := c.appleScoreRows(group.AppleApps)
	googleRows := c.googleScoreRows(group.GoogleApps)

	var sb strings.Builder
	sb.WriteString("<b>Apple Apps</b>\n")
	if len(appleRows) == 0 {
		sb.WriteString("<i>(none)</i>\n")
	} else {
		sb.WriteString(fmt.Sprintf("<pre>%s</pre>\n", util.BuildTable(headers, appleRows)))
	}
	sb.WriteString("\n<b>Google Apps</b>\n")
	if len(googleRows) == 0 {
		sb.WriteString("<i>(none)</i>\n")
	} else {
		sb.WriteString(fmt.Sprintf("<pre>%s</pre>\n", util.BuildTable(headers, googleRows)))
	}
	_ = sender.SendMessage(msg.Chat.ID, sb.String())
}

func (c *CheckAppScoresCommand) appleScoreRows(apps []model.AppInfo) [][]string {
	rows := make([][]string, 0, len(apps))
	for _, a := range apps {
		resp, err := c.appleScraper.GetApp(a.AppID, a.Country)
		if err != nil || resp == nil {
			rows = append(rows, []string{a.AppID, "?", "?"})
			continue
		}
		rows = append(rows, []string{a.AppID, formatScore(resp.Score), fmt.Sprintf("%d", resp.Ratings)})
	}
	return rows
}

func (c *CheckAppScoresCommand) googleScoreRows(apps []model.AppInfo) [][]string {
	rows := make([][]string, 0, len(apps))
	for _, a := range apps {
		resp, err := c.googleScraper.GetApp(a.AppID, a.Country)
		if err != nil || resp == nil {
			rows = append(rows, []string{a.AppID, "?", "?"})
			continue
		}
		rows = append(rows, []string{a.AppID, formatScore(resp.Score), fmt.Sprintf("%d", resp.Ratings)})
	}
	return rows
}

// formatScore rounds to 1 decimal place (Java Precision.round(score, 1)).
func formatScore(score float64) string {
	rounded := math.Round(score*10) / 10
	return fmt.Sprintf("%.1f", rounded)
}
