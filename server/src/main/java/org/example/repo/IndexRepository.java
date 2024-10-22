package org.example.repo;

import org.example.utils.IndexXmlUtil;
import org.example.utils.XmlUtil;
import org.w3c.dom.Document;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class IndexRepository {
  public void createIndex(String indexName, String databaseName, String tableName, List<String> columnNames, boolean isUnique) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    String indexFileName = indexName + ".ind";

    //TODO verifica daca exista coloanele
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
