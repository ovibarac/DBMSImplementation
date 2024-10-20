package org.example.model;

import java.util.List;
import java.util.Set;

public class TableSchema {
  private List<Column> columns;
  private Set<String> primaryKeys;
  private List<ForeignKey> foreignKeys;

  public TableSchema(List<Column> columns, Set<String> primaryKeys, List<ForeignKey> foreignKeys) {
    this.columns = columns;
    this.primaryKeys = primaryKeys;
    this.foreignKeys = foreignKeys;
  }

  public List<Column> getColumns() {
    return columns;
  }

  public Set<String> getPrimaryKeys() {
    return primaryKeys;
  }

  public List<ForeignKey> getForeignKeys() {
    return foreignKeys;
  }

  @Override
  public String toString() {
    return "Columns: " + columns + ", Primary Keys: " + primaryKeys + ", Foreign Keys: " + foreignKeys;
  }
}