package org.example.model;

public class ForeignKey {
  private String columnName;
  private String referencedTable;
  private String referencedColumn;

  public ForeignKey(String columnName, String referencedTable, String referencedColumn) {
    this.columnName = columnName;
    this.referencedTable = referencedTable;
    this.referencedColumn = referencedColumn;
  }

  public String getColumnName() {
    return columnName;
  }

  public String getReferencedTable() {
    return referencedTable;
  }

  public String getReferencedColumn() {
    return referencedColumn;
  }

  @Override
  public String toString() {
    return "FOREIGN KEY (" + columnName + ") REFERENCES " + referencedTable + "(" + referencedColumn + ")";
  }
}
