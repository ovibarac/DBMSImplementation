package org.example.utils;

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

  public static NodeList getAllChildElements(Document doc, Element parent, String childName) {
    return parent.getElementsByTagName(childName);
  }
}
