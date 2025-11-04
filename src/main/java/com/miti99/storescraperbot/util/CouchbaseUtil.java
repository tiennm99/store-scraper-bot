package com.miti99.storescraperbot.util;

import static com.miti99.storescraperbot.config.Config.COUCHBASE_BUCKET_NAME;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_CONNECTION_STRING;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_PASSWORD;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_USERNAME;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.miti99.storescraperbot.config.Config;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CouchbaseUtil {
  public static final Cluster CLUSTER;
  public static final Bucket BUCKET;

  static {
    CLUSTER =
        Cluster.connect(
            COUCHBASE_CONNECTION_STRING,
            ClusterOptions.clusterOptions(COUCHBASE_USERNAME, COUCHBASE_PASSWORD)
                .environment(env -> {}));

    BUCKET = CLUSTER.bucket(COUCHBASE_BUCKET_NAME);
    BUCKET.waitUntilReady(Duration.ofSeconds(10));
  }

  public static void createScope(String scopeName) {
    var collectionManager = BUCKET.collections();
    try {
      boolean scopeExists =
          collectionManager.getAllScopes().stream().anyMatch(s -> s.name().equals(scopeName));

      if (!scopeExists) {
        collectionManager.createScope(scopeName);
        log.info("Scope created: {}", scopeName);
      } else {
        log.info("Scope existed: {}", scopeName);
      }
    } catch (Exception e) {
      log.error("createScope error - scopeName: '{}'", scopeName, e);
    }
  }

  public static void createCollection(String scopeName, String collectionName) {
    var collectionManager = BUCKET.collections();
    try {
      var scopeSpecOpt =
          collectionManager.getAllScopes().stream()
              .filter(s -> s.name().equals(scopeName))
              .findFirst();
      if (scopeSpecOpt.isEmpty()) {
        createScope(scopeName);
        createCollection(scopeName, collectionName);
        return;
      }

      var scopeSpec = scopeSpecOpt.get();
      boolean collectionExists =
          scopeSpec.collections().stream().anyMatch(c -> c.name().equals(collectionName));

      if (!collectionExists) {
        var spec = CollectionSpec.create(collectionName, scopeName);
        collectionManager.createCollection(spec);
        log.info("Collection created: {} in {}", collectionName, scopeName);
      } else {
        log.info("Collection existed: {} in {}", collectionName, scopeName);
      }
    } catch (Exception e) {
      log.error(
          "createCollection error - collectionName: '{}', scopeName: '{}'",
          collectionName,
          scopeName,
          e);
    }
  }
}
