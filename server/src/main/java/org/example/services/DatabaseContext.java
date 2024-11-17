package org.example.services;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

public class DatabaseContext {
  private static String currentDatabase;
  private static MongoClient conn;

  public static void setConnection(MongoClient conn){
    DatabaseContext.conn = conn;
  }

  public static String getCurrentDatabase() {
    return currentDatabase;
  }

  public static MongoDatabase getDBConnection(){
    return conn.getDatabase(currentDatabase);
  }

  public static void setCurrentDatabase(String database) {
    currentDatabase = database;
  }
}
