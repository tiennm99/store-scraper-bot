import { MongoClient } from 'mongodb';

let client;
let database;

export async function initMongoDB(config) {
  client = new MongoClient(config.mongoUri, {
    serverSelectionTimeoutMS: config.mongoTimeoutMs,
  });
  await client.connect();
  await client.db(config.mongoDatabase).command({ ping: 1 });
  database = client.db(config.mongoDatabase);
  config.logger.info(
    { database: config.mongoDatabase, uri: config.mongoUri },
    'Connected to MongoDB',
  );
}

export async function closeMongoDB() {
  if (client) await client.close();
}

export function getDatabase() {
  if (!database) throw new Error('MongoDB not initialized');
  return database;
}

export function getCollection(name) {
  return getDatabase().collection(name);
}
