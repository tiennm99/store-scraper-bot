package util

import (
	"fmt"
	"strings"
)

func BuildTable(headers []string, rows [][]string) string {
	if len(rows) == 0 {
		return ""
	}

	// Calculate column widths
	columnWidths := make([]int, len(headers))
	for i, header := range headers {
		columnWidths[i] = len(header)
	}

	for _, row := range rows {
		for i, cell := range row {
			if i < len(columnWidths) && len(cell) > columnWidths[i] {
				columnWidths[i] = len(cell)
			}
		}
	}

	// Build table
	var sb strings.Builder

	// Top border
	sb.WriteString("```\n")

	// Header
	for i, header := range headers {
		sb.WriteString(padRight(header, columnWidths[i]))
		if i < len(headers)-1 {
			sb.WriteString(" | ")
		}
	}
	sb.WriteString("\n")

	// Separator
	for i, width := range columnWidths {
		sb.WriteString(strings.Repeat("-", width))
		if i < len(columnWidths)-1 {
			sb.WriteString("-+-")
		}
	}
	sb.WriteString("\n")

	// Rows
	for _, row := range rows {
		for i, cell := range row {
			if i < len(columnWidths) {
				sb.WriteString(padRight(cell, columnWidths[i]))
				if i < len(row)-1 {
					sb.WriteString(" | ")
				}
			}
		}
		sb.WriteString("\n")
	}

	// Bottom border
	sb.WriteString("```")

	return sb.String()
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
	return s[:maxLen-3] + "..."
}

func FormatNumber(n int64) string {
	if n >= 1000000 {
		return fmt.Sprintf("%.1fM", float64(n)/1000000)
	} else if n >= 1000 {
		return fmt.Sprintf("%.1fK", float64(n)/1000)
	}
	return fmt.Sprintf("%d", n)
}
