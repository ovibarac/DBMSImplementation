package org.example.repo;

import org.example.model.Column;
import org.example.model.ForeignKey;
import org.example.utils.DatabaseXmlUtil;
import org.example.utils.TableXmlUtil;
import org.example.utils.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    tableElement.getParentNode().removeChild(tableElement);
    XmlUtil.writeXmlFile(doc, DatabaseConfig.XML_FILE_PATH);

    String tableFileName = tableName + ".kv";
    File kvFile = new File(tableFileName);
    if (kvFile.exists()) {
      kvFile.delete();
      System.out.println("Deleted table file: " + tableFileName);
    }
    //TODO delete index files for table
  }
}
