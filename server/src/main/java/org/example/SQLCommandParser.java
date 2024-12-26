package org.example;

import org.example.model.Column;
import org.example.model.Condition;
import org.example.model.ForeignKey;
import org.example.services.DatabaseService;
import org.example.services.IndexService;
import org.example.services.TableService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SQLCommandParser {

  public String parseAndExecute(String sqlCommand, DatabaseService dbService, TableService tblService, IndexService indexService) {
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

    if (sqlCommand.matches("(?i)CREATE (UNIQUE )?INDEX .*")) {
      return handleCreateIndex(sqlCommand, indexService);
    }

    if (sqlCommand.matches("(?i)INSERT INTO .*")) {
      return handleInsertTable(sqlCommand, tblService);
    }

    if (sqlCommand.matches("(?i)DELETE FROM .*")) {
      return handleDeleteFromTable(sqlCommand, tblService);
    }

    if (sqlCommand.matches("(?i)SELECT .*")) {
      return handleSelectionFromTable(sqlCommand, tblService);
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

  private String handleCreateIndex(String sqlCommand, IndexService indexService) {
    try {
      boolean isUnique = sqlCommand.matches("(?i)CREATE UNIQUE INDEX .*");

      Pattern pattern = Pattern.compile("(?i)CREATE (UNIQUE )?INDEX (\\w+) ON (\\w+) \\(([^)]+)\\)");
      Matcher matcher = pattern.matcher(sqlCommand);
      if (!matcher.find()) {
        return "Invalid CREATE INDEX syntax.";
      }

      String indexName = matcher.group(2);
      String tableName = matcher.group(3);
      String columnsPart = matcher.group(4);

      List<String> columnNames = Arrays.stream(columnsPart.split(","))
              .map(String::trim)
              .collect(Collectors.toList());

      String response =  indexService.createIndex(indexName, tableName, columnNames, isUnique);
      System.out.println(response);

      return response;
    } catch (Exception e) {
      return "Error creating index: " + e.getMessage();
    }
  }

  private String handleInsertTable(String sqlCommand, TableService tableService){
    try {
      Pattern pattern = Pattern.compile("(?i)INSERT INTO (\\w+) VALUES \\(([^)]+)\\);?");
      Matcher matcher = pattern.matcher(sqlCommand);
      if (!matcher.find()) {
        return "Invalid INSERT syntax";
      }

      String tableName = matcher.group(1);
      String values = matcher.group(2);

      List<String> columns = Arrays.stream(values.split(",")).map(String::trim).toList();

      String response = tableService.insertRegisterService(tableName, columns);

      System.out.println(response);
      return response;
    } catch (Exception e) {
      return "Error inserting: " + e.getMessage();
    }
  }

  private String handleDeleteFromTable(String sqlCommand, TableService tableService) {
    try {
      Pattern pattern = Pattern.compile("(?i)DELETE FROM (\\w+) WHERE id = (\\d+);?");
      Matcher matcher = pattern.matcher(sqlCommand);

      if (!matcher.find()) {
        return "Invalid DELETE syntax. Expected format: DELETE FROM <tableName> WHERE id = <id>;";
      }

      String tableName = matcher.group(1);
      String id = matcher.group(2);

      String response = tableService.deleteRecords(tableName, id);
      System.out.println(response);
      return response;
    } catch (Exception e) {
      return "Error deleting record: " + e.getMessage();
    }
  }

  private String handleSelectionFromTable(String sqlCommand, TableService tableService) {
    try {
      boolean distinct = sqlCommand.matches("(?i)SELECT DISTINCT .*");
      Pattern pattern = Pattern.compile("(?i)SELECT (DISTINCT )?(.+) FROM (.+) WHERE (.+)");
      Matcher matcher = pattern.matcher(sqlCommand);

      if(!matcher.find())
        return "Invalid SELECT sintax";

      String columns = matcher.group(2);
      String tables = matcher.group(3);
      String conditions = matcher.group(4);

      List<String> tableList = parseTables(tables);
      List<String> columnList = parseColumns(columns,tableList);
      List<Condition> conditionList = parseConditions(conditions,tableList);

      String response = tableService.selectService(tableList,columnList,conditionList,distinct);
      System.out.println(response);
      return response;
    } catch (Exception e) {
      return "Error selection: " + e.getMessage();
    }
  }

  private List<String> parseTables(String tables){
    List<String> rez = new ArrayList<>();
    String[] params = tables.split(",");
    for(int i=0 ; i<params.length ; i++) {
      String[] t = params[i].split(" ");
      //Daca sunt mai multe tabele, sunt de forma Attributes a, ca au nevoie de denumiri
      //Se va transpune in coloane daca e nevoie, a.numeColoana
      //numele tabelului, pozitiile pare, denumirea parametrului, pozitiile impare
      rez.addAll(Arrays.asList(t));
    }
    return rez;
  }

  private List<String> parseColumns (String columns, List<String> tables) throws Exception {
    List<String> rez = new ArrayList<>();
    String[] elems = columns.split(",");
    for(int i=0 ; i<elems.length ; i++){
      //daca size-ul este mai mare de 1, exista denumiri de variable ale tabelului, trebuie verificate daca sunt folosite de coloane
      if(tables.size()>1)
      {
        if(!tables.contains(elems[i].split("\\.")[0]))
          throw new Exception("Invalid SELECT sintax (error in column list)");
      }
      rez.add(elems[i]);
    }
    return rez;
  }

  private List<Condition> parseConditions (String conditions, List<String> tables) throws Exception {
    List<Condition> rez = new ArrayList<>();
    String[] cond = conditions.split("AND");
    for(int i=0 ; i<cond.length ; i++){
      if(cond[i].charAt(0)==' ')
        cond[i] = cond[i].substring(1);
      Condition c = new Condition(cond[i]);
      if(tables.size()>1)
      {
        String column = c.getColumn();
        if(!tables.contains(column.split("\\.")[0]))
          throw new Exception("Invalid SELECT sintax (column error in a condition!)");
      }
      List<String> signs = List.of("=","LIKE",">","<");
      String sign = c.getSign();
      if(!signs.contains(sign))
        throw new Exception("Invalid SELECT sintax (invalid sign in a condition!)");
      rez.add(c);
    }
    return rez;
  }
}
