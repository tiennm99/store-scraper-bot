package com.miti99.storescraperbot.api.apple;

import com.miti99.storescraperbot.api.apple.entity.AppleAppDetail;
import com.miti99.storescraperbot.api.apple.request.ITunesLookupRequest;
import com.miti99.storescraperbot.api.apple.response.ITunesLookupResponse;
import com.miti99.storescraperbot.repository.AppleAppRepository;
import com.miti99.storescraperbot.util.GsonUtil;
import com.miti99.storescraperbot.util.RequestUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
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
  private static final String LOOKUP_URL = "https://itunes.apple.com/lookup?";

  @SneakyThrows
  public static String rawLookup(ITunesLookupRequest request) {
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(LOOKUP_URL + RequestUtil.makeGetParams(request)))
            // .timeout(Duration.ofMillis(TIMEOUT))
            .header("Content-Type", "application/json")
            .GET()
            .build();

    try (var httpClient =
        HttpClient.newBuilder()
            .followRedirects(Redirect.NORMAL)
            // .connectTimeout(Duration.ofMillis(TIMEOUT))
            .build()) {
      return httpClient.send(httpRequest, BodyHandlers.ofString()).body();
    } catch (Exception e) {
      log.error("rawLookup error - request: '{}'", GsonUtil.toJson(request), e);
      return null;
    }
  }

  @SneakyThrows
  public static AppleAppDetail app(ITunesLookupRequest request) {
    return GsonUtil.fromJson(rawLookup(request), ITunesLookupResponse.class).getAppDetail();
  }

  public static AppleAppDetail app(String appId) {
    return app(new ITunesLookupRequest(appId));
  }

  public static AppleAppDetail app(long id) {
    return app(new ITunesLookupRequest(id));
  }

  public static AppleAppDetail getAppResponse(String appId) {
    boolean isInCache = AppleAppRepository.INSTANCE.exist(appId);
    if (isInCache) {
      var app = AppleAppRepository.INSTANCE.load(appId);
      return app.detail();
    } else {
      var response = app(appId);
      AppleAppRepository.INSTANCE.init(appId);
      var app = AppleAppRepository.INSTANCE.load(appId);
      app.detail(response);
      AppleAppRepository.INSTANCE.save(appId, app);
      return response;
    }
  }

  public static LocalDate getAppUpdated(String appId) {
    var detail = getAppResponse(appId);
    if (detail == null) {
      log.error("detail is null");
      return LocalDate.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
    }
    return LocalDate.ofInstant(
        Instant.parse(detail.currentVersionReleaseDate()), ZoneId.systemDefault());
  }

  public static double getAppScore(String appId) {
    var response = getAppResponse(appId);
    if (response == null) {
      log.error("response is null");
      return 0.0;
    }
    return response.averageUserRating();
  }
}
