package org.example.services;

import com.mongodb.client.MongoDatabase;
import org.example.repo.IndexRepository;

import java.util.List;

public class IndexService {
  private IndexRepository indexRepository = new IndexRepository();

  public String createIndex(String indexName, String tableName, List<String> columnNames, boolean isUnique) throws Exception {
    String response;
    try{
      String currentDatabase = DatabaseContext.getCurrentDatabase();
      if (currentDatabase == null) {
        return "No database selected. Use 'USE DB' to select a database.";
      }
      MongoDatabase db = DatabaseContext.getDBConnection();
      indexRepository.createIndex(indexName, currentDatabase, tableName, columnNames, isUnique, db);
      response = "Index '" + indexName + "' created on columns " + columnNames + " isUnique="+ isUnique + ".";
    } catch(Exception e) {
      response = e.getMessage();
    }

    return response;
  }
}
