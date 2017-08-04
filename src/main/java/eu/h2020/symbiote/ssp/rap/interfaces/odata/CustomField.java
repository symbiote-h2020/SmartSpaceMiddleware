/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces.odata;

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
        return this.type.equalsIgnoreCase("ID");
    }
        
    public boolean typeIsPrimitive(){
        return (this.type.equalsIgnoreCase("Literal") || 
                this.type.equalsIgnoreCase("langString") ||
                this.type.equalsIgnoreCase("ID") ||
                this.type.equalsIgnoreCase("string") ||
                this.type.equalsIgnoreCase("boolean") ||
                this.type.equalsIgnoreCase("integer") ||
                this.type.equalsIgnoreCase("double") );
    }
    
    public static boolean typeIsPrimitive(String type){
        return (type.equalsIgnoreCase("Literal") || 
                type.equalsIgnoreCase("langString") ||
                type.equalsIgnoreCase("ID") ||
                type.equalsIgnoreCase("string") ||
                type.equalsIgnoreCase("boolean") ||
                type.equalsIgnoreCase("integer") ||
                type.equalsIgnoreCase("double") );
    }
    
    public String ToString(){
        return "{Type: "+type+" ,Name: "+name+" }";
    }
}
