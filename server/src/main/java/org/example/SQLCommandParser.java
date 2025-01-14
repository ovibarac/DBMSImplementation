package org.example;

import org.example.model.Column;
import org.example.model.Condition;
import org.example.model.ForeignKey;
import org.example.model.JoinCondition;
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

    if (sqlCommand.matches("1M")) {
      return handleInsertOneMillionProducts(sqlCommand, tblService);
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

  private String handleInsertOneMillionProducts(String sqlCommand, TableService tableService) {
      String tableName = "Orders";
      int i = 1;
      while(i <= 40655) {
        try {
          int id1 = randomPrice();
          int id2 = randomId();
          int quantity = (int) (Math.random() * 20) +1;
          //String passwd = randomString(10);
          //String color = randomColor();
          //int price = randomPrice();
          List<String> columns = Arrays.asList(
                  String.valueOf(i),
                  Integer.toString(id1),
                  Integer.toString(id2),
                  Integer.toString(quantity)
          );
          String response = tableService.insertRegisterService(tableName, columns);
          i+=1;
        } catch (Exception e){
          String response = e.getMessage();
        }
      }
      return "Successfully inserted 1,000,000 products.";
  }

  private String randomString(int length) {
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    StringBuilder randomString = new StringBuilder();
    for (int i = 0; i < length; i++) {
      int index = (int) (characters.length() * Math.random());
      randomString.append(characters.charAt(index));
    }
    return randomString.toString();
  }

  private String randomColor() {
    String[] colors = {"Red", "Green", "Blue", "Yellow", "Black", "White", "Pink", "Purple", "Orange", "Brown"};
    return colors[(int) (Math.random() * colors.length)];
  }

  private int randomPrice() {
    return (int) (Math.random() * 100000) + 1;
  }

  private int randomId() {
    return (int) (Math.random() * 1000000) +1;
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
      Pattern pattern = Pattern.compile("(?i)SELECT (DISTINCT )?(.+) FROM (.+) WHERE (.+) JOIN (.+) ON (.+);?");
      Matcher matcher = pattern.matcher(sqlCommand);
      String joinCondition = "";
      String tableJoin = "";

      if(!matcher.find()) {
        pattern = Pattern.compile("(?i)SELECT (DISTINCT )?(.+) FROM (.+) WHERE (.+);?");
        matcher = pattern.matcher(sqlCommand);
        if(!matcher.find())
          return "Invalid SELECT sintax";
      }
      else{
        tableJoin = matcher.group(5);
        joinCondition = matcher.group(6);
      }

      String columns = matcher.group(2);
      String tables = matcher.group(3);
      String conditions = matcher.group(4);

      List<String> tableList = parseTables(tables);
      List<JoinCondition> jcondList = new ArrayList<>();
      JoinCondition jcond;
      if(!joinCondition.isEmpty()) {
        jcond = parseJoin(tableList, tableJoin, joinCondition);
        jcondList.add(jcond);
      }

      List<String> columnList = parseColumns(columns,tableList);
      List<Condition> conditionList = parseConditions(conditions,tableList,jcondList);

      String response = tableService.selectService(tableList,columnList,conditionList,distinct,jcondList);
      System.out.println(String.join("\n", Arrays.stream(response.split("\\|")).toList()));


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

  private List<Condition> parseConditions (String conditions, List<String> tables, List<JoinCondition> jcondList) throws Exception {
    List<Condition> rez = new ArrayList<>();
    String[] cond = conditions.split("AND");
    for(int i=0 ; i<cond.length ; i++){
      int joinCondition = 0;
      if(cond[i].charAt(0)==' ')
        cond[i] = cond[i].substring(1);
      Condition c = new Condition(cond[i]);
      if(tables.size()>1)
      {
        String column = c.getColumn();
        int index1 = tables.indexOf(column.split("\\.")[0]);
        if(index1==-1)
          throw new Exception("Invalid SELECT sintax (column error in a condition!)");
        String column1 = c.getColumn().split("\\.")[1];
        String column2 = "";
        if(c.getValue().split("\\.").length == 2)
          column2 = c.getValue().split("\\.")[1];
        if(column1.equals(column2))
        {
          if(!c.getSign().equals("="))
            throw new Exception("Invalid SELECT sintax (invalid sign in the join condition!)");
          joinCondition = 1;
          String value = c.getValue();
          int index2 = tables.indexOf(value.split("\\.")[1]);
          if(index2==-1)
            throw new Exception("Invalid SELECT sintax (column error in the join condition!)");
          jcondList.add(new JoinCondition(tables.get(index1-1),tables.get(index2-1),column1));
        }
      }
      List<String> signs = List.of("=","LIKE",">","<","<=",">=");
      String sign = c.getSign();
      if(!signs.contains(sign))
        throw new Exception("Invalid SELECT sintax (invalid sign in a condition!)");
      if(joinCondition==0)
        rez.add(c);
    }
    return rez;
  }

  private JoinCondition parseJoin (List<String> tables, String tableJoin, String joinCondition) throws Exception{
    if(tables.size()!=2)
      throw new Exception("Invalid SELECT sintax (when using JOIN command, only one table is allowed on FROM selection and it needs a variable!)");
    if(tableJoin.charAt(0)==' ')
      tableJoin = tableJoin.substring(1);
    if(joinCondition.charAt(0)==' ')
      joinCondition = joinCondition.substring(1);
    String[] table_params = tableJoin.split(" ");
    String[] params = joinCondition.split(" ");
    if(params.length != 3)
      throw new Exception("Invalid join condition: "+joinCondition);
    if(table_params.length != 2)
      throw new Exception("Invalid table join: "+tableJoin);
    if(!params[1].equals("="))
      throw new Exception("Invalid SELECT sintax (invalid sign in the join condition!)");
    if(!params[0].split("\\.")[0].equals(tables.get(1)))
      throw new Exception("Invalid SELECT sintax (table mismatch on join condition!)");
    if(!params[2].split("\\.")[0].equals(table_params[1]))
      throw new Exception("Invalid SELECT sintax (table mismatch on join condition!)");
    String column1 = params[0].split("\\.")[1];
    String column2 = params[2].split("\\.")[1];
    if(!column1.equals(column2))
      throw new Exception("Invalid SELECT sintax (invalid column equality on join condition!)");
    tables.add(table_params[0]);
    tables.add(table_params[1]);
    return new JoinCondition(tables.get(0),table_params[0],column1);
  }
}
