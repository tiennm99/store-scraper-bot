package com.miti99.storescraperbot.util;

import static com.miti99.storescraperbot.env.Environment.MONGODB_DATABASE_NAME;
import static com.miti99.storescraperbot.env.Environment.MONGODB_CONNECTION_STRING;
import static com.miti99.storescraperbot.env.Environment.MONGODB_USERNAME;
import static com.miti99.storescraperbot.env.Environment.MONGODB_PASSWORD;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.CreateIndexOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoDBUtil {
  public static final MongoClient MONGO_CLIENT;
  public static final MongoDatabase DATABASE;

  static {
    String connectionString = MONGODB_CONNECTION_STRING;
    String username = MONGODB_USERNAME;
    String password = MONGODB_PASSWORD;

    String mongoUri;
    if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
      mongoUri = String.format("mongodb://%s:%s@%s", username, password, connectionString);
    } else {
      mongoUri = connectionString;
    }

    MONGO_CLIENT = MongoClients.create(mongoUri);
    DATABASE = MONGO_CLIENT.getDatabase(MONGODB_DATABASE_NAME);
    log.info("MongoDB connection established to database: {}", MONGODB_DATABASE_NAME);
  }

  public static void createCollectionIfNotExists(String collectionName) {
    try {
      boolean collectionExists = false;
      for (String name : DATABASE.listCollectionNames()) {
        if (name.equals(collectionName)) {
          collectionExists = true;
          break;
        }
      }

      if (!collectionExists) {
        DATABASE.createCollection(collectionName);
        log.info("Collection created: {}", collectionName);
      } else {
        log.info("Collection existed: {}", collectionName);
      }
    } catch (Exception e) {
      log.error("createCollectionIfNotExists error - collectionName: '{}'", collectionName, e);
    }
  }

  public static void createTTLIndexIfNotExists(String collectionName, String fieldName, long expireAfterSeconds) {
    try {
      MongoCollection<?> collection = DATABASE.getCollection(collectionName);

      // Check if TTL index already exists
      boolean indexExists = false;
      for (var index : collection.listIndexes()) {
        String indexOptions = index.toJson();
        if (indexOptions.contains("\"expireAfterSeconds\": " + expireAfterSeconds)) {
          indexExists = true;
          break;
        }
      }

      if (!indexExists) {
        CreateIndexOptions options = new CreateIndexOptions().expireAfter(expireAfterSeconds, java.util.concurrent.TimeUnit.SECONDS);
        collection.createIndex(Indexes.descending(fieldName), options);
        log.info("TTL index created on {} in collection {} with expire time: {} seconds",
                fieldName, collectionName, expireAfterSeconds);
      } else {
        log.info("TTL index already existed on {} in collection {}", fieldName, collectionName);
      }
    } catch (Exception e) {
      log.error("createTTLIndexIfNotExists error - collectionName: '{}', fieldName: '{}'",
               collectionName, fieldName, e);
    }
  }
}