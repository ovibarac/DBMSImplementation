package org.example;

import org.example.model.Column;
import org.example.model.ForeignKey;
import org.example.services.DatabaseService;
import org.example.services.TableService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLCommandParser {

  public String parseAndExecute(String sqlCommand, DatabaseService dbService, TableService tblService) {
    sqlCommand = sqlCommand.trim().replaceAll("\\s+", " ");

    if (sqlCommand.matches("(?i)CREATE DATABASE .*")) {
      return handleCreateDatabase(sqlCommand, dbService);
    }

    if (sqlCommand.matches("(?i)USE .*")) {
      return handleUseDatabase(sqlCommand, dbService);
    }

    if (sqlCommand.matches("(?i)CREATE TABLE .*")) {
      return handleCreateTable(sqlCommand, tblService);
    }

    if (sqlCommand.matches("(?i)DROP DATABASE .*")) {
      return handleDropDatabase(sqlCommand, dbService);
    }

    if (sqlCommand.matches("(?i)DROP TABLE .*")) {
      return handleDropTable(sqlCommand, tblService);
    }

    return "Unknown or unsupported SQL command.";
  }

  private String handleCreateDatabase(String sqlCommand, DatabaseService dbService) {
    Pattern pattern = Pattern.compile("(?i)CREATE DATABASE (\\w+);?");
    Matcher matcher = pattern.matcher(sqlCommand);

    if (matcher.find()) {
      String databaseName = matcher.group(1);
      String response = dbService.createDatabase(databaseName);
      System.out.println(response);

      return response;
    }

    return "Invalid CREATE DATABASE command.";
  }

  private String handleUseDatabase(String sql, DatabaseService dbService) {
    Pattern useDbPattern = Pattern.compile("(?i)USE\\s+(\\w+);?");
    Matcher matcher = useDbPattern.matcher(sql);
    if (matcher.find()) {
      String databaseName = matcher.group(1);
      String response = dbService.useDatabase(databaseName);
      System.out.println(response);

      return response;
    }

    return "Invalid USE command.";
  }

  private String handleCreateTable(String sqlCommand, TableService tblService) {
    Pattern pattern = Pattern.compile("(?i)CREATE TABLE (\\w+) \\((.+)\\);?");
    Matcher matcher = pattern.matcher(sqlCommand);

    if (matcher.find()) {
      String tableName = matcher.group(1);
      String columnsString = matcher.group(2);

      List<Column> columns = new ArrayList<>();
      Set<String> primaryKeys = new HashSet<>();
      List<ForeignKey> foreignKeys = new ArrayList<>();

      parseColumnsAndConstraints(columnsString, columns, primaryKeys, foreignKeys);

      if (columns.isEmpty()) {
        return "Invalid column definition.";
      }

      String response = tblService.createTable(tableName, columns, primaryKeys, foreignKeys);
      System.out.println(response);

      return response;
    }

    return "Invalid CREATE TABLE command.";
  }

  private void parseColumnsAndConstraints(String columnsString, List<Column> columns,
                                          Set<String> primaryKeys, List<ForeignKey> foreignKeys) {
    String[] columnDefs = columnsString.split(",");

    for (String columnDef : columnDefs) {
      String trimmedDef = columnDef.trim();

      if (trimmedDef.matches("(?i)PRIMARY KEY \\((.+)\\)")) {
        Matcher pkMatcher = Pattern.compile("(?i)PRIMARY KEY \\((.+)\\)").matcher(trimmedDef);
        if (pkMatcher.find()) {
          String[] pkColumns = pkMatcher.group(1).split(",");
          for (String pkColumn : pkColumns) {
            primaryKeys.add(pkColumn.trim());
          }
        }
      } else if (trimmedDef.matches("(?i)FOREIGN KEY \\((.+)\\) REFERENCES (\\w+)\\((.+)\\)")) {
        Matcher fkMatcher = Pattern.compile("(?i)FOREIGN KEY \\((.+)\\) REFERENCES (\\w+)\\((.+)\\)").matcher(trimmedDef);
        if (fkMatcher.find()) {
          String fkColumn = fkMatcher.group(1).trim();
          String referencedTable = fkMatcher.group(2).trim();
          String referencedColumn = fkMatcher.group(3).trim();
          foreignKeys.add(new ForeignKey(fkColumn, referencedTable, referencedColumn));
        }
      } else {
        String[] parts = trimmedDef.split("\\s+");
        if (parts.length >= 2) {
          String columnName = parts[0];
          String columnType = parts[1];
          columns.add(new Column(columnName, columnType));
        }
      }
    }
  }

  private String handleDropDatabase(String sqlCommand, DatabaseService dbService) {
    Pattern pattern = Pattern.compile("(?i)DROP DATABASE (\\w+);?");
    Matcher matcher = pattern.matcher(sqlCommand);

    if (matcher.find()) {
      String databaseName = matcher.group(1);
      String response = dbService.dropDatabase(databaseName);
      System.out.println(response);

      return response;
    }

    return "Invalid DROP DATABASE command.";
  }

  private String handleDropTable(String sqlCommand, TableService tblService) {
    Pattern pattern = Pattern.compile("(?i)DROP TABLE (\\w+);?");
    Matcher matcher = pattern.matcher(sqlCommand);

    if (matcher.find()) {
      String tableName = matcher.group(1);
      String response = tblService.dropTable(tableName);
      System.out.println(response);

      return response;
    }

    return "Invalid DROP TABLE command.";
  }
}
