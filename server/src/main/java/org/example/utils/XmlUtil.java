package org.example.utils;

import org.example.model.Column;
import org.example.model.ForeignKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

public class XmlUtil {
  public static Document loadXmlFile(String filePath) throws Exception {
    File xmlFile = new File(filePath);
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc;

    if (xmlFile.exists()) {
      doc = dBuilder.parse(xmlFile);
    } else {
      doc = dBuilder.newDocument();
      Element rootElement = doc.createElement("Databases");
      doc.appendChild(rootElement);
    }

    return doc;
  }

  public static void writeXmlFile(Document doc, String filePath) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();

    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    StringWriter stringWriter = new StringWriter();
    StreamResult streamResult = new StreamResult(stringWriter);
    transformer.transform(new DOMSource(doc), streamResult);

    String xmlOutput = stringWriter.toString();

    String[] lines = xmlOutput.split("\n");
    StringBuilder cleanedOutput = new StringBuilder();

    for (String line : lines) {
      String trimmedLine = line.trim();

      if (!trimmedLine.isEmpty()) {
        cleanedOutput.append(line).append("\n");
      } else if (cleanedOutput.length() > 0 && cleanedOutput.charAt(cleanedOutput.length() - 1) != '\n') {
        cleanedOutput.append("\n");
      }
    }

    try (FileWriter writer = new FileWriter(filePath)) {
      if (cleanedOutput.length() > 0 && cleanedOutput.charAt(cleanedOutput.length() - 1) == '\n') {
        cleanedOutput.setLength(cleanedOutput.length() - 1); // Remove last newline
      }
      writer.write(cleanedOutput.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
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

  public static Element findTableElement(Document doc, Element dbElement, String tableName) {
    NodeList tables = dbElement.getElementsByTagName("Table");
    for (int j = 0; j < tables.getLength(); j++) {
      Element tableElement = (Element) tables.item(j);
      if (tableElement.getAttribute("tableName").equals(tableName)) {
        return tableElement;
      }
    }
    return null;
  }

  public static Element getOrCreateChildElement(Document doc, Element parent, String childName) {
    NodeList childNodes = parent.getElementsByTagName(childName);
    if (childNodes.getLength() > 0) {
      return (Element) childNodes.item(0);
    } else {
      Element childElement = doc.createElement(childName);
      parent.appendChild(childElement);
      return childElement;
    }
  }

  public static void createDatabaseElement(Document doc, String databaseName) {
    Element dbElement = doc.createElement("DataBase");
    dbElement.setAttribute("dataBaseName", databaseName);
    doc.getDocumentElement().appendChild(dbElement);
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
