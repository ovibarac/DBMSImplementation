package org.example.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DatabaseXmlUtil {
  public static void createDatabaseElement(Document doc, String databaseName) {
    Element dbElement = doc.createElement("DataBase");
    dbElement.setAttribute("dataBaseName", databaseName);
    doc.getDocumentElement().appendChild(dbElement);
  }

  public static Element findDatabaseElement(Document doc, String databaseName) {
    NodeList databases = doc.getElementsByTagName("DataBase");
    for (int i = 0; i < databases.getLength(); i++) {
      Element dbElement = (Element) databases.item(i);
      if (dbElement.getAttribute("dataBaseName").equals(databaseName)) {
        return dbElement;
      }
    }

    return null;
  }
}
