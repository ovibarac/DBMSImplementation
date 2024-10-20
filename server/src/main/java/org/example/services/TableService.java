package org.example.services;

import org.example.model.Column;
import org.example.model.ForeignKey;
import org.example.repo.TableRepository;

import java.util.List;
import java.util.Set;

public class TableService {

  private TableRepository tableRepository = new TableRepository();

  public String createTable(String tableName, List<Column> columns, Set<String> primaryKeys, List<ForeignKey> foreignKeys) {
    String response;
    try{
      String currentDatabase = DatabaseContext.getCurrentDatabase();
      if (currentDatabase == null) {
        return "No database selected. Use 'USE DB' to select a database.";
      }
      tableRepository.createTable(currentDatabase, tableName, columns, primaryKeys, foreignKeys);
      response = "Table '" + tableName + "' created.";
    } catch(Exception e) {
      response = e.getMessage();
    }

    return response;
  }

  public String dropTable(String tableName) {
    String response;
    try{
      String currentDatabase = DatabaseContext.getCurrentDatabase();
      tableRepository.dropTable(currentDatabase, tableName);
      response="Table '" + tableName + "' dropped successfully.";
    }catch(Exception e){
      response=e.getMessage();
    }

    return response;
  }
}
