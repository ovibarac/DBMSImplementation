package org.example.model;

public class Condition {
    private String column;
    private String sign;
    private String value;

    public Condition(String s) throws Exception {
        String[] params = s.split(" ");
        if(params.length != 3)
            throw new Exception("Invalid condition: "+s);
        this.column = params[0];
        this.sign = params[1];
        this.value = params[2];
    }

    public String getColumn() {
        return column;
    }

    public String getSign() {
        return sign;
    }

    public String getValue() {
        return value;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public void validateValue(String type) throws Exception {
        if(type.equals("INT")) {
            int value = Integer.parseInt(this.value);
        }
        if(type.equals("FLOAT")){
            float value = Float.parseFloat(this.value);
        }
        if(type.equals("DOUBLE")){
            double value = Double.parseDouble(this.value);
        }
        if(type.contains("varchar")){
            int j = 8;
            StringBuilder reqSize= new StringBuilder();
            while(type.charAt(j)>='0' && type.charAt(j)<='9'){
                reqSize.append(type.charAt(j));
                j++;
            }
            if(this.value.length()>Integer.parseInt(reqSize.toString()))
                throw new Exception("varchar length condition not respected!");
        }
    }

    public Object parseValue(String type) throws Exception {
        String sqlType = type.toUpperCase();
        if (type.equals("INT")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value '" + value + "' is not a valid INTEGER.");
            }
        }
        else if (type.equals("FLOAT")) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value '" + value + "' is not a valid FLOAT.");
            }
        }
        else if (type.equals("DOUBLE")) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value '" + value + "' is not a valid FLOAT.");
            }
        }
        else if (type.contains("VARCHAR")) {
            return value;
        }

        return value;
    }
}
