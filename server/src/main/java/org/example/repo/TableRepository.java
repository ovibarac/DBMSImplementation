package org.example.repo;

import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import org.example.model.Column;
import org.example.model.Condition;
import org.example.model.ForeignKey;
import org.example.model.JoinCondition;
import org.example.utils.DatabaseXmlUtil;
import org.example.utils.IndexXmlUtil;
import org.example.utils.TableXmlUtil;
import org.example.utils.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class TableRepository {
  String XML_FILE_PATH = DatabaseConfig.XML_FILE_PATH;

  public void createTable(String databaseName, String tableName, List<Column> columns, Set<String> primaryKeys, List<ForeignKey> foreignKeys) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);

    Element dbElement = DatabaseXmlUtil.findDatabaseElement(doc, databaseName);
    if (dbElement == null) {
      throw new Exception("Database does not exist");
    }

    for(ForeignKey fk : foreignKeys)
      checkForeignKey(databaseName,fk);

    TableXmlUtil.createTableElement(doc, dbElement, tableName, columns, primaryKeys, foreignKeys);

    XmlUtil.writeXmlFile(doc, XML_FILE_PATH);
  }

  private void checkForeignKey(String databaseName, ForeignKey fk) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);
    TableXmlUtil.findTableElement(doc,databaseName,fk.getReferencedTable());
    List<String> structure = getTableStructure(databaseName,fk.getReferencedTable());
    int ok = 0;
    for(String attribute : structure){
      if(attribute.split("#")[0].equals(fk.getReferencedColumn())){
        ok=1;
        break;
      }
    }
    if(ok==0)
      throw new Exception("Column "+fk.getReferencedColumn()+" does not exist in Table "+fk.getReferencedTable());
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

    /**String tableFileName = tableName + ".kv";
    File kvFile = new File(tableFileName);
    if (kvFile.exists()) {
      kvFile.delete();
      System.out.println("Deleted table file: " + tableFileName);
    }**/
  }

  public org.bson.Document findRegisterbyId(String table, MongoDatabase db, String id){
    MongoCollection<org.bson.Document> collection = db.getCollection(table);
    org.bson.Document filter = new org.bson.Document("_id",id);

    return collection.find(filter).first();
  }

  public List<org.bson.Document> findAllByIdCondition(String table, MongoDatabase db, Condition condition, String type) {
    List<org.bson.Document> items = new ArrayList<>();
    MongoCollection<org.bson.Document> collection = db.getCollection(table);

    try {
      Object parsedValue = condition.parseValue(type);

      List<org.bson.Document> pipeline = new ArrayList<>();

      if (!type.toUpperCase().contains("VARCHAR")) {
        String castOperator = type.equalsIgnoreCase("INT") ? "$toInt" :
                type.equalsIgnoreCase("FLOAT") ? "$toDecimal" :
                        "$toDouble";
        pipeline.add(new org.bson.Document("$addFields",
                new org.bson.Document("typedId", new org.bson.Document(castOperator, "$_id"))));
      } else {
        pipeline.add(new org.bson.Document("$addFields",
                new org.bson.Document("typedId", "$_id")));
      }

      org.bson.Document matchFilter = new org.bson.Document();
      switch (condition.getSign()) {
        case "=":
          matchFilter.append("typedId", parsedValue);
          break;
        case "<":
          matchFilter.append("typedId", new org.bson.Document("$lt", parsedValue));
          break;
        case "<=":
          matchFilter.append("typedId", new org.bson.Document("$lte", parsedValue));
          break;
        case ">":
          matchFilter.append("typedId", new org.bson.Document("$gt", parsedValue));
          break;
        case ">=":
          matchFilter.append("typedId", new org.bson.Document("$gte", parsedValue));
          break;
        case "!=":
          matchFilter.append("typedId", new org.bson.Document("$ne", parsedValue));
          break;
        case "LIKE":
          if (!type.toUpperCase().contains("VARCHAR")) {
            throw new IllegalArgumentException("LIKE operator is only supported for VARCHAR type.");
          }
          matchFilter.append("typedId", new org.bson.Document("$regex", parsedValue.toString()).append("$options", "i"));
          break;
        default:
          throw new IllegalArgumentException("Unsupported sign: " + condition.getSign());
      }
      pipeline.add(new org.bson.Document("$match", matchFilter));

      AggregateIterable<org.bson.Document> results = collection.aggregate(pipeline);
      try (MongoCursor<org.bson.Document> cursor = results.iterator()) {
        while (cursor.hasNext()) {
          items.add(cursor.next());
        }
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    return items;
  }

  public List<org.bson.Document> findAllByColumnCondition(String table, String databaseName, MongoDatabase db, Condition condition) throws Exception {
    List<org.bson.Document> items = new ArrayList<>();
    int columnPosition = getColumnPositionInTableStructure(databaseName, table, condition.getColumn()) - 1;

    if(columnPosition < 0){
      return items;
    }

    MongoCollection<org.bson.Document> collection = db.getCollection(table);
    for (org.bson.Document document : collection.find()) {
      String value = document.getString("value");
      String[] values = value.split("#");

      try {
        if (columnPosition >= values.length) {
          return items;
        }

        String columnValue = values[columnPosition];
        boolean matches = false;
        String conditionValue = condition.getValue();

        matches = switch (condition.getSign()) {
          case "=" -> columnValue.equals(conditionValue);
          case "<" -> columnValue.compareTo(conditionValue) < 0;
          case "<=" -> columnValue.compareTo(conditionValue) <= 0;
          case ">" -> columnValue.compareTo(conditionValue) > 0;
          case ">=" -> columnValue.compareTo(conditionValue) >= 0;
          case "!=" -> !columnValue.equals(conditionValue);
          case "LIKE" -> columnValue.matches(".*" + condition.getValue() + ".*");
          default -> throw new IllegalArgumentException("Unsupported sign: " + condition.getSign());
        };

        if (matches) {
          items.add(document);
        }

      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }

    return items;
  }

  public List<List<String>> applyProjectionsToEntries(String databaseName, String table, List<String> columns, List<org.bson.Document> entries) {
    List<Integer> positions = columns.stream()
            .map(column -> {
              try {
                return getColumnPositionInTableStructure(databaseName, table, column);
              } catch (Exception e) {
                throw new RuntimeException("Projection " + column + " could not be applied");
              }
            })
            .toList();

    List<List<String>> projected = new ArrayList<>();
    for (org.bson.Document document : entries) {
      String value = document.getString("value");
      String id = document.getString("_id");
      String[] values = value.split("#");

      List<String> row = new ArrayList<>();
      positions.forEach(position -> {
        if(position == 0){
          row.add(id);
        }else{
          row.add(values[position - 1]);
        }
      });

      projected.add(row);

    }

    return projected;
  }


  public int getColumnPositionInTableStructure(String databaseName, String table, String columnName) throws Exception {
    List<String> structure = getTableStructure(databaseName, table);

    for(int i = 0; i < structure.size(); i++){
      String attribute = structure.get(i);
      if(attribute.split("#")[0].equals(columnName)){
        return i;
      }
    }

    return -1;
  }

  public boolean isPrimaryKey(String databaseName, String tableName, String columnName) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    Element primaryKeyElement = (Element) tableElement.getElementsByTagName("primaryKey").item(0);
    if (primaryKeyElement == null) {
      return false;
    }

    NodeList pkAttributes = XmlUtil.getAllChildElements(doc, primaryKeyElement, "pkAttribute");
    for (int i = 0; i < pkAttributes.getLength(); i++) {
      Element pkNode = (Element) pkAttributes.item(i);
      String pkAttribute = pkNode.getTextContent().trim();
      if (pkAttribute.equals(columnName)) {
        return true;
      }
    }
    return false;
  }

  public boolean findRegisterbyColumnValue(String table, String databaseName, MongoDatabase db, String columnName,  String columnValue) throws Exception {
    //Gaseste pozitia coloanei in campul value din Mongo
    int position = getColumnPositionInTableStructure(databaseName, table, columnName) - 1;
    if(position < 0){
      return false;
    }

    MongoCollection<org.bson.Document> collection = db.getCollection(table);

    //Cauta o inregistrare cu valoarea cautata in pozitia position din campul value
    for (org.bson.Document document : collection.find()) {
      String value = document.getString("value");
      String[] values = value.split("#");

      if (position < values.length && values[position].equals(columnValue)) {
        return true;
      }
    }
    return false;
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

  public List<String> getReferencingTables(String databaseName, String tableName) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);
    Element databaseElement = DatabaseXmlUtil.findDatabaseElement(doc, databaseName);
    if (databaseElement == null) {
      throw new Exception("Database does not exist");
    }

    NodeList tableNodes = XmlUtil.getAllChildElements(doc, databaseElement, "Table");
    List<String> referencingTables = new ArrayList<>();

    for (int i = 0; i < tableNodes.getLength(); i++) {
      Element tableElement = (Element) tableNodes.item(i);
      NodeList foreignKeys = XmlUtil.getAllChildElements(doc, tableElement, "foreignKey");

      for (int j = 0; j < foreignKeys.getLength(); j++) {
        Element foreignKeyElement = (Element) foreignKeys.item(j);
        String refTable = XmlUtil.getChildElementTextContent(foreignKeyElement, "refTable");
        if (refTable != null && refTable.equals(tableName)) {
          referencingTables.add(tableElement.getAttribute("tableName"));
        }
      }
    }

    return referencingTables;
  }

  public String getReferencingColumn(String databaseName, String tableName, String referencedTable) throws Exception {
    Document doc = XmlUtil.loadXmlFile(XML_FILE_PATH);
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    NodeList foreignKeys = XmlUtil.getAllChildElements(doc, tableElement, "foreignKey");
    for (int i = 0; i < foreignKeys.getLength(); i++) {
      Element foreignKeyElement = (Element) foreignKeys.item(i);
      String refTable = XmlUtil.getChildElementTextContent(foreignKeyElement, "refTable");
      if (refTable != null && refTable.equals(referencedTable)) {
        return XmlUtil.getChildElementTextContent(foreignKeyElement, "refAttribute");
      }
    }

    throw new Exception("No foreign key referencing table " + referencedTable + " found in table " + tableName);
  }

  public List<String> findIndexesForColumn(String databaseName, String tableName, String columnName) throws Exception {
    List<String> indexNames = new ArrayList<>();

    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    Element tableElement = TableXmlUtil.findTableElement(doc, databaseName, tableName);

    NodeList indexFiles = tableElement.getElementsByTagName("IndexFile");
    for (int j = 0; j < indexFiles.getLength(); j++) {
      Element indexFileElement = (Element) indexFiles.item(j);

      NodeList indexAttributes = indexFileElement.getElementsByTagName("IAttribute");
      for (int k = 0; k < indexAttributes.getLength(); k++) {
        Element indexAttributeElement = (Element) indexAttributes.item(k);
        if (columnName.equals(indexAttributeElement.getTextContent().trim())) {
          indexNames.add(indexFileElement.getAttribute("indexName"));
          indexNames.add(indexFileElement.getAttribute("isUnique"));
        }
      }
    }

    return indexNames;
  }

  public boolean isRecordReferenced(String databaseName, String referencingTable, MongoDatabase db, String columnName, String id) throws Exception {
    List<String> indexNames = findIndexesForColumn(databaseName, referencingTable, columnName);

    if(!indexNames.isEmpty()) {
      //Cauta id folosind index-ul daca exista
      String indexName = indexNames.get(0);

      return findRegisterbyId(indexName, db, id) != null;
    }else{
      //Table scan daca nu exista index
      return findRegisterbyColumnValue(referencingTable, databaseName, db, columnName, id);
    }
  }

  public void deleteRegisterById(String tableName, MongoDatabase db, String id) {
    MongoCollection<org.bson.Document> collection = db.getCollection(tableName);
    org.bson.Document filter = new org.bson.Document("_id", id);
    collection.deleteOne(filter);
  }

  public void createIndex(String indexName, String databaseName, String tableName, List<String> columnNames, boolean isUnique, MongoDatabase db) throws Exception {
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

    db.createCollection(indexFileName);

    checkIndexUnique(db,isUnique,tableName,indexFileName,databaseName,columnNames);
  }

  private void checkIndexUnique(MongoDatabase db, boolean isUnique, String tableName, String indexFileName, String databaseName, List<String> columnNames) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    MongoCollection<org.bson.Document> collection = db.getCollection(tableName);
    MongoCollection<org.bson.Document> indexCollection = db.getCollection(indexFileName);
    int pos = getColumnPositionInTableStructure(databaseName,tableName,columnNames.get(0)) -1;


    for(org.bson.Document collectionDoc : collection.find()) {
      String value = collectionDoc.getString("value").split("#")[pos];
      org.bson.Document query = new org.bson.Document("_id", value);
      org.bson.Document indexDoc = indexCollection.find(query).first();

      String id = collectionDoc.getString("_id");
      org.bson.Document elem = new org.bson.Document("_id", value).append("value", id);

      if(indexDoc == null){
        indexCollection.insertOne(elem);
      }
      else {
        if (isUnique) {
          indexCollection.drop();
          throw new Exception("Current data does not respect the unique constraint of " + indexFileName);
        }
        else {
          String updated = indexDoc.getString("value");
          indexCollection.updateOne(eq("_id",value),set("value",updated+"#"+id));
        }
      }
    }

    IndexXmlUtil.createIndexElement(doc, databaseName, tableName, columnNames, isUnique, indexFileName);
    XmlUtil.writeXmlFile(doc, DatabaseConfig.XML_FILE_PATH);
  }

  public void updateRegisterById(String tableName, MongoDatabase db, String id, String value){
    MongoCollection<org.bson.Document> collection = db.getCollection(tableName);
    collection.updateOne(eq("_id",id),set("value",value));
  }

  private List<String> getForeignKeys (Document doc, String database, String tablename) throws Exception {
    List<String> fKeys = new ArrayList<>();
    Element tableElement = TableXmlUtil.findTableElement(doc, database, tablename);
    NodeList elems = tableElement.getElementsByTagName("foreignKey");
    for(int i=0 ; i<elems.getLength() ; i++) {
      String key = "";
      Element e = (Element) elems.item(i);
      NodeList tableColumn = e.getElementsByTagName("fkAttribute");
      key +=  tableColumn.item(0).getTextContent();
      NodeList refTable = e.getElementsByTagName("refTable");
      key += "#"+refTable.item(0).getTextContent();
      NodeList refColumn = e.getElementsByTagName("refAttribute");
      key += "#"+refColumn.item(0).getTextContent();
      fKeys.add(key);
    }
    return fKeys;
  }

  private int getColumn (List<String> attr, String column){
    for(int i=0 ; i<attr.size() ; i++)
      if(attr.get(i).contains(column))
        return i;
    return -1;
  }

  public void checkInsertConstraints(MongoDatabase db, String databaseName, String tableName, List<String> values) throws Exception {
    Document doc = XmlUtil.loadXmlFile(DatabaseConfig.XML_FILE_PATH);
    List<String> attr = getTableStructure(databaseName,tableName);
    List<String> fKeys = getForeignKeys(doc,databaseName,tableName);
    for(int i=0 ; i<fKeys.size() ; i++)
    {
      String key = fKeys.get(i);
      int index = getColumn(attr,key.split("#")[0]);
      String value = values.get(index);
      org.bson.Document d = findRegisterbyId(key.split("#")[1],db,value);
      if(d==null)
        throw new Exception("Foreign key "+key.split("#")[2]+" with value "+value+" does not exist in Table "+key.split("#")[1]);
    }
    for(int i=1 ; i<attr.size() ; i++)
    {
      String column = attr.get(i).split("#")[0];
      List<String> indexes = findIndexesForColumn(databaseName,tableName,column);
      if(!indexes.isEmpty())
        if(indexes.get(1).equals("1")){
          org.bson.Document elem = findRegisterbyId(indexes.get(0),db,values.get(i));
          if(elem!=null)
            throw new Exception("Register violates unique constraint on index file "+indexes.get(0)+" for column "+column);
        }
    }
  }

  //pentru a lucra doar cu datele filtrate, vom crea 2 fisiere temporare pentru a le stoca, vor fi sterse ulterior dupa finalizare
  public void createTemporaryTables(MongoDatabase db, String databaseName, List<List<org.bson.Document>> finalTables, JoinCondition jcond) throws Exception {
    for(int i=0 ; i<finalTables.size() ; i++) {
      List<org.bson.Document> l = finalTables.get(i);
      int index;
      String temp_table;
      if(i==0) {
        temp_table = "temp" + jcond.getTable1();
        index = getColumnPositionInTableStructure(databaseName, jcond.getTable1(), jcond.getColumn());
      }
      else {
        temp_table = "temp" + jcond.getTable2();
        index = getColumnPositionInTableStructure(databaseName, jcond.getTable2(), jcond.getColumn());
      }
      db.createCollection(temp_table);
      MongoCollection<org.bson.Document> tempCollection = db.getCollection(temp_table);
      /**
       * daca index = 0, atunci atat _id, cat si value vor avea valoarea cheii primare, pentru a pastra aceeasi logica
       * daca index > 0, se scade 1 si se ia valoarea din value dupa split pentru _id si cheia primara asociata pentru
       * value
       */
      for(int j=0 ; j<l.size() ; j++) {
        String id = l.get(j).getString("_id");
        if (index == 0) {
          tempCollection.insertOne(new org.bson.Document("_id", id).append("value", id));
        } else {
          String value = l.get(j).getString("value");
          String col_value = value.split("#")[index-1];

          org.bson.Document d = findRegisterbyId(temp_table, db, col_value);
          if (d != null) {
            String updated = d.getString("value");
            tempCollection.updateOne(eq("_id", col_value), set("value", updated + "#" + id));
          } else
            tempCollection.insertOne(new org.bson.Document("_id", col_value).append("value", id));
        }
      }
      //sortarea e returnata ca un iterator, deci o vom aplica direct in algoritmi
    }
  }

  //se poate folosi functia pt ambii algoritmi de join
  public List<List<String>> getCombinationsFromDocs(MongoDatabase db, String databaseName, String value1, String value2, List<String> tables, List<String> columns) throws Exception {
    List<List<String>> results = new ArrayList<>();
    int size1 = getTableStructure(databaseName,tables.get(0)).size();

    List<Integer> positions = new ArrayList<>();
    for(String column : columns){
      String t = column.split("\\.")[0];
      String c = column.split("\\.")[1];
      int index = tables.indexOf(t)-1;
      if(index==0)
        positions.add(getColumnPositionInTableStructure(databaseName,tables.get(0),c));
      else
        positions.add(size1+getColumnPositionInTableStructure(databaseName,tables.get(2),c));
    }

    String[] ids1 = value1.split("#");
    String[] ids2 = value2.split("#");
    for(int i=0 ; i<ids1.length ; i++)
    {
      org.bson.Document d1 = findRegisterbyId(tables.get(0),db,ids1[i]);
      for(int j=0 ; j<ids2.length ; j++) {
        org.bson.Document d2 = findRegisterbyId(tables.get(2), db, ids2[j]);

        String merged = d1.getString("_id")+"#"+d1.getString("value")+"#"+d2.getString("_id")+"#"+d2.getString("value");
        List<String> row = new ArrayList<>();
        String[] elems = merged.split("#");

        for(int poz : positions)
          row.add(elems[poz]);
        results.add(row);
      }
    }
    return results;
  }

  public List<List<String>> MergeJoin(MongoDatabase db, String databaseName, List<String> tables, List<String> columns) throws Exception {
    List<List<String>> results= new ArrayList<>();

    MongoCollection<org.bson.Document> temp1 = db.getCollection("temp"+tables.get(0));
    MongoCollection<org.bson.Document> temp2 = db.getCollection("temp"+tables.get(2));

    //colectiile sortate
    MongoCursor<org.bson.Document> sorted1 = temp1.find().sort(Sorts.ascending("_id")).iterator();
    MongoCursor<org.bson.Document> sorted2 = temp2.find().sort(Sorts.ascending("_id")).iterator();

    org.bson.Document doc1 = sorted1.hasNext() ? sorted1.next() : null;
    org.bson.Document doc2 = sorted2.hasNext() ? sorted2.next() : null;

    //interclasare
    while(doc1!=null && doc2!=null){
      String id1 = doc1.getString("_id");
      String id2 = doc2.getString("_id");

      //o singura pereche de _id ar putea egale deoarece in index file id-urile care contin aceeasi valoare
      //sunt stocate impreuna
      if(id1.equals(id2)){
        results.addAll(getCombinationsFromDocs(db,databaseName,doc1.getString("value"),doc2.getString("value"),tables,columns));
        doc1 = sorted1.hasNext() ? sorted1.next() : null;
        doc2 = sorted2.hasNext() ? sorted2.next() : null;
      }
      else if(id1.compareTo(id2)<0)
        doc1 = sorted1.hasNext() ? sorted1.next() : null;
      else
        doc2 = sorted2.hasNext() ? sorted2.next() : null;
    }

    //eliminarea colectiilor temporare
    temp1.drop();
    temp2.drop();
    return results;
  }
}
