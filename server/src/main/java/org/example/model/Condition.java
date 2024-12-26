package org.example.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public boolean checkCondition(String s, String type) throws Exception {
        ///Incompleta, m-am blocat in idei
        Object elem1;
        Object elem2;

        if(type.equals("INT")) {
            elem1 = Integer.parseInt(s);
            elem2 = Integer.parseInt(this.value);

        }
        if(type.equals("FLOAT")){
            elem1 = Float.parseFloat(s);
            elem2 = Float.parseFloat(this.value);
        }
        if(type.equals("DOUBLE")){
            elem1 = Double.parseDouble(s);
            elem2 = Double.parseDouble(this.value);
        }
        else{
            elem1 = s;
            elem2 = this.value;
        }

        if(this.sign.equals("LIKE"))
            if(elem2 instanceof String) {
                Pattern pattern = Pattern.compile((String) elem2);
                Matcher matcher = pattern.matcher((String)elem1);
                return matcher.find();
            } else{
                throw new Exception("Error! "+elem2.toString()+" cannot be used for a pattern");
            }
        return true;
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
            String reqSize="";
            while(type.charAt(j)>='0' && type.charAt(j)<='9'){
                reqSize+=type.charAt(j);
                j++;
            }
            if(this.value.length()>Integer.parseInt(reqSize))
                throw new Exception("varchar length condition not respected!");
        }
    }
}
