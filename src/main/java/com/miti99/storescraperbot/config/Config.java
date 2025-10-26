package com.miti99.storescraperbot.config;

public class Config {
  public static final String COUCHBASE_CONNECTION_STRING =
      System.getenv("COUCHBASE_CONNECTION_STRING");
  public static final String COUCHBASE_USERNAME = System.getenv("COUCHBASE_USERNAME");
  public static final String COUCHBASE_PASSWORD = System.getenv("COUCHBASE_PASSWORD");
  public static final String COUCHBASE_BUCKET_NAME = System.getenv("COUCHBASE_BUCKET_NAME");
}
