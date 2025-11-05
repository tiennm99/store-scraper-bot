package com.miti99.storescraperbot.api.apple;

import com.miti99.storescraperbot.api.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.api.apple.response.AppleAppResponse;
import com.miti99.storescraperbot.repository.AppleAppRepository;
import com.miti99.storescraperbot.util.GsonUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppStoreScraper {
  public static final String BASE_URL = "https://miti-app-store-scraper.vercel.app/";

  @SneakyThrows
  public static AppleAppResponse app(AppleAppRequest request) {
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/app"))
            // .timeout(Duration.ofMillis(TIMEOUT))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(GsonUtil.toJson(request)))
            .build();

    try (var httpClient =
        HttpClient.newBuilder()
            .followRedirects(Redirect.NORMAL)
            // .connectTimeout(Duration.ofMillis(TIMEOUT))
            .build()) {
      var body = httpClient.send(httpRequest, BodyHandlers.ofString()).body();
      return GsonUtil.fromJson(body, AppleAppResponse.class);
    }
  }

  public static LocalDate getLastUpdateOfApp(String appId) {
    boolean isInCache = AppleAppRepository.INSTANCE.exist(appId);
    AppleAppResponse response = null;
    if (isInCache) {
      var app = AppleAppRepository.INSTANCE.load(appId);
      response = app.getApp();
    } else {
      response = app(new AppleAppRequest(appId));
      AppleAppRepository.INSTANCE.init(appId);
      var app = AppleAppRepository.INSTANCE.load(appId);
      app.setApp(response);
      AppleAppRepository.INSTANCE.save(appId, app);
    }
    if (response != null) {
      return LocalDate.ofInstant(Instant.parse(response.updated()), ZoneId.systemDefault());
    } else {
      log.error("response is null");
      return LocalDate.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
    }
  }
}
