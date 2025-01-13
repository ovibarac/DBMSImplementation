package org.example.model;

public class JoinCondition {
    private String table1;
    private String table2;
    private String column;

    public JoinCondition(String table1, String table2, String column) {
        this.table1 = table1;
        this.table2 = table2;
        this.column = column;
    }

    public String getTable1() {
        return table1;
    }

    public String getTable2() {
        return table2;
    }

    public String getColumn() {
        return column;
    }
}
