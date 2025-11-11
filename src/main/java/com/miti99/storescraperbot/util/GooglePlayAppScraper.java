package com.miti99.storescraperbot.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;

public class GooglePlayAppScraper {

  private static final String BASE_URL = "https://play.google.com";
  private static final String PLAYSTORE_URL = BASE_URL + "/store/apps/details";

  /**
   * Main method to fetch app details by appId
   * @param appId The application ID (package name) to fetch details for
   * @return JsonObject containing app details
   * @throws Exception if there's an error during the request or parsing
   */
  public static JsonObject getAppDetails(String appId) throws Exception {
    return getAppDetails(appId, "en", "us");
  }

  /**
   * Fetch app details by appId with language and country parameters
   * @param appId The application ID (package name) to fetch details for
   * @param lang Language code (e.g., "en")
   * @param country Country code (e.g., "us")
   * @return JsonObject containing app details
   * @throws Exception if there's an error during the request or parsing
   */
  public static JsonObject getAppDetails(String appId, String lang, String country)
      throws Exception {
    if (appId == null || appId.isEmpty()) {
      throw new IllegalArgumentException("appId missing");
    }

    // Set default values for lang and country if not provided
    if (lang == null || lang.isEmpty()) lang = "en";
    if (country == null || country.isEmpty()) country = "us";

    // Construct URL with query parameters
    // String url = String.format("%s?id=%s&hl=%s&gl=%s", PLAYSTORE_URL, appId, lang, country);
    String url = String.format("%s?id=%s", PLAYSTORE_URL, appId);

    // Make HTTP request
    String response = makeRequest(url);

    // Parse the response and extract data
    JsonObject result = parseResponse(response);

    // Add appId and URL to the result
    result.addProperty("appId", appId);
    result.addProperty("url", url);

    return result;
  }

  /**
   * Make HTTP request to the Google Play Store
   * @param url The URL to request
   * @return The response as a String
   * @throws Exception if there's an error during the request
   */
  private static String makeRequest(String url) throws Exception {
    URL obj = new URL(url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty(
        "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

    int responseCode = con.getResponseCode();
    if (responseCode != 200) {
      throw new RuntimeException("HTTP request failed with code: " + responseCode);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuilder response = new StringBuilder();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();

    return response.toString();
  }

  /**
   * Parse the Google Play Store response and extract app data
   * @param response The HTML response from Google Play Store
   * @return JsonObject with extracted app data
   */
  private static JsonObject parseResponse(String response) {
    JsonObject result = new JsonObject();

    try {
      // Extract script data using regex (similar to scriptData.js)
      Map<String, Object> parsedData = parseScriptData(response);

      // Extract fields using mappings (similar to MAPPINGS in app.js)
      extractFields(result, parsedData);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * Parse JavaScript objects from AF_initDataCallback functions
   * @param response The HTML response
   * @return Map of parsed data
   */
  private static Map<String, Object> parseScriptData(String response) {
    Map<String, Object> parsedData = new HashMap<>();

    // Regex patterns to match script data
    Pattern scriptPattern =
        Pattern.compile(">AF_initDataCallback[\\s\\S]*?</script", Pattern.CASE_INSENSITIVE);
    Pattern keyPattern = Pattern.compile("(ds:.*?)'");
    Pattern valuePattern = Pattern.compile("data:([\\s\\S]*?), sideChannel: \\{}}\\);<");

    Matcher scriptMatcher = scriptPattern.matcher(response);

    while (scriptMatcher.find()) {
      String data = scriptMatcher.group();
      Matcher keyMatcher = keyPattern.matcher(data);
      Matcher valueMatcher = valuePattern.matcher(data);

      if (keyMatcher.find() && valueMatcher.find()) {
        String key = keyMatcher.group(1);
        String valueStr = valueMatcher.group(1);

        try {
          // Try to parse as JSON
          Object value = parseJson(valueStr);
          parsedData.put(key, value);
        } catch (Exception e) {
          // If parsing fails, store as string
          parsedData.put(key, valueStr);
        }
      }
    }

    return parsedData;
  }

  /**
   * Simple JSON parser for the extracted data
   * @param jsonStr The JSON string to parse
   * @return Parsed object
   */
  private static Object parseJson(String jsonStr) {
    JsonElement element = JsonParser.parseString(jsonStr);
    return element;
  }

  /**
   * Extract fields from parsed data using mappings
   * @param result The result JsonObject to populate
   * @param parsedData The parsed data map
   */
  private static void extractFields(JsonObject result, Map<String, Object> parsedData) {
    // Extract all fields (full versions of the MAPPINGS from app.js)
    try {
      // Get the main data array
      JsonElement ds5 = (JsonElement) parsedData.get("ds:5");

      // Title - ['ds:5', 1, 2, 0, 0]
      Object titleObj = getPath(ds5, 1, 2, 0, 0);
      if (titleObj instanceof JsonElement && ((JsonElement) titleObj).isJsonPrimitive()) {
        result.addProperty("title", ((JsonElement) titleObj).getAsString());
      }

      // Description - ['ds:5', 1, 2] with helper function
      Object descriptionObj = getPath(ds5, 1, 2);
      if (descriptionObj instanceof JsonElement) {
        String description = extractDescriptionText((JsonElement) descriptionObj);
        result.addProperty("description", description);
      }

      // Description HTML - ['ds:5', 1, 2] with helper function
      Object descriptionHtmlObj = getPath(ds5, 1, 2);
      if (descriptionHtmlObj instanceof JsonElement) {
        String descriptionHtml = extractDescriptionHtmlLocalized((JsonElement) descriptionHtmlObj);
        result.addProperty("descriptionHTML", descriptionHtml);
      }

      // Summary - ['ds:5', 1, 2, 73, 0, 1]
      Object summaryObj = getPath(ds5, 1, 2, 73, 0, 1);
      if (summaryObj instanceof JsonElement && ((JsonElement) summaryObj).isJsonPrimitive()) {
        result.addProperty("summary", ((JsonElement) summaryObj).getAsString());
      }

      // Installs - ['ds:5', 1, 2, 13, 0]
      Object installsObj = getPath(ds5, 1, 2, 13, 0);
      if (installsObj instanceof JsonElement && ((JsonElement) installsObj).isJsonPrimitive()) {
        result.addProperty("installs", ((JsonElement) installsObj).getAsString());
      }

      // Min Installs - ['ds:5', 1, 2, 13, 1]
      Object minInstallsObj = getPath(ds5, 1, 2, 13, 1);
      if (minInstallsObj instanceof JsonElement
          && ((JsonElement) minInstallsObj).isJsonPrimitive()) {
        result.addProperty("minInstalls", ((JsonElement) minInstallsObj).getAsLong());
      }

      // Max Installs - ['ds:5', 1, 2, 13, 2]
      Object maxInstallsObj = getPath(ds5, 1, 2, 13, 2);
      if (maxInstallsObj instanceof JsonElement
          && ((JsonElement) maxInstallsObj).isJsonPrimitive()) {
        result.addProperty("maxInstalls", ((JsonElement) maxInstallsObj).getAsLong());
      }

      // Score - ['ds:5', 1, 2, 51, 0, 1]
      Object scoreObj = getPath(ds5, 1, 2, 51, 0, 1);
      if (scoreObj instanceof JsonElement && ((JsonElement) scoreObj).isJsonPrimitive()) {
        result.addProperty("score", ((JsonElement) scoreObj).getAsFloat());
      }

      // Score Text - ['ds:5', 1, 2, 51, 0, 0]
      Object scoreTextObj = getPath(ds5, 1, 2, 51, 0, 0);
      if (scoreTextObj instanceof JsonElement && ((JsonElement) scoreTextObj).isJsonPrimitive()) {
        result.addProperty("scoreText", ((JsonElement) scoreTextObj).getAsString());
      }

      // Ratings - ['ds:5', 1, 2, 51, 2, 1]
      Object ratingsObj = getPath(ds5, 1, 2, 51, 2, 1);
      if (ratingsObj instanceof JsonElement && ((JsonElement) ratingsObj).isJsonPrimitive()) {
        result.addProperty("ratings", ((JsonElement) ratingsObj).getAsLong());
      }

      // Reviews - ['ds:5', 1, 2, 51, 3, 1]
      Object reviewsObj = getPath(ds5, 1, 2, 51, 3, 1);
      if (reviewsObj instanceof JsonElement && ((JsonElement) reviewsObj).isJsonPrimitive()) {
        result.addProperty("reviews", ((JsonElement) reviewsObj).getAsLong());
      }

      // Histogram - ['ds:5', 1, 2, 51, 1] with helper function
      Object histogramObj = getPath(ds5, 1, 2, 51, 1);
      JsonObject histogram = buildHistogram(histogramObj);
      result.add("histogram", histogram);

      // Price - ['ds:5', 1, 2, 57, 0, 0, 0, 0, 1, 0, 0] with function
      Object priceObj = getPath(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 0, 0);
      double price = 0;
      if (priceObj instanceof JsonElement && ((JsonElement) priceObj).isJsonPrimitive()) {
        price = ((JsonElement) priceObj).getAsLong() / 1000000.0;
      }
      result.addProperty("price", price);

      // Original Price - ['ds:5', 1, 2, 57, 0, 0, 0, 0, 1, 1, 0] with function
      Object originalPriceObj = getPath(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 1, 0);
      if (originalPriceObj instanceof JsonElement
          && ((JsonElement) originalPriceObj).isJsonPrimitive()) {
        long originalPriceValue = ((JsonElement) originalPriceObj).getAsLong();
        if (originalPriceValue > 0) {
          result.addProperty("originalPrice", originalPriceValue / 1000000.0);
        }
      }

      // Discount End Date - ['ds:5', 1, 2, 57, 0, 0, 0, 0, 14, 1]
      Object discountEndDateObj = getPath(ds5, 1, 2, 57, 0, 0, 0, 0, 14, 1);
      if (discountEndDateObj instanceof JsonElement
          && ((JsonElement) discountEndDateObj).isJsonPrimitive()) {
        result.addProperty("discountEndDate", ((JsonElement) discountEndDateObj).getAsLong());
      }

      // Free - ['ds:5', 1, 2, 57, 0, 0, 0, 0, 1, 0, 0] with function
      boolean isFree = false;
      if (priceObj instanceof JsonElement && ((JsonElement) priceObj).isJsonPrimitive()) {
        isFree = ((JsonElement) priceObj).getAsLong() == 0;
      }
      result.addProperty("free", isFree);

      // Currency - ['ds:5', 1, 2, 57, 0, 0, 0, 0, 1, 0, 1]
      Object currencyObj = getPath(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 0, 1);
      if (currencyObj instanceof JsonElement && ((JsonElement) currencyObj).isJsonPrimitive()) {
        result.addProperty("currency", ((JsonElement) currencyObj).getAsString());
      }

      // Price Text - ['ds:5', 1, 2, 57, 0, 0, 0, 0, 1, 0, 2] with helper
      Object priceTextPathObj = getPath(ds5, 1, 2, 57, 0, 0, 0, 0, 1, 0, 2);
      String priceText = "Free";
      if (priceTextPathObj instanceof JsonElement
          && ((JsonElement) priceTextPathObj).isJsonPrimitive()) {
        priceText = ((JsonElement) priceTextPathObj).getAsString();
      }
      result.addProperty("priceText", priceText);

      // Available - ['ds:5', 1, 2, 18, 0] with Boolean function
      Object availableObj = getPath(ds5, 1, 2, 18, 0);
      boolean isAvailable = false;
      if (availableObj instanceof JsonElement && ((JsonElement) availableObj).isJsonPrimitive()) {
        isAvailable = ((JsonElement) availableObj).getAsInt() == 1;
      }
      result.addProperty("available", isAvailable);

      // Offers IAP - ['ds:5', 1, 2, 19, 0] with Boolean function
      Object offersIAPObj = getPath(ds5, 1, 2, 19, 0);
      boolean offersIAP = false;
      if (offersIAPObj instanceof JsonElement && ((JsonElement) offersIAPObj).isJsonPrimitive()) {
        offersIAP = ((JsonElement) offersIAPObj).getAsInt() == 1;
      }
      result.addProperty("offersIAP", offersIAP);

      // IAP Range - ['ds:5', 1, 2, 19, 0]
      Object iapRangeObj = getPath(ds5, 1, 2, 19, 0);
      if (iapRangeObj instanceof JsonElement && ((JsonElement) iapRangeObj).isJsonPrimitive()) {
        result.addProperty("IAPRange", ((JsonElement) iapRangeObj).getAsString());
      }

      // Android Version - ['ds:5', 1, 2, 140, 1, 1, 0, 0, 1] with helper
      Object androidVersionObj = getPath(ds5, 1, 2, 140, 1, 1, 0, 0, 1);
      String androidVersion = "VARY";
      if (androidVersionObj instanceof JsonElement
          && ((JsonElement) androidVersionObj).isJsonPrimitive()) {
        androidVersion = normalizeAndroidVersion(((JsonElement) androidVersionObj).getAsString());
      }
      result.addProperty("androidVersion", androidVersion);

      // Android Version Text - ['ds:5', 1, 2, 140, 1, 1, 0, 0, 1] with helper
      Object androidVersionTextObj = getPath(ds5, 1, 2, 140, 1, 1, 0, 0, 1);
      String androidVersionText = "Varies with device";
      if (androidVersionTextObj instanceof JsonElement
          && ((JsonElement) androidVersionTextObj).isJsonPrimitive()) {
        androidVersionText = ((JsonElement) androidVersionTextObj).getAsString();
      }
      result.addProperty("androidVersionText", androidVersionText);

      // Developer - ['ds:5', 1, 2, 68, 0]
      Object developerObj = getPath(ds5, 1, 2, 68, 0);
      if (developerObj instanceof JsonElement && ((JsonElement) developerObj).isJsonPrimitive()) {
        result.addProperty("developer", ((JsonElement) developerObj).getAsString());
      }

      // Developer Id - ['ds:5', 1, 2, 68, 1, 4, 2] with function
      Object developerIdObj = getPath(ds5, 1, 2, 68, 1, 4, 2);
      if (developerIdObj instanceof JsonElement
          && ((JsonElement) developerIdObj).isJsonPrimitive()) {
        String devUrl = ((JsonElement) developerIdObj).getAsString();
        String devId = extractDeveloperId(devUrl);
        if (devId != null) {
          result.addProperty("developerId", devId);
        }
      }

      // Developer Email - ['ds:5', 1, 2, 69, 1, 0]
      Object developerEmailObj = getPath(ds5, 1, 2, 69, 1, 0);
      if (developerEmailObj instanceof JsonElement
          && ((JsonElement) developerEmailObj).isJsonPrimitive()) {
        result.addProperty("developerEmail", ((JsonElement) developerEmailObj).getAsString());
      }

      // Developer Website - ['ds:5', 1, 2, 69, 0, 5, 2]
      Object developerWebsiteObj = getPath(ds5, 1, 2, 69, 0, 5, 2);
      if (developerWebsiteObj instanceof JsonElement
          && ((JsonElement) developerWebsiteObj).isJsonPrimitive()) {
        result.addProperty("developerWebsite", ((JsonElement) developerWebsiteObj).getAsString());
      }

      // Developer Address - ['ds:5', 1, 2, 69, 2, 0]
      Object developerAddressObj = getPath(ds5, 1, 2, 69, 2, 0);
      if (developerAddressObj instanceof JsonElement
          && ((JsonElement) developerAddressObj).isJsonPrimitive()) {
        result.addProperty("developerAddress", ((JsonElement) developerAddressObj).getAsString());
      }

      // Privacy Policy - ['ds:5', 1, 2, 99, 0, 5, 2]
      Object privacyPolicyObj = getPath(ds5, 1, 2, 99, 0, 5, 2);
      if (privacyPolicyObj instanceof JsonElement
          && ((JsonElement) privacyPolicyObj).isJsonPrimitive()) {
        result.addProperty("privacyPolicy", ((JsonElement) privacyPolicyObj).getAsString());
      }

      // Genre - ['ds:5', 1, 2, 79, 0, 0, 0]
      Object genreObj = getPath(ds5, 1, 2, 79, 0, 0, 0);
      if (genreObj instanceof JsonElement && ((JsonElement) genreObj).isJsonPrimitive()) {
        result.addProperty("genre", ((JsonElement) genreObj).getAsString());
      }

      // Genre Id - ['ds:5', 1, 2, 79, 0, 0, 2]
      Object genreIdObj = getPath(ds5, 1, 2, 79, 0, 0, 2);
      if (genreIdObj instanceof JsonElement && ((JsonElement) genreIdObj).isJsonPrimitive()) {
        result.addProperty("genreId", ((JsonElement) genreIdObj).getAsString());
      }

      // Icon - ['ds:5', 1, 2, 95, 0, 3, 2]
      Object iconObj = getPath(ds5, 1, 2, 95, 0, 3, 2);
      if (iconObj instanceof JsonElement && ((JsonElement) iconObj).isJsonPrimitive()) {
        result.addProperty("icon", ((JsonElement) iconObj).getAsString());
      }

      // Header Image - ['ds:5', 1, 2, 96, 0, 3, 2]
      Object headerImageObj = getPath(ds5, 1, 2, 96, 0, 3, 2);
      if (headerImageObj instanceof JsonElement
          && ((JsonElement) headerImageObj).isJsonPrimitive()) {
        result.addProperty("headerImage", ((JsonElement) headerImageObj).getAsString());
      }

      // Screenshots - ['ds:5', 1, 2, 78, 0] with function
      Object screenshotsObj = getPath(ds5, 1, 2, 78, 0);
      JsonArray screenshots = extractScreenshots(screenshotsObj);
      result.add("screenshots", screenshots);

      // Content Rating - ['ds:5', 1, 2, 9, 0]
      Object contentRatingObj = getPath(ds5, 1, 2, 9, 0);
      if (contentRatingObj instanceof JsonElement
          && ((JsonElement) contentRatingObj).isJsonPrimitive()) {
        result.addProperty("contentRating", ((JsonElement) contentRatingObj).getAsString());
      }

      // Ad Supported - ['ds:5', 1, 2, 48] with Boolean function
      Object adSupportedObj = getPath(ds5, 1, 2, 48);
      boolean adSupported = false;
      if (adSupportedObj instanceof JsonElement
          && ((JsonElement) adSupportedObj).isJsonPrimitive()) {
        adSupported = ((JsonElement) adSupportedObj).getAsInt() == 1;
      }
      result.addProperty("adSupported", adSupported);

      // Released - ['ds:5', 1, 2, 10, 0]
      Object releasedObj = getPath(ds5, 1, 2, 10, 0);
      if (releasedObj instanceof JsonElement && ((JsonElement) releasedObj).isJsonPrimitive()) {
        result.addProperty("released", ((JsonElement) releasedObj).getAsString());
      }

      // Updated - ['ds:5', 1, 2, 145, 0, 1, 0] with function
      Object updatedObj = getPath(ds5, 1, 2, 145, 0, 1, 0);
      if (updatedObj instanceof JsonElement && ((JsonElement) updatedObj).isJsonPrimitive()) {
        long timestamp = ((JsonElement) updatedObj).getAsLong();
        result.addProperty("updated", timestamp * 1000);
      }

      // Version - ['ds:5', 1, 2, 140, 0, 0, 0] with function
      Object versionObj = getPath(ds5, 1, 2, 140, 0, 0, 0);
      String version = "VARY";
      if (versionObj instanceof JsonElement && ((JsonElement) versionObj).isJsonPrimitive()) {
        version = ((JsonElement) versionObj).getAsString();
        if (version == null || version.isEmpty()) {
          version = "VARY";
        }
      }
      result.addProperty("version", version);

      // Recent Changes - ['ds:5', 1, 2, 144, 1, 1]
      Object recentChangesObj = getPath(ds5, 1, 2, 144, 1, 1);
      if (recentChangesObj instanceof JsonElement
          && ((JsonElement) recentChangesObj).isJsonPrimitive()) {
        result.addProperty("recentChanges", ((JsonElement) recentChangesObj).getAsString());
      }

    } catch (Exception e) {
      // In case of any error, we continue with what we have
      e.printStackTrace();
    }
  }

  /**
   * Extract description text from search array
   * @param searchArray The JSON element containing description data
   * @return The processed description text
   */
  private static String extractDescriptionText(JsonElement searchArray) {
    String descriptionHtml = extractDescriptionHtmlLocalized(searchArray);
    if (descriptionHtml == null) return "";

    // This is a simplified version of the descriptionText function
    // The original uses cheerio to convert HTML to text while preserving line breaks
    // We'll do a basic HTML tag removal with line break preservation
    return descriptionHtml.replaceAll("<br>", "\n").replaceAll("<[^>]+>", "").trim();
  }

  /**
   * Extract description HTML localized from search array
   * @param searchArray The JSON element containing description data
   * @return The HTML localized description
   */
  private static String extractDescriptionHtmlLocalized(JsonElement searchArray) {
    // This mimics descriptionHtmlLocalized from mappingHelpers.js
    // It looks for description translation or original description
    JsonElement descriptionTranslation = getPath(searchArray, 12, 0, 0, 1);
    JsonElement descriptionOriginal = getPath(searchArray, 72, 0, 1);

    if (descriptionTranslation != null && descriptionTranslation.isJsonPrimitive()) {
      return descriptionTranslation.getAsString();
    } else if (descriptionOriginal != null && descriptionOriginal.isJsonPrimitive()) {
      return descriptionOriginal.getAsString();
    } else {
      return "";
    }
  }

  /**
   * Build histogram from container
   * @param container The histogram data container
   * @return JsonObject with histogram data
   */
  private static JsonObject buildHistogram(Object container) {
    JsonObject histogram = new JsonObject();
    if (container == null) {
      histogram.addProperty("1", 0);
      histogram.addProperty("2", 0);
      histogram.addProperty("3", 0);
      histogram.addProperty("4", 0);
      histogram.addProperty("5", 0);
      return histogram;
    }

    JsonElement containerElement = (JsonElement) container;
    if (containerElement.isJsonArray()) {
      JsonArray arr = containerElement.getAsJsonArray();
      if (arr.size() > 5) {
        JsonObject hist = new JsonObject();
        JsonElement one = getPath(arr, 1, 1);
        JsonElement two = getPath(arr, 2, 1);
        JsonElement three = getPath(arr, 3, 1);
        JsonElement four = getPath(arr, 4, 1);
        JsonElement five = getPath(arr, 5, 1);

        hist.addProperty("1", one != null && one.isJsonPrimitive() ? one.getAsInt() : 0);
        hist.addProperty("2", two != null && two.isJsonPrimitive() ? two.getAsInt() : 0);
        hist.addProperty("3", three != null && three.isJsonPrimitive() ? three.getAsInt() : 0);
        hist.addProperty("4", four != null && four.isJsonPrimitive() ? four.getAsInt() : 0);
        hist.addProperty("5", five != null && five.isJsonPrimitive() ? five.getAsInt() : 0);
        return hist;
      }
    }

    // Default values if histogram data is not available
    histogram.addProperty("1", 0);
    histogram.addProperty("2", 0);
    histogram.addProperty("3", 0);
    histogram.addProperty("4", 0);
    histogram.addProperty("5", 0);
    return histogram;
  }

  /**
   * Normalize Android version text
   * @param androidVersionText The original Android version text
   * @return Normalized Android version
   */
  private static String normalizeAndroidVersion(String androidVersionText) {
    if (androidVersionText == null) return "VARY";

    String number = androidVersionText.split(" ")[0];
    try {
      double num = Double.parseDouble(number);
      return Double.toString(num);
    } catch (NumberFormatException e) {
      return "VARY";
    }
  }

  /**
   * Extract developer ID from developer URL
   * @param devUrl The developer URL
   * @return The extracted developer ID
   */
  private static String extractDeveloperId(String devUrl) {
    if (devUrl != null && devUrl.contains("id=")) {
      String[] parts = devUrl.split("id=");
      if (parts.length > 1) {
        return parts[1];
      }
    }
    return null;
  }

  /**
   * Extract screenshots from screenshots data
   * @param screenshotsObj The screenshots data object
   * @return JsonArray of screenshot URLs
   */
  private static JsonArray extractScreenshots(Object screenshotsObj) {
    JsonArray screenshots = new JsonArray();
    if (screenshotsObj == null) return screenshots;

    JsonElement screenshotsElement = (JsonElement) screenshotsObj;
    if (screenshotsElement.isJsonArray()) {
      JsonArray array = screenshotsElement.getAsJsonArray();
      for (int i = 0; i < array.size(); i++) {
        JsonElement screenshotItem = array.get(i);
        JsonElement url = getPath(screenshotItem, 3, 2);
        if (url != null && url.isJsonPrimitive()) {
          screenshots.add(url.getAsString());
        }
      }
    }
    return screenshots;
  }

  /**
   * Get value at specified path in nested JSON structure
   * @param element The root JSON element
   * @param path The path indices
   * @return The value at the specified path, or null if not found
   */
  private static JsonElement getPath(JsonElement element, int... path) {
    if (element == null) return null;

    JsonElement current = element;

    for (int index : path) {
      if (current == null) return null;

      if (current.isJsonArray()) {
        JsonArray array = current.getAsJsonArray();
        if (index < array.size()) {
          current = array.get(index);
        } else {
          return null;
        }
      } else if (current.isJsonObject()) {
        // For objects, we might need a different approach
        return null;
      } else {
        return null;
      }
    }

    return current;
  }

    /**
     * Main method for testing
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
      // if (args.length < 1) {
      //     System.out.println("Usage: java GooglePlayAppScraper <appId>");
      //     System.out.println("Example: java GooglePlayAppScraper com.android.chrome");
      //     return;
      // }

      // String appId = args[0];
      String appId = "vn.kvtm.js";
            System.out.println("Fetching details for app: " + appId);
            JsonObject appDetails = getAppDetails(appId);

            // Pretty print the JSON response
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(appDetails));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
