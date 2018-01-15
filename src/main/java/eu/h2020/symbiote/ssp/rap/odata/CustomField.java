/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.odata;


/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class CustomField {
    private String type;
    private String name;

    public CustomField(String type, String name) {
        this.type = type;
        this.name = name;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isID(){
        return this.type.equalsIgnoreCase("ID") || this.name.equalsIgnoreCase("ID");
    }
        
    public boolean typeIsPrimitive(){
        return (this.type.equalsIgnoreCase("Literal") || 
                this.type.equalsIgnoreCase("langString") ||
                this.type.equalsIgnoreCase("ID") ||
                this.type.equalsIgnoreCase("string") ||
                this.type.equalsIgnoreCase("boolean") ||
                this.type.equalsIgnoreCase("integer") ||
                this.type.equalsIgnoreCase("int") ||
                this.type.equalsIgnoreCase("double") ||
                this.type.equalsIgnoreCase("decimal") ||
                this.type.equalsIgnoreCase("DateTimeStamp") ||
                this.type.equalsIgnoreCase("DateTime") );
    }
    
    public static boolean typeIsPrimitive(String type){
        return (type.equalsIgnoreCase("Literal") || 
                type.equalsIgnoreCase("langString") ||
                type.equalsIgnoreCase("ID") ||
                type.equalsIgnoreCase("string") ||
                type.equalsIgnoreCase("boolean") ||
                type.equalsIgnoreCase("integer") ||
                type.equalsIgnoreCase("int") ||
                type.equalsIgnoreCase("double") ||
                type.equalsIgnoreCase("decimal") ||
                type.equalsIgnoreCase("DateTimeStamp") ||
                type.equalsIgnoreCase("DateTime") );
    }
    
    public String ToString(){
        return "{Type: "+type+" ,Name: "+name+" }";
    }

    @Override
    public String toString() {
        return ToString();
    }
    
    
}
