package com.miti99.storescraperbot.api.apple;

import com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpHeaderNames;
import com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpHeaderValues;
import com.miti99.storescraperbot.api.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.api.apple.response.AppleAppResponse;
import com.miti99.storescraperbot.util.JacksonUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import lombok.SneakyThrows;

public class AppStoreScraper {
  public static final String BASE_URL = "https://miti-app-store-scraper.vercel.app/";

  @SneakyThrows
  public static AppleAppResponse app(AppleAppRequest request) {
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/app"))
            // .timeout(Duration.ofMillis(TIMEOUT))
            .header(
                HttpHeaderNames.CONTENT_TYPE.toString(),
                HttpHeaderValues.APPLICATION_JSON.toString())
            .POST(BodyPublishers.ofString(JacksonUtil.writeValueAsString(request)))
            .build();

    var body =
        HttpClient.newBuilder()
            .followRedirects(Redirect.NORMAL)
            // .connectTimeout(Duration.ofMillis(TIMEOUT))
            .build()
            .send(httpRequest, BodyHandlers.ofString())
            .body();
    return JacksonUtil.readValue(body, AppleAppResponse.class);
  }
}
