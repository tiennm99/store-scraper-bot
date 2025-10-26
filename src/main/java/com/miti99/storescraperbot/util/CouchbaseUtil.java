package com.miti99.storescraperbot.util;

import static com.miti99.storescraperbot.config.Config.COUCHBASE_BUCKET_NAME;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_CONNECTION_STRING;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_PASSWORD;
import static com.miti99.storescraperbot.config.Config.COUCHBASE_USERNAME;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.query.QueryResult;
import java.time.Duration;

public class CouchbaseUtil {
  public static final Cluster CLUSTER;
  public static final Bucket BUCKET;
  static {
    CLUSTER =
        Cluster.connect(
            COUCHBASE_CONNECTION_STRING,
            ClusterOptions.clusterOptions(COUCHBASE_USERNAME, COUCHBASE_PASSWORD)
                .environment(
                    env -> {
                      // Customize client settings by calling methods on the "env" variable.
                    }));

    // get a bucket reference
    BUCKET = CLUSTER.bucket(COUCHBASE_BUCKET_NAME);
    BUCKET.waitUntilReady(Duration.ofSeconds(10));

    // get a user-defined collection reference
    Scope scope = BUCKET.scope("tenant_agent_00");
    Collection collection = scope.collection("users");

    // Upsert Document
    MutationResult upsertResult =
        collection.upsert("my-document", JsonObject.create().put("name", "mike"));

    // Get Document
    GetResult getResult = collection.get("my-document");
    String name = getResult.contentAsObject().getString("name");
    System.out.println(name); // name == "mike"

    // Call the query() method on the scope object and store the result.
    Scope inventoryScope = BUCKET.scope("inventory");
    QueryResult result = inventoryScope.query("SELECT * FROM airline WHERE id = 10;");

    // Return the result rows with the rowsAsObject() method and print to the terminal.
    System.out.println(result.rowsAsObject());
  }
}
