package org.example.services;

public class DatabaseContext {
  private static String currentDatabase;

  public static String getCurrentDatabase() {
    return currentDatabase;
  }

  public static void setCurrentDatabase(String database) {
    currentDatabase = database;
  }
}
