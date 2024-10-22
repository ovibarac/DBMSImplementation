package org.example.utils;

import org.example.model.Column;
import org.example.model.ForeignKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Set;

public class TableXmlUtil {
  public static Element findTableElement(Document doc, String databaseName, String tableName) throws Exception {
    Element dbElement = DatabaseXmlUtil.findDatabaseElement(doc, databaseName);
    if (dbElement == null) {
      throw new Exception("Database '" + databaseName + "' does not exist.");
    }

    NodeList tables = dbElement.getElementsByTagName("Table");
    for (int j = 0; j < tables.getLength(); j++) {
      Element tableElement = (Element) tables.item(j);
      if (tableElement.getAttribute("tableName").equals(tableName)) {
        return tableElement;
      }
    }
    throw new Exception("Table '" + tableName + "' does not exist in database '" + databaseName + "'.");
  }

  public static void createTableElement(Document doc, Element dbElement, String tableName, List<Column> columns, Set<String> primaryKeys, List<ForeignKey> foreignKeys){
    Element tablesElement = XmlUtil.getOrCreateChildElement(doc, dbElement, "Tables");
    Element tableElement = doc.createElement("Table");
    tableElement.setAttribute("tableName", tableName);
    tableElement.setAttribute("fileName", tableName + ".kv");

    Element structureElement = doc.createElement("Structure");
    for (Column column : columns) {
      createColumnElement(doc, column, structureElement);
    }
    tableElement.appendChild(structureElement);

    if(!primaryKeys.isEmpty()){
      Element pkElement = doc.createElement("primaryKey");
      for (String pk : primaryKeys) {
        createPkElement(doc, pk, pkElement);
      }
      tableElement.appendChild(pkElement);
    }

    if (!foreignKeys.isEmpty()) {
      Element fkElement = doc.createElement("foreignKeys");
      for (ForeignKey fk : foreignKeys) {
        createFkElement(doc, fk, fkElement);
      }
      tableElement.appendChild(fkElement);
    }

    tablesElement.appendChild(tableElement);
  }

  private static void createColumnElement(Document doc, Column column, Element structureElement) {
    Element attrElement = doc.createElement("Attribute");
    attrElement.setAttribute("attributeName", column.getName());
    attrElement.setAttribute("type", column.getType());
    structureElement.appendChild(attrElement);
  }

  private static void createPkElement(Document doc, String pk, Element pkElement) {
    Element pkAttr = doc.createElement("pkAttribute");
    pkAttr.setTextContent(pk);
    pkElement.appendChild(pkAttr);
  }

  private static void createFkElement(Document doc, ForeignKey fk, Element fkElement) {
    Element foreignKey = doc.createElement("foreignKey");
    Element fkAttr = doc.createElement("fkAttribute");
    fkAttr.setTextContent(fk.getColumnName());
    Element references = doc.createElement("references");
    Element refTable = doc.createElement("refTable");
    refTable.setTextContent(fk.getReferencedTable());
    Element refAttr = doc.createElement("refAttribute");
    refAttr.setTextContent(fk.getReferencedColumn());

    references.appendChild(refTable);
    references.appendChild(refAttr);
    foreignKey.appendChild(fkAttr);
    foreignKey.appendChild(references);
    fkElement.appendChild(foreignKey);
  }
}
