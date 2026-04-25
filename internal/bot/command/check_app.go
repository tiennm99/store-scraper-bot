package command

import (
	"context"
	"fmt"
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

// /checkapp — Java CheckAppCommand. Reports update status per app, per store.
type CheckAppCommand struct {
	cfg           *config.Config
	adminRepo     *repository.AdminRepository
	groupRepo     *repository.GroupRepository
	appleScraper  *apple.AppleScraper
	googleScraper *google.GoogleScraper
}

func NewCheckAppCommand(cfg *config.Config, adminRepo *repository.AdminRepository, groupRepo *repository.GroupRepository, a *apple.AppleScraper, g *google.GoogleScraper) *CheckAppCommand {
	return &CheckAppCommand{cfg: cfg, adminRepo: adminRepo, groupRepo: groupRepo, appleScraper: a, googleScraper: g}
}

func (c *CheckAppCommand) Execute(msg *tgbotapi.Message, sender Sender) {
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
	now := time.Now()
	threshold := c.cfg.NumDaysWarningNotUpdated

	headers := []string{"AppId", "Updated", "Days", "OK"}
	appleRows := c.appleRows(group.AppleApps, now, threshold)
	googleRows := c.googleRows(group.GoogleApps, now, threshold)

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

func (c *CheckAppCommand) appleRows(apps []model.AppInfo, now time.Time, threshold int) [][]string {
	rows := make([][]string, 0, len(apps))
	for _, a := range apps {
		resp, err := c.appleScraper.GetApp(a.AppID, a.Country)
		if err != nil || resp == nil {
			rows = append(rows, []string{a.AppID, "?", "?", okMark(false)})
			continue
		}
		updated, days, ok := evalAppleUpdated(resp.Updated, now, threshold)
		rows = append(rows, []string{a.AppID, updated, fmt.Sprintf("%d", days), okMark(ok)})
	}
	return rows
}

func (c *CheckAppCommand) googleRows(apps []model.AppInfo, now time.Time, threshold int) [][]string {
	rows := make([][]string, 0, len(apps))
	for _, a := range apps {
		resp, err := c.googleScraper.GetApp(a.AppID, a.Country)
		if err != nil || resp == nil {
			rows = append(rows, []string{a.AppID, "?", "?", okMark(false)})
			continue
		}
		updated, days, ok := evalGoogleUpdated(resp.Updated, now, threshold)
		rows = append(rows, []string{a.AppID, updated, fmt.Sprintf("%d", days), okMark(ok)})
	}
	return rows
}

// evalAppleUpdated parses Apple's ISO 8601 timestamp and returns (yyyy-MM-dd,
// days since update, OK).
func evalAppleUpdated(updated string, now time.Time, threshold int) (string, int, bool) {
	t, err := time.Parse(time.RFC3339, updated)
	if err != nil {
		return updated, 0, false
	}
	days := int(now.Sub(t).Hours() / 24)
	return t.Format("2006-01-02"), days, days <= threshold
}

func evalGoogleUpdated(millis int64, now time.Time, threshold int) (string, int, bool) {
	t := time.UnixMilli(millis)
	days := int(now.Sub(t).Hours() / 24)
	return t.Format("2006-01-02"), days, days <= threshold
}

func okMark(ok bool) string {
	if ok {
		return "✅"
	}
	return "❌"
}
