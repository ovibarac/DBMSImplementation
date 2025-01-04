package org.example.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.example.model.Column;
import org.example.model.Condition;
import org.example.model.ForeignKey;
import org.example.repo.TableRepository;

import java.util.*;
import java.util.stream.Stream;

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
      MongoDatabase db = DatabaseContext.getDBConnection();
      db.createCollection(tableName);
      response = "Table '" + tableName + "' created.";
    } catch(Exception e) {
      response = e.getMessage();
    }

    return response;
  }

  public String dropTable(String tableName) {
    MongoDatabase db = DatabaseContext.getDBConnection();
    String response;
    try{
      String currentDatabase = DatabaseContext.getCurrentDatabase();

      //Gaseste tabelele cu foreign key la id-ul tabelului curent
      List<String> referencingTables = tableRepository.getReferencingTables(currentDatabase, tableName);
      if(!referencingTables.isEmpty()) {
        return "Drop rejected: Table " + tableName + " is referenced by table " + referencingTables.get(0) + ".";
      }

      tableRepository.dropTable(currentDatabase, tableName);
      MongoCollection<org.bson.Document> collection = db.getCollection(tableName);
      collection.drop();

      response="Table '" + tableName + "' dropped successfully.";
    }catch(Exception e){
      response=e.getMessage();
    }

    return response;
  }

  private void validateData(List<String> attributes, List<String> values) throws Exception {
    if(attributes.size()!=values.size())
      throw new Exception("incorrect number of attributes!");
    for(int i=0 ; i<attributes.size();i++){
      String type = attributes.get(i).split("#")[1];
      if(type.equals("INT")) {
        int value = Integer.parseInt(values.get(i));
      }
      if(type.equals("FLOAT")){
        float value = Float.parseFloat(values.get(i));
      }
      if(type.equals("DOUBLE")){
        double value = Double.parseDouble(values.get(i));
      }
      if(type.contains("varchar")){
        int j = 8;
        String reqSize="";
        while(type.charAt(j)>='0' && type.charAt(j)<='9'){
          reqSize+=type.charAt(j);
          j++;
        }
        if(values.get(i).length()>Integer.parseInt(reqSize))
          throw new Exception("varchar length condition not respected!");
      }
    }
  }

  public String insertRegisterService(String tableName, List<String> values){
    String response;
    try {
      String currentDatabase = DatabaseContext.getCurrentDatabase();
      List<String> attr = tableRepository.getTableStructure(currentDatabase,tableName);

      validateData(attr,values);
      MongoDatabase db = DatabaseContext.getDBConnection();
      Document existent = tableRepository.findRegisterbyId(tableName,db,values.get(0));
      if(existent!=null)
        throw new Exception("The selected id is already in the database!");
      tableRepository.checkInsertConstraints(db,currentDatabase,tableName,values);

      String params="";
      for(int i=1 ; i<values.size();i++) {
        params+=values.get(i);
        if(i!=values.size()-1)
          params+="#";
      }

      Document doc = new Document("_id",values.get(0)).append("value",params);
      tableRepository.insertRegister(tableName,db,doc);

      for(int i=1 ; i<attr.size() ; i++)
      {
        String col = attr.get(i).split("#")[0];
        List<String> indexes = tableRepository.findIndexesForColumn(currentDatabase,tableName,col);
        if(!indexes.isEmpty())
        {
          Document elemIndex = tableRepository.findRegisterbyId(indexes.get(0),db,values.get(i));
          if(elemIndex==null)
            tableRepository.insertRegister(indexes.get(0),db,new Document("_id",values.get(i)).append("value",values.get(0)));
          else
          {
            String old_val = elemIndex.getString("value");
            tableRepository.updateRegisterById(indexes.get(0),db,values.get(i),old_val+"#"+values.get(0));
          }
        }
      }

      response="Registration added to Table "+tableName;
    }catch(Exception e){
      response=e.getMessage();
    }
    return response;
  }

  public String deleteRecords(String tableName, String id) {
    String response;
    try {
      MongoDatabase db = DatabaseContext.getDBConnection();
      String databaseName = DatabaseContext.getCurrentDatabase();

      Document existent = tableRepository.findRegisterbyId(tableName, db, id);
      if (existent == null) {
        throw new Exception("Record with id " + id + " does not exist in table " + tableName);
      }

      //Gaseste tabelele cu foreign key la id-ul tabelului curent
      List<String> referencingTables = tableRepository.getReferencingTables(databaseName, tableName);

      for (String refTable : referencingTables) {
        String referencingColumn = tableRepository.getReferencingColumn(databaseName, refTable, tableName);
        System.out.println("TableService: Table " + tableName + " is referenced by table " + refTable +" on column " + referencingColumn);

        //Opreste stergerea daca id-ul este gasit
        boolean isReferenced = tableRepository.isRecordReferenced(databaseName, refTable, db, referencingColumn, id);
        if (isReferenced) {
          throw new Exception("Delete rejected:  Record with id " + id + " referenced by table " + refTable + " on column " + referencingColumn + ".");
        }
      }

      List<String> attr = tableRepository.getTableStructure(databaseName,tableName);
      String[] values = existent.getString("value").split("#");
      for(int i=1 ; i<attr.size() ; i++){
        String column = attr.get(i).split("#")[0];
        List<String> indexes = tableRepository.findIndexesForColumn(databaseName,tableName,column);
        if(!indexes.isEmpty()){
          Document indexDoc = tableRepository.findRegisterbyId(indexes.get(0),db,values[i-1]);
          String[] elem = indexDoc.getString("value").split("#");
          if(elem.length == 1)
            tableRepository.deleteRegisterById(indexes.get(0),db,values[i-1]);
          else{
            String newvalue = "";
            for(int j=0 ; j<elem.length ; j++)
              if(!elem[j].equals(id))
                newvalue+=elem[j]+"#";
            newvalue = newvalue.substring(0,newvalue.length()-1);
            tableRepository.updateRegisterById(indexes.get(0),db,values[i-1],newvalue);
          }
        }
      }

      tableRepository.deleteRegisterById(tableName, db, id);


      response = "Record with id " + id + " successfully deleted from table " + tableName;
    } catch (Exception e) {
      response = e.getMessage();
    }
    return response;
  }

  private String findColumnInStructure(List<String> structure, String column){
      for (String s : structure)
          if (s.startsWith(column))
              return s.split("#")[1];
    return null;
  }

  private void validateSelect(String database, List<String> tables, List<String> columns, List<Condition> conditions) throws Exception {
    if(tables.size()==1)
    {
      String t = tables.get(0);
      List<String> attr = tableRepository.getTableStructure(database,t);
      for (String column : columns)
        if (findColumnInStructure(attr, column)==null)
          throw new Exception("Column " + column + " does not exist in table " + t);
      for (Condition c : conditions)
        if (findColumnInStructure(attr, c.getColumn())==null)
          throw new Exception("Column " + c.getColumn()+ " does not exist in table "+t);
    }
    else{
      for (String column : columns){
        int index = tables.indexOf(column.split("\\.")[0]);
        String t = tables.get(index-1);
        List<String> attr = tableRepository.getTableStructure(database,t);
        if(findColumnInStructure(attr,column.split("\\.")[1])==null)
          throw new Exception("Column " + column.split("\\.")[1] + " does not exist in table " + t);
      }
      for (Condition c : conditions){
        String col = c.getColumn();
        int index = tables.indexOf(col.split("\\.")[0]);
        String t = tables.get(index-1);
        List<String> attr = tableRepository.getTableStructure(database,t);
        String type = findColumnInStructure(attr,col.split("\\.")[1]);
        if(type==null)
          throw new Exception("Column " + col.split("\\.")[1] + " does not exist in table " + t);
        c.validateValue(type);
      }
    }
  }

  public String selectService(List<String> tables, List<String> columns, List<Condition> conditions, boolean distinct){
    String response="";

    try {
      MongoDatabase db = DatabaseContext.getDBConnection();
      String databaseName = DatabaseContext.getCurrentDatabase();
      validateSelect(databaseName,tables,columns,conditions);

      if(tables.size()==1) {
        //For one table
        String t = tables.get(0);
        List<List<String>> idsByCondition = new ArrayList<>();

        for (Condition condition : conditions) {
          //Identify applicable indexes for column
          List<String> indexes = tableRepository.findIndexesForColumn(databaseName, t, condition.getColumn());
          List<String> attr = tableRepository.getTableStructure(databaseName,t);
          String type = findColumnInStructure(attr,condition.getColumn());

          if(type==null)
            throw new Exception("Column " + condition.getColumn() + " does not exist in table " + t);

          if(tableRepository.isPrimaryKey(databaseName, t, condition.getColumn())){
            //Use default index
            List<org.bson.Document> filteredItems = tableRepository.findAllByIdCondition(t, db, condition, type);
            List<String> itemIds = filteredItems.stream()
                    .map(doc -> doc.get("_id").toString())
                    .filter(Objects::nonNull)
                    .toList();

            idsByCondition.add(itemIds);
          } else if(!indexes.isEmpty()){
            //Use index
            List<org.bson.Document> filteredItems = tableRepository.findAllByIdCondition(indexes.get(0), db, condition, type);
            List<String> itemIds = filteredItems.stream()
                    .map(doc -> doc.get("value"))
                    .filter(Objects::nonNull)
                    .flatMap(value -> Arrays.stream(value.toString().split("#")))
                    .toList();

            idsByCondition.add(itemIds);
          }else{
            //Table scan and filter
            List<org.bson.Document> filteredItems = tableRepository.findAllByColumnCondition(t, databaseName, db, condition);

            List<String> itemIds = filteredItems.stream()
                    .map(doc -> doc.get("_id").toString())
                    .filter(Objects::nonNull)
                    .toList();

            idsByCondition.add(itemIds);
          }
        }

        //Get common results from all conditions
        Set<String> commonIds = idsByCondition.stream()
                .map(HashSet::new)
                .reduce((set1, set2) -> {
                  set1.retainAll(set2);
                  return set1;
                })
                .orElse(new HashSet<>());

        List<org.bson.Document> entries = commonIds.stream()
                .map(entryId -> tableRepository.findRegisterbyId(t, db, entryId))
                .toList();

        //Apply projections
        List<List<String>> projectedEntries = tableRepository.applyProjectionsToEntries(databaseName, t, columns, entries);

        //Apply distinct
        if(distinct){
          Set<List<String>> distinctSet = new HashSet<>(projectedEntries);
          projectedEntries = new ArrayList<>(distinctSet);
        }

        return String.join(
                "|",
                Stream.concat(
                        Stream.of(String.join(",", columns)),
                        projectedEntries.stream().map(line -> String.join(",", line))
                ).toList()
        );
      }
      else {
        //TODO for multiple tables
      }

    } catch (Exception e) {
      response = e.getMessage();
    }

    return response;
  }
}
