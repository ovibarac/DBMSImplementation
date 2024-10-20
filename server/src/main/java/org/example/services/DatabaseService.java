package org.example.services;

import org.example.repo.DatabaseRepository;

public class DatabaseService {

  private DatabaseRepository databaseRepository = new DatabaseRepository();

  public String createDatabase(String databaseName) {
    String response;
    try {
      databaseRepository.createDatabase(databaseName);
      response="Database '" + databaseName + "' created successfully.";
    } catch (Exception e){
      response=e.getMessage();
    }

    return response;
  }

  public String useDatabase(String dbName) {
    DatabaseContext.setCurrentDatabase(dbName);
    String response = "Using database: " + dbName;

    return response;
  }

  public String dropDatabase(String databaseName) {
    String response;
    try {
      databaseRepository.dropDatabase(databaseName);
      response="Database '" + databaseName + "' dropped successfully.";
    } catch (Exception e){
      response=e.getMessage();
    }

    return response;
  }
}
