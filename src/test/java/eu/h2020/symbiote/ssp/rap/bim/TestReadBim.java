package eu.h2020.symbiote.resources.bim;


import eu.h2020.symbiote.bim.OwlapiHelp;
import eu.h2020.symbiote.service.CustomField;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.semanticweb.owlapi.model.IRI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestConfiguration
public class TestReadBim {
    
    @Autowired
    private OwlapiHelp owlApiHelp;
    
    @Test
    public void readBim(){
        //map
        HashMap<String,HashMap<String,String>> owlMap = owlApiHelp.getMap();
        assert(owlMap != null);
        assert(!owlMap.isEmpty());
        String entityName = "Actuator";
        Optional<String> classLongName = owlMap.keySet().stream().filter(str -> (getShortClassName(str)).equalsIgnoreCase(entityName)).findFirst();
        assert(classLongName.isPresent());
        HashMap<String,String> mapOfEntityName = owlMap.get(classLongName.get());
        assert(mapOfEntityName != null);
        assert(!mapOfEntityName.isEmpty());
        String superclass = "Superclass";
        assert(mapOfEntityName.containsKey(superclass));
        String entityNameSuperClass = "Device";
        String entityNameSuperClassLongName = mapOfEntityName.get(superclass);
        assert(getShortClassName(entityNameSuperClassLongName).equals(entityNameSuperClass));
        //classes
        HashMap<String,HashMap<String,String>> owlClasses = owlApiHelp.getClasses();
        assert(owlClasses != null);
        assert(!owlClasses.isEmpty());
        HashMap<String,String> classOfActuator = owlClasses.get(classLongName.get());
        assert(classOfActuator != null);
        assert(!classOfActuator.isEmpty());
        HashMap<String,String> classOfDevice = owlClasses.get(entityNameSuperClassLongName);
        assert(classOfDevice != null);
        assert(!classOfDevice.isEmpty());
        assert(classOfDevice.containsKey("id"));
        for(String key: classOfDevice.keySet()){
            assert(classOfActuator.containsKey(key));
            assert(classOfActuator.get(key).equals(classOfDevice.get(key)));
        }
    }
    
    private String getShortClassName(String type) {
        String simpleName = type.replace("[]", "");
        if (!CustomField.typeIsPrimitive(simpleName)) {
            IRI iri = IRI.create(simpleName);
            simpleName = iri.getShortForm();
            if (simpleName.contains("#")) {
                String[] array = simpleName.split("#");
                simpleName = array[array.length - 1];
            }
        }
        return simpleName;
    }
}
