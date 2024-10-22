package org.example.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.List;

public class IndexXmlUtil {
  public static void createIndexElement(Document doc, String databaseName, String tableName, List<String> columnNames, boolean isUnique, String indexFileName) throws Exception {
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    Element indexFilesElement = XmlUtil.getOrCreateChildElement(doc, tableElement, "IndexFiles");
    Element indexFileElement = doc.createElement("IndexFile");
    indexFileElement.setAttribute("indexName", indexFileName);
    indexFileElement.setAttribute("isUnique", isUnique ? "1" : "0");
    indexFileElement.setAttribute("indexType", "BTree");

    Element indexAttributesElement = doc.createElement("IndexAttributes");
    for (String column : columnNames) {
      Element iAttributeElement = doc.createElement("IAttribute");
      iAttributeElement.setTextContent(column);
      indexAttributesElement.appendChild(iAttributeElement);
    }

    indexFileElement.appendChild(indexAttributesElement);
    indexFilesElement.appendChild(indexFileElement);
  }

  public static NodeList findIndexElements(Document doc, String databaseName, String tableName) throws Exception {
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);
    return XmlUtil.getAllChildElements(doc, tableElement, "IndexFiles");
  }
}
