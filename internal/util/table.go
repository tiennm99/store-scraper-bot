package util

import (
	"fmt"
	"strings"
)

// BuildTable mirrors Java bot/table/Table.java:
//   - left-aligned columns padded to max(header, cell) width
//   - "│" column separator
//   - row separator inserted every 5 rows using "─" cells joined by "─┼─"
//
// Output is intended to be wrapped in <pre> for Telegram HTML rendering.
func BuildTable(headers []string, rows [][]string) string {
	widths := computeWidths(headers, rows)
	var sb strings.Builder

	writeRow(&sb, headers, widths)
	sb.WriteString("\n")
	writeSeparator(&sb, widths)

	for i, row := range rows {
		sb.WriteString("\n")
		if i > 0 && i%5 == 0 {
			writeSeparator(&sb, widths)
			sb.WriteString("\n")
		}
		writeRow(&sb, row, widths)
	}
	return sb.String()
}

func computeWidths(headers []string, rows [][]string) []int {
	widths := make([]int, len(headers))
	for i, h := range headers {
		widths[i] = len(h)
	}
	for _, row := range rows {
		for i, cell := range row {
			if i < len(widths) && len(cell) > widths[i] {
				widths[i] = len(cell)
			}
		}
	}
	return widths
}

func writeRow(sb *strings.Builder, cells []string, widths []int) {
	for i, w := range widths {
		var cell string
		if i < len(cells) {
			cell = cells[i]
		}
		sb.WriteString(padRight(cell, w))
		if i < len(widths)-1 {
			sb.WriteString(" │ ")
		}
	}
}

func writeSeparator(sb *strings.Builder, widths []int) {
	for i, w := range widths {
		sb.WriteString(strings.Repeat("─", w))
		if i < len(widths)-1 {
			sb.WriteString("─┼─")
		}
	}
}

func padRight(s string, length int) string {
	if len(s) >= length {
		return s
	}
	return s + strings.Repeat(" ", length-len(s))
}

func TruncateString(s string, maxLen int) string {
	if len(s) <= maxLen {
		return s
	}
	if maxLen <= 3 {
		return s[:maxLen]
	}
	return s[:maxLen-3] + "..."
}

func FormatNumber(n int64) string {
	if n >= 1_000_000 {
		return fmt.Sprintf("%.1fM", float64(n)/1_000_000)
	}
	if n >= 1_000 {
		return fmt.Sprintf("%.1fK", float64(n)/1_000)
	}
	return fmt.Sprintf("%d", n)
}
