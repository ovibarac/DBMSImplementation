package org.example.repo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.example.model.Column;
import org.example.model.ForeignKey;
import org.example.utils.DatabaseXmlUtil;
import org.example.utils.IndexXmlUtil;
import org.example.utils.TableXmlUtil;
import org.example.utils.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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

    TableXmlUtil.createTableElement(doc, dbElement, tableName, columns, primaryKeys, foreignKeys);

    XmlUtil.writeXmlFile(doc, XML_FILE_PATH);
    createTableFile(tableName + ".kv");
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

    String tableFileName = tableName + ".kv";
    File kvFile = new File(tableFileName);
    if (kvFile.exists()) {
      kvFile.delete();
      System.out.println("Deleted table file: " + tableFileName);
    }
  }

  public org.bson.Document findRegisterbyId(String table, MongoDatabase db, String id){
    MongoCollection<org.bson.Document> collection = db.getCollection(table);
    org.bson.Document filter = new org.bson.Document("id",id);
    return collection.find(filter).first();
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
}
