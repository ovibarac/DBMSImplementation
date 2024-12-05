package org.example.repo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import org.bson.types.ObjectId;
import org.example.model.Column;
import org.example.model.ForeignKey;
import org.example.utils.DatabaseXmlUtil;
import org.example.utils.IndexXmlUtil;
import org.example.utils.TableXmlUtil;
import org.example.utils.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TableRepository {
  String XML_FILE_PATH = DatabaseConfig.XML_FILE_PATH;

  public void createTable(String databaseName, String tableName, List<Column> columns, Set<String> primaryKeys, List<ForeignKey> foreignKeys) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);

    Element dbElement = DatabaseXmlUtil.findDatabaseElement(doc, databaseName);
    if (dbElement == null) {
      throw new Exception("Database does not exist");
    }

    for(ForeignKey fk : foreignKeys)
      checkForeignKey(databaseName,fk);

    TableXmlUtil.createTableElement(doc, dbElement, tableName, columns, primaryKeys, foreignKeys);

    XmlUtil.writeXmlFile(doc, XML_FILE_PATH);
    //createTableFile(tableName + ".kv");
  }

  private void checkForeignKey(String databaseName, ForeignKey fk) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);
    TableXmlUtil.findTableElement(doc,databaseName,fk.getReferencedTable());
    List<String> structure = getTableStructure(databaseName,fk.getReferencedTable());
    int ok = 0;
    for(String attribute : structure){
      if(attribute.split("#")[0].equals(fk.getReferencedColumn())){
        ok=1;
        break;
      }
    }
    if(ok==0)
      throw new Exception("Column "+fk.getReferencedColumn()+" does not exist in Table "+fk.getReferencedTable());
  }

  private void createTableFile(String fileName) throws IOException {
    FileWriter writer = new FileWriter(fileName);
    writer.write("");
    writer.close();
  }

  public void dropTable(String databaseName, String tableName) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);

    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);
    NodeList indexes = IndexXmlUtil.findIndexElements(doc, databaseName, tableName);

    for(int i = 0; i< indexes.getLength(); i++){
      Element node = (Element) indexes.item(i);
      String indexName = node.getAttribute("indexName");
      File indexFile = new File(indexName);
      if(indexFile.exists()){
        indexFile.delete();
        System.out.println("Delete index file: "+indexFile);
      }
    }

    tableElement.getParentNode().removeChild(tableElement);
    XmlUtil.writeXmlFile(doc, DatabaseConfig.XML_FILE_PATH);

    /**String tableFileName = tableName + ".kv";
    File kvFile = new File(tableFileName);
    if (kvFile.exists()) {
      kvFile.delete();
      System.out.println("Deleted table file: " + tableFileName);
    }**/
  }

  public org.bson.Document findRegisterbyId(String table, MongoDatabase db, String id){
    MongoCollection<org.bson.Document> collection = db.getCollection(table);
    org.bson.Document filter = new org.bson.Document("_id",id);

    return collection.find(filter).first();
  }

  public int getColumnPositionInTableStructure(String databaseName, String table, String columnName) throws Exception {
    List<String> structure = getTableStructure(databaseName, table);

    for(int i = 0; i < structure.size(); i++){
      String attribute = structure.get(i);
      if(attribute.split("#")[0].equals(columnName)){
        return i;
      }
    }

    return -1;
  }

  public boolean findRegisterbyColumnValue(String table, String databaseName, MongoDatabase db, String columnName,  String columnValue) throws Exception {
    //Gaseste pozitia coloanei in campul value din Mongo
    int position = getColumnPositionInTableStructure(databaseName, table, columnName) - 1;
    if(position < 0){
      return false;
    }

    MongoCollection<org.bson.Document> collection = db.getCollection(table);

    //Cauta o inregistrare cu valoarea cautata in pozitia position din campul value
    for (org.bson.Document document : collection.find()) {
      String value = document.getString("value");
      String[] values = value.split("#");

      if (position < values.length && values[position].equals(columnValue)) {
        return true;
      }
    }
    return false;
  }

  public void insertRegister(String table, MongoDatabase db, org.bson.Document d){
    MongoCollection<org.bson.Document> collection = db.getCollection(table);
    collection.insertOne(d);
  }

  public List<String> getTableStructure(String databaseName, String tableName) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    Element struct = (Element) tableElement.getElementsByTagName("Structure").item(0);
    NodeList attr = XmlUtil.getAllChildElements(doc,struct,"Attribute");

    List<String> structure = new ArrayList<>();
    for(int i=0 ; i<attr.getLength() ; i++){
      Element node = (Element) attr.item(i);
      String s1 = node.getAttribute("attributeName");
      String s2 = node.getAttribute("type");
      structure.add(s1+"#"+s2);
    }

    return structure;
  }

  public List<String> getReferencingTables(String databaseName, String tableName) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);
    Element databaseElement = DatabaseXmlUtil.findDatabaseElement(doc, databaseName);
    if (databaseElement == null) {
      throw new Exception("Database does not exist");
    }

    NodeList tableNodes = XmlUtil.getAllChildElements(doc, databaseElement, "Table");
    List<String> referencingTables = new ArrayList<>();

    for (int i = 0; i < tableNodes.getLength(); i++) {
      Element tableElement = (Element) tableNodes.item(i);
      NodeList foreignKeys = XmlUtil.getAllChildElements(doc, tableElement, "foreignKey");

      for (int j = 0; j < foreignKeys.getLength(); j++) {
        Element foreignKeyElement = (Element) foreignKeys.item(j);
        String refTable = XmlUtil.getChildElementTextContent(foreignKeyElement, "refTable");
        if (refTable != null && refTable.equals(tableName)) {
          referencingTables.add(tableElement.getAttribute("tableName"));
        }
      }
    }

    return referencingTables;
  }

  public String getReferencingColumn(String databaseName, String tableName, String referencedTable) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    NodeList foreignKeys = XmlUtil.getAllChildElements(doc, tableElement, "foreignKey");
    for (int i = 0; i < foreignKeys.getLength(); i++) {
      Element foreignKeyElement = (Element) foreignKeys.item(i);
      String refTable = XmlUtil.getChildElementTextContent(foreignKeyElement, "refTable");
      if (refTable != null && refTable.equals(referencedTable)) {
        return XmlUtil.getChildElementTextContent(foreignKeyElement, "refAttribute");
      }
    }

    throw new Exception("No foreign key referencing table " + referencedTable + " found in table " + tableName);
  }

  public List<String> findIndexesForColumn(String databaseName, String tableName, String columnName) throws Exception {
    List<String> indexNames = new ArrayList<>();

    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    NodeList indexFiles = tableElement.getElementsByTagName("IndexFile");
    for (int j = 0; j < indexFiles.getLength(); j++) {
      Element indexFileElement = (Element) indexFiles.item(j);

      NodeList indexAttributes = indexFileElement.getElementsByTagName("IAttribute");
      for (int k = 0; k < indexAttributes.getLength(); k++) {
        Element indexAttributeElement = (Element) indexAttributes.item(k);
        if (columnName.equals(indexAttributeElement.getTextContent().trim())) {
          indexNames.add(indexFileElement.getAttribute("indexName"));
        }
      }
    }

    return indexNames;
  }

  public boolean isRecordReferenced(String databaseName, String referencingTable, MongoDatabase db, String columnName, String id) throws Exception {
    List<String> indexNames = findIndexesForColumn(databaseName, referencingTable, columnName);

    if(!indexNames.isEmpty()) {
      //Cauta id folosind index-ul daca exista
      String indexName = indexNames.get(0);

      return findRegisterbyId(indexName, db, id) != null;
    }else{
      //Table scan daca nu exista index
      return findRegisterbyColumnValue(referencingTable, databaseName, db, columnName, id);
    }
  }

  public void deleteRegisterById(String tableName, MongoDatabase db, String id) {
    MongoCollection<org.bson.Document> collection = db.getCollection(tableName);
    org.bson.Document filter = new org.bson.Document("_id", id);
    collection.deleteOne(filter);
  }

  public void createIndex(String indexName, String databaseName, String tableName, List<String> columnNames, boolean isUnique, MongoDatabase db) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    String indexFileName = indexName + ".ind";

    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    ///Se extrage primary key din Table
    NodeList primaryKey = tableElement.getElementsByTagName("primaryKey");
    Element pkElem = (Element) primaryKey.item(0);


    for (String columnName : columnNames) {
      ///verificam daca coloana exista sau daca face parte din Primary Key
      boolean columnExists = TableXmlUtil.findColumn(doc,databaseName,tableName,columnName);
      if (!columnExists)
        throw new Exception("Column "+ columnName + " is not in table!");
      if (pkElem != null) {
        String primaryKeyAttribute = pkElem.getElementsByTagName("pkAttribute").item(0).getTextContent();
        if (columnName.equals(primaryKeyAttribute))
          throw new Exception("Column " + columnName + " is the primary key!");
      }
    }

    db.createCollection(indexFileName);

    checkIndexUnique(db,isUnique,tableName,indexFileName,databaseName,columnNames);
  }

  private void checkIndexUnique(MongoDatabase db, boolean isUnique, String tableName, String indexFileName, String databaseName, List<String> columnNames) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    MongoCollection<org.bson.Document> collection = db.getCollection(tableName);
    MongoCollection<org.bson.Document> indexCollection = db.getCollection(indexFileName);
    int pos = getColumnPositionInTableStructure(databaseName,tableName,columnNames.get(0)) -1;


    for(org.bson.Document collectionDoc : collection.find()) {
      String value = collectionDoc.getString("value").split("#")[pos];
      org.bson.Document query = new org.bson.Document("_id", value);
      org.bson.Document indexDoc = indexCollection.find(query).first();

      String id = collectionDoc.getString("_id");
      org.bson.Document elem = new org.bson.Document("_id", value).append("value", id);

      if(indexDoc == null){
        indexCollection.insertOne(elem);
      }
      else {
        if (isUnique) {
          indexCollection.drop();
          throw new Exception("Current data does not respect the unique constraint of " + indexFileName);
        }
        else {
          String updated = indexDoc.getString("value");
          indexCollection.updateOne(eq("_id",value),set("value",updated+"#"+id));
        }
      }
    }

    IndexXmlUtil.createIndexElement(doc, databaseName, tableName, columnNames, isUnique, indexFileName);
    XmlUtil.writeXmlFile(doc, DatabaseConfig.XML_FILE_PATH);
  }

  public void updateRegisterById(String tableName, MongoDatabase db, String id, String value){
    MongoCollection<org.bson.Document> collection = db.getCollection(tableName);
    collection.updateOne(eq("_id",id),set("value",value));
  }

  public void checkConstraints(MongoDatabase db, String databaseName, String tableName, List<String> values){
    
  }
}
