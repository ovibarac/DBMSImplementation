package org.example.model;

public class Column {
  private String name;
  private String type;

  public Column(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return name + " " + type;
  }
}
