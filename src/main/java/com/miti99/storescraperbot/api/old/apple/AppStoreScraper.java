package com.miti99.storescraperbot.api.old.apple;

import com.miti99.storescraperbot.api.old.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.api.old.apple.response.AppleAppResponse;
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
  public static String rawApp(AppleAppRequest request) {
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
      return httpClient.send(httpRequest, BodyHandlers.ofString()).body();
    } catch (Exception e) {
      log.error("rawAppResponse error - request: '{}'", GsonUtil.toJson(request), e);
      return null;
    }
  }

  @SneakyThrows
  public static AppleAppResponse app(AppleAppRequest request) {
    return GsonUtil.fromJson(rawApp(request), AppleAppResponse.class);
  }

  public static AppleAppResponse getAppResponse(String appId, String country) {
    boolean isInCache = AppleAppRepository.INSTANCE.exist(appId);
    if (isInCache) {
      var app = AppleAppRepository.INSTANCE.load(appId);
      return app.app();
    } else {
      var response = app(new AppleAppRequest(appId, country));
      AppleAppRepository.INSTANCE.init(appId);
      var app = AppleAppRepository.INSTANCE.load(appId);
      app.app(response);
      AppleAppRepository.INSTANCE.save(appId, app);
      return response;
    }
  }

  public static LocalDate getAppUpdated(String appId, String country) {
    var response = getAppResponse(appId, country);
    if (response == null) {
      log.error("response is null");
      return LocalDate.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
    }
    return LocalDate.ofInstant(Instant.parse(response.updated()), ZoneId.systemDefault());
  }

  public static double getAppScore(String appId, String country) {
    var response = getAppResponse(appId, country);
    if (response == null) {
      log.error("response is null");
      return 0.0;
    }
    return response.score();
  }

  public static long getAppReviews(String appId, String country) {
    var response = getAppResponse(appId, country);
    if (response == null) {
      log.error("response is null");
      return 0L;
    }
    return response.reviews();
  }

  public static long getAppRatings(String appId, String country) {
    var response = getAppResponse(appId, country);
    if (response == null) {
      log.error("response is null");
      return 0L;
    }
    return response.ratings();
  }
}
