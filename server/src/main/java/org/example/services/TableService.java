package org.example.services;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
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
      MongoDatabase db = DatabaseContext.getDBConnection();
      db.createCollection(tableName);
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
        if(values.size()>Integer.parseInt(reqSize))
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

      String params="";
      for(int i=1 ; i<values.size();i++) {
        params+=values.get(i);
        if(i!=values.size()-1)
          params+="#";
      }

      Document doc = new Document("_id",values.get(0)).append("value",params);
      tableRepository.insertRegister(tableName,db,doc);
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

      Document existent = tableRepository.findRegisterbyId(tableName, db, id);
      if (existent == null) {
        throw new Exception("Record with id " + id + " does not exist in table " + tableName);
      }

      tableRepository.deleteRegisterById(tableName, db, id);
      response = "Record with id " + id + " successfully deleted from table " + tableName;
    } catch (Exception e) {
      response = e.getMessage();
    }
    return response;
  }
}
