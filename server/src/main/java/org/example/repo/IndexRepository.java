package org.example.repo;

import org.example.utils.IndexXmlUtil;
import org.example.utils.TableXmlUtil;
import org.example.utils.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class IndexRepository {
  public void createIndex(String indexName, String databaseName, String tableName, List<String> columnNames, boolean isUnique) throws Exception {
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

    IndexXmlUtil.createIndexElement(doc, databaseName, tableName, columnNames, isUnique, indexFileName);

    XmlUtil.writeXmlFile(doc, DatabaseConfig.XML_FILE_PATH);
    createIndexFile(indexFileName);
  }

  private void createIndexFile(String fileName) throws IOException {
    FileWriter writer = new FileWriter(fileName);
    writer.write("");
    writer.close();
  }
}
