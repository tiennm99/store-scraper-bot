package command

import "strings"

// splitArgs mirrors Java BotCommand argument parsing: split on whitespace,
// drop empty tokens.
func splitArgs(s string) []string {
	if s == "" {
		return nil
	}
	parts := strings.Fields(s)
	return parts
}
