package com.miti99.storescraperbot.util;

import static com.miti99.storescraperbot.env.Environment.MONGODB_CONNECTION_STRING;

import com.miti99.storescraperbot.constant.Constant;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoDBUtil {
  public static final MongoClient MONGO_CLIENT;
  public static final MongoDatabase DATABASE;

  static {
    var serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();
    var connectionString = new ConnectionString(MONGODB_CONNECTION_STRING);
    var settings =
        MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .serverApi(serverApi)
            .build();
    MONGO_CLIENT = MongoClients.create(settings);
    var databaseName =
        connectionString.getDatabase() != null
            ? connectionString.getDatabase()
            : Constant.DEFAULT_DATABASE_NAME;
    DATABASE = MONGO_CLIENT.getDatabase(databaseName);
    try {
      DATABASE.runCommand(new Document("ping", 1));
      log.info("Pinged your deployment. You successfully connected to MongoDB!");
    } catch (MongoException e) {
      log.error(e);
    }
  }

  public static void createCollectionIfNotExists(String collectionName) {
    try {
      boolean collectionExists = false;
      for (var name : DATABASE.listCollectionNames()) {
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
}
