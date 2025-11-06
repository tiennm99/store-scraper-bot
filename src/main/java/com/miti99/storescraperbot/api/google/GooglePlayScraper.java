package com.miti99.storescraperbot.api.google;

import com.miti99.storescraperbot.api.google.request.GoogleAppRequest;
import com.miti99.storescraperbot.api.google.response.GoogleAppResponse;
import com.miti99.storescraperbot.repository.GoogleAppRepository;
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
public class GooglePlayScraper {
  public static final String BASE_URL = "https://miti-google-play-scraper.vercel.app/";

  @SneakyThrows
  public static GoogleAppResponse app(GoogleAppRequest request) {
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/app"))
            // .timeout(Duration.ofMillis(TIMEOUT))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(GsonUtil.toJson(request)))
            .build();

    try (var httpClient =
        HttpClient.newBuilder()
            // .connectTimeout(Duration.ofMillis(TIMEOUT))
            .followRedirects(Redirect.NORMAL)
            .build()) {
      var body = httpClient.send(httpRequest, BodyHandlers.ofString()).body();
      return GsonUtil.fromJson(body, GoogleAppResponse.class);
    }
  }

  private static GoogleAppResponse getResponse(String appId, String country) {
    boolean isInCache = GoogleAppRepository.INSTANCE.exist(appId);
    if (isInCache) {
      var app = GoogleAppRepository.INSTANCE.load(appId);
      return app.getApp();
    } else {
      var response = app(new GoogleAppRequest(appId, country));
      GoogleAppRepository.INSTANCE.init(appId);
      var app = GoogleAppRepository.INSTANCE.load(appId);
      app.setApp(response);
      GoogleAppRepository.INSTANCE.save(appId, app);
      return response;
    }
  }

  public static LocalDate getLastUpdateOfApp(String appId, String country) {
    var response = getResponse(appId, country);
    long lastUpdateMillis = 0;
    if (response != null) {
      lastUpdateMillis = response.updated();
    } else {
      log.error("response is null");
    }
    return LocalDate.ofInstant(Instant.ofEpochMilli(lastUpdateMillis), ZoneId.systemDefault());
  }

  public static double getAppScore(String appId, String country) {
    var response = getResponse(appId, country);
    if (response == null) {
      log.error("response is null");
      return 0.0;
    }
    return response.score();
  }

  public static long getAppRatings(String appId, String country) {
    var response = getResponse(appId, country);
    if (response == null) {
      log.error("response is null");
      return 0L;
    }
    return response.ratings();
  }
}
