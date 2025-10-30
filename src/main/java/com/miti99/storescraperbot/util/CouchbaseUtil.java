package com.miti99.storescraperbot.util;

import static com.miti99.storescraperbot.config.Config.COUCHBASE_BUCKET_NAME;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_CONNECTION_STRING;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_PASSWORD;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_USERNAME;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import java.time.Duration;

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
}
