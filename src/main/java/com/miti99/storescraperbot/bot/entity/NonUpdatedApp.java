package com.miti99.storescraperbot.bot.entity;

import java.time.LocalDate;

public record NonUpdatedApp(String appId, LocalDate updated, long days) {}
