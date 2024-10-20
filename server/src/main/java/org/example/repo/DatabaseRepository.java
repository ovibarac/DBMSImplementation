package org.example.repo;

import org.example.utils.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

public class DatabaseRepository {
  String XML_FILE_PATH = DatabaseConfig.XML_FILE_PATH;
  public void createDatabase(String databaseName) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);

    if (XmlUtil.findDatabaseElement(doc, databaseName) != null) {
      throw new Exception("Database '" + databaseName + "' already exists.");
    }

    XmlUtil.createDatabaseElement(doc, databaseName);

    XmlUtil.writeXmlFile(doc, XML_FILE_PATH);
  }

  public void dropDatabase(String databaseName) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    Element dbElement = XmlUtil.findDatabaseElement(doc, databaseName);
    if (dbElement != null) {
      NodeList tables = dbElement.getElementsByTagName("Table");
      for (int i = 0; i < tables.getLength(); i++) {
        Element tableElement = (Element) tables.item(i);
        String tableFileName = tableElement.getAttribute("fileName");
        File kvFile = new File(tableFileName);
        if (kvFile.exists()) {
          kvFile.delete();
          System.out.println("Deleted table file: " + tableFileName);
        }
      }

      dbElement.getParentNode().removeChild(dbElement);
      XmlUtil.writeXmlFile(doc, DatabaseConfig.XML_FILE_PATH);
    } else {
      throw new Exception("Database '" + databaseName + "' does not exist.");
    }
  }
}