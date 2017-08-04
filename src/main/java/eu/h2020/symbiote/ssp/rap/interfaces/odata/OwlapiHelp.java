/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces.odata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class OwlapiHelp {

    private static final Log log = LogFactory.getLog(OwlapiHelp.class);
    
    private static final String CIM_FILE = "/core-v1.0.owl";
    private static final String PIM_FILE = "/pim.owl";
    
    private HashMap<String, HashMap<String, String>> map;
    private HashMap<String, HashMap<String, String>> classes;
    private final HashSet<String> classesStart = new HashSet();
    private final HashSet <String> classesRead = new HashSet();
    private List<OWLOntologyID> allOntology;
    public String filePath;
    
    public OwlapiHelp(){
        this.filePath = OwlapiHelp.class.getResource(PIM_FILE).getPath();
    }
    
    public OwlapiHelp(String filePath){
        this.filePath = filePath;
    }
    
    public HashMap<String, HashMap<String, String>> fromOwlToClasses(){        
        map = createMapClass2PropAndSuperclass();
        classes = new HashMap();
        //this populate this.classes
        for(String key: map.keySet()){
            HashMap<String,String> attribute2type = fromOwlToClassesPrivate(key,map.get(key),map);
        }
        for(String removeClass: classesRead){
            classesStart.remove(removeClass);
        }
        return classes;
    }
    
    public HashSet<String> getSubClassesOfClass(String classStart){
        HashSet<String> subClasses = new HashSet();
            for(String keyClass : map.keySet()){
                HashMap<String, String> mapClass = map.get(keyClass);
                String superClassArrayString = mapClass.get("Superclass");
                String[] superClassArray = superClassArrayString.split(",");
                for(String superClass : superClassArray)
                    if(classStart.equals(superClass)){
                        subClasses.add(keyClass);
                        subClasses.addAll(getSubClassesOfClass(keyClass));
                    }
            }
        return subClasses;
    }
    
    public HashSet<String> getClassesStart(){
        int level = 0;
        return getClassesStart(level);
    }
    public HashSet<String> getClassesStart(int level){
        HashSet<String> classesStartLevel = new HashSet();
        classesStartLevel.addAll(classesStart);       
        if(level > 0)
            classesStartLevel = getClassesStartPrivate(classesStartLevel, level);
        return classesStartLevel;
    }
    public HashSet<String> getClassesStartPrivate(HashSet<String> classesStartLevel, int level){
        HashSet<String> classesStartLevelNew = new HashSet();
        classesStartLevelNew.addAll(classesStartLevel);
        if(level > 0){
            for(String keyClass : map.keySet()){
                HashMap<String, String> mapClass = map.get(keyClass);
                String superClassArrayString = mapClass.get("Superclass");
                String[] superClassArray = superClassArrayString.split(",");
                for(String superClass : superClassArray)
                    if(classesStartLevel.contains(superClass))
                        classesStartLevelNew.add(keyClass);
            }
            classesStartLevelNew = getClassesStartPrivate(classesStartLevelNew,level-1);
        }
        return classesStartLevelNew;
    }
    
    private HashMap<String,String> fromOwlToClassesPrivate(String className, HashMap<String, String> key2value, HashMap<String, HashMap<String, String>> map){
        HashMap<String,String> attribute2type = new HashMap();
        try{
        for(String key: key2value.keySet()){
            //prendere attributi delle superclass
            String value = key2value.get(key);
            if(key.equals("Superclass")){
                String[] superClassArray = value.split(",");
                for(String superClass: superClassArray){
                    if(!superClass.equals(className) && map.containsKey(superClass)){
                        HashMap<String,String> attribute2typeNew;
                        if(classes.containsKey(superClass))
                            attribute2typeNew = classes.get(superClass);
                        else
                            attribute2typeNew = fromOwlToClassesPrivate(superClass,map.get(superClass),map);
                        
                        attribute2type.putAll(attribute2typeNew);
                    }
                }
            }
            //aggiungere attributi e tipo ma rimuovere Specifiche OWL non utilizzate (es. DISJOINTCLASSES)
            else if(! StringUtils.isAllUpperCase(key)){
                attribute2type.put(key, value);
            }
        }
        }catch(Exception e){
            System.err.println(e);
        }
        classes.put(className, attribute2type);
        return attribute2type;
    }
    
    private OWLOntology addOntologyImport(OWLOntology ontology){
        Stream<OWLOntology> owlOntologyStream = ontology.imports();
        Iterator<OWLOntology> owlOntologyiterator = owlOntologyStream.iterator();
        while(owlOntologyiterator.hasNext()){
            OWLOntology ontologyImport = owlOntologyiterator.next();
            if(!allOntology.contains(ontologyImport.getOntologyID())){
                Stream<OWLAxiom> axiomStream = ontologyImport.axioms();
                Iterator<OWLAxiom> axiomIterator = axiomStream.iterator();
                while (axiomIterator.hasNext()) {                        
                    OWLAxiom axiom = axiomIterator.next();
                    ontology.addAxiom(axiom);
                }
                allOntology.add(ontologyImport.getOntologyID());
                addOntologyImport(ontology);
            }
        }
        return ontology;
    }

    
    public HashMap<String, HashMap<String, String>> createMapClass2PropAndSuperclass() {
        HashMap<String, HashMap<String, String>> map = new HashMap();
        File file = new File(filePath);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        allOntology = new ArrayList<>();
        try {
            
            ontology = manager.loadOntologyFromOntologyDocument(file);              
            ontology = addOntologyImport(ontology);
            
            //ADD PROPERTY rdf:type owl:ObjectProperty ;
            HashMap<IRI, HashMap<String, String>> property2domain_range = new HashMap();
            HashMap<String, HashMap<String, String>> domain2property2range = new HashMap();

            Stream<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainStream = ontology.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN);
            Iterator<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainIterator = owlObjectPropertyDomainStream.iterator();
            while (owlObjectPropertyDomainIterator.hasNext()) {
                OWLObjectPropertyDomainAxiom op = owlObjectPropertyDomainIterator.next();
                log.info(op);
                HashMap<String, String> mapDomain = new HashMap();
                String domainName = op.getDomain().toString();
                if (op.getDomain().isOWLClass()) {
                    IRI iriDomain = op.getDomain().asOWLClass().getIRI();
                    domainName = iriDomain.toString();
//                    if(domainName.contains(startOntologyId))
//                        domainName = iriDomain.getShortForm();
                }
                mapDomain.put("Domain", domainName);
                property2domain_range.put(op.getProperty().getNamedProperty().getIRI(), mapDomain);

                HashMap<String, String> mapProperty = new HashMap();
                if (domain2property2range.containsKey(domainName)) {
                    mapProperty = domain2property2range.get(domainName);
                }
                mapProperty.put(op.getProperty().getNamedProperty().getIRI().getShortForm(), "");
                domain2property2range.put(domainName, mapProperty);
            }

            Stream<OWLObjectPropertyRangeAxiom> owlObjectPropertyRangeStream = ontology.axioms(AxiomType.OBJECT_PROPERTY_RANGE);
            Iterator<OWLObjectPropertyRangeAxiom> owlObjectPropertyRangeIterator = owlObjectPropertyRangeStream.iterator();
            while (owlObjectPropertyRangeIterator.hasNext()) {
                OWLObjectPropertyRangeAxiom op = owlObjectPropertyRangeIterator.next();
                log.info(op);

                if (property2domain_range.containsKey(op.getProperty().getNamedProperty().getIRI())) {
                    HashMap<String, String> mapRange = property2domain_range.get(op.getProperty().getNamedProperty().getIRI());

                    String rangeName = op.getRange().toString();
                    if (op.getRange().isOWLClass()) {
                        IRI iriRange = op.getRange().asOWLClass().getIRI();
                        rangeName = iriRange.toString();
//                        if(rangeName.contains(startOntologyId))
//                            rangeName = iriRange.getShortForm();
                        rangeName += "[]";
                    }
                    mapRange.put("Range", rangeName);

                    String domainName = mapRange.get("Domain");
                    HashMap<String, String> mapProperty = domain2property2range.get(domainName);
                    //mapProperty.get(op.getProperty().getNamedProperty().getIRI().getShortForm());
                    mapProperty.put(op.getProperty().getNamedProperty().getIRI().getShortForm(), rangeName);
                }
            }

            Stream<OWLClass> classesStream = ontology.classesInSignature();
            Iterator<OWLClass> classesIterator = classesStream.iterator();
            while (classesIterator.hasNext()) {
                OWLClass c = classesIterator.next();
                String className = c.getIRI().toString();
//                if(className.contains(startOntologyId))
//                    className = c.getIRI().getShortForm();
                HashMap<String, String> prop2type = new HashMap();
                String superclass = "";
                

                log.info(c.getIRI().getShortForm());


                Stream<OWLClassAxiom> owlClassAxiomStream = ontology.axioms(c);
                Iterator<OWLClassAxiom> owlClassAxiomIterator = owlClassAxiomStream.iterator();
                while (owlClassAxiomIterator.hasNext()) {
                    OWLClassAxiom owlClassAxiom = owlClassAxiomIterator.next();
                    log.info("\t" + owlClassAxiom.toString());
                    if (owlClassAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
                        OWLSubClassOfAxiomImpl owlSubClassAxiom = (OWLSubClassOfAxiomImpl) owlClassAxiom;
                        
                        String typeClass = "";
                        String namePro = "";

                        Stream<OWLDataProperty> owlDataPropertyStream = owlClassAxiom.dataPropertiesInSignature();
                        Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                        if (owlDataPropertyOptional.isPresent()) {
                            namePro = owlDataPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLObjectProperty> owlObjectPropertyStream = owlClassAxiom.objectPropertiesInSignature();
                        Optional<OWLObjectProperty> owlObjectPropertyOptional = owlObjectPropertyStream.findFirst();
                        if (owlObjectPropertyOptional.isPresent()) {
                            namePro = owlObjectPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLDatatype> owlDatatypeStream = owlClassAxiom.datatypesInSignature();
                        Optional<OWLDatatype> owlDatatypeOptional = owlDatatypeStream.findFirst();
                        if (owlDatatypeOptional.isPresent()) {
                            typeClass = owlDatatypeOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLClass> owlClassStream = owlClassAxiom.classesInSignature();
                        Iterator<OWLClass> owlClassIterator = owlClassStream.iterator();
                        while (owlClassIterator.hasNext()) {
                            OWLClass owlClass = owlClassIterator.next();
                            if (!owlClass.getIRI().equals(c.getIRI())) {
                                typeClass = owlClass.getIRI().toString();
//                                if(typeClass.contains(startOntologyId))
//                                    typeClass = owlClass.getIRI().getShortForm();
                                break;
                            }
                        }
                        
                        Boolean isArray = true;
                        OWLClassExpression superClassExpression = owlSubClassAxiom.getSuperClass();
                        if(superClassExpression != null){
                            String typeOfSuperClass = superClassExpression.getClass().getName();
                            if(typeOfSuperClass.equals(OWLDataExactCardinalityImpl.class.getName())){
                                OWLDataExactCardinalityImpl exactCardinality = (OWLDataExactCardinalityImpl) superClassExpression;
                                if(exactCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLDataMaxCardinalityImpl.class.getName())){
                                OWLDataMaxCardinalityImpl maxCardinality = (OWLDataMaxCardinalityImpl) superClassExpression;
                                if(maxCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLObjectExactCardinalityImpl.class.getName())){
                                OWLObjectExactCardinalityImpl exactCardinality = (OWLObjectExactCardinalityImpl) superClassExpression;
                                if(exactCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLObjectMaxCardinalityImpl.class.getName())){
                                OWLObjectMaxCardinalityImpl maxCardinality = (OWLObjectMaxCardinalityImpl) superClassExpression;
                                if(maxCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                        }
                        
                        
                        
                        if (!typeClass.isEmpty() && namePro.isEmpty()) {
                            if(superclass.isEmpty())
                                superclass = typeClass;
                            else
                                superclass += ","+typeClass;
                        } else {
                            classesRead.add(typeClass);
                            if(!typeClass.isEmpty() && isArray)
                                typeClass += "[]";
                            prop2type.put(namePro, typeClass);
                        }
                        log.info("\t\t +: " + typeClass + " " + namePro);
                    } else {
                        String typeClass = "";
                        Stream<OWLClass> owlClassStream = owlClassAxiom.classesInSignature();
                        Iterator<OWLClass> owlClassIterator = owlClassStream.iterator();
                        while (owlClassIterator.hasNext()) {
                            OWLClass owlClass = owlClassIterator.next();
                            if (!owlClass.getIRI().equals(c.getIRI())) {
                                typeClass = owlClass.getIRI().toString();
//                                if(typeClass.contains(startOntologyId))
//                                    typeClass = owlClass.getIRI().getShortForm();
                                break;
                            }
                        }
                        prop2type.put(owlClassAxiom.getAxiomType().getName().toUpperCase(), typeClass);
                    }
                }

                if (domain2property2range.containsKey(className)) {
                    HashMap<String, String> property2range = domain2property2range.get(className);
                    for (String property : property2range.keySet()) {
                        if (!prop2type.containsKey(property)) {
                            String range = property2range.get(property);
                            prop2type.put(property, range);
                            log.info("\t\t +: " + range + " " + property);
                            classesRead.add(range.replace("[]", ""));
                        }
                    }

                }
                if(superclass.isEmpty())
                    classesStart.add(className);
                prop2type.put("Superclass", superclass);
                map.put(className, prop2type);
            }
        } catch (OWLOntologyCreationException ex) {
            log.error(ex);
        }
        return map;
    }
    
    

    public HashMap<String, HashMap<String, ArrayList<String>>> test() {
        HashMap<String, HashMap<String, ArrayList<String>>> m = new HashMap();
        String name = OwlapiHelp.class.getResource(CIM_FILE).getPath();
        File file = new File(name);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        OWLDataFactory dataFactory;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(file);
            //reasoner = reasonerFactory.createReasoner(ontology);
            dataFactory = manager.getOWLDataFactory();

            Set<OWLClass> cls;

            cls = ontology.getClassesInSignature();            
            //configurator = new OWLAPIOntologyConfigurator(this);    

            for (OWLClass c : cls) {
                //System.out.println(owlDataProperty.getIRI());

                //System.out.println(owlObjectProperty.getIRI());

                OWLDatatype owlDatatype = dataFactory.getOWLDatatype(c.getIRI());
                log.info(owlDatatype.getIRI());

                Stream<OWLClassAxiom> owlClassAxiomStream = ontology.axioms(c);
                Iterator<OWLClassAxiom> owlClassAxiomIterator = owlClassAxiomStream.iterator();
                while (owlClassAxiomIterator.hasNext()) {
                    OWLClassAxiom owlClassAxiom = owlClassAxiomIterator.next();
                    log.info(owlClassAxiom.toString());
                }

                log.info("\t +PROPERTY: ");
                for (OWLSubClassOfAxiom axiom : ontology.getSubClassAxiomsForSubClass(c)) {
                    OWLClassExpression expression = axiom.getSuperClass();

                    if (expression.isAnonymous()) {
                        String typeClass = "";
                        String namePro = "";
                        Stream<OWLDataProperty> owlDataPropertyStream = expression.dataPropertiesInSignature();
                        Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                        if (owlDataPropertyOptional.isPresent()) {
                            namePro = owlDataPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLObjectProperty> owlObjectPropertyStream = expression.objectPropertiesInSignature();
                        Optional<OWLObjectProperty> owlObjectPropertyOptional = owlObjectPropertyStream.findFirst();
                        if (owlObjectPropertyOptional.isPresent()) {
                            namePro = owlObjectPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLDatatype> owlDatatypeStream = expression.datatypesInSignature();
                        Optional<OWLDatatype> owlDatatypeOptional = owlDatatypeStream.findFirst();
                        if (owlDatatypeOptional.isPresent()) {
                            typeClass = owlDatatypeOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLClass> owlClassStream = expression.classesInSignature();
                        Optional<OWLClass> owlClassOptional = owlClassStream.findFirst();
                        if (owlClassOptional.isPresent()) {
                            typeClass = owlClassOptional.get().getIRI().getShortForm();
                        }
                        log.info("\t\t +: " + typeClass + " " + namePro);
                    }
                }

                Iterator<OWLSubClassOfAxiom> axiomIterator = ontology.subClassAxiomsForSuperClass(c).iterator();
                while (axiomIterator.hasNext()) {
                    OWLSubClassOfAxiom axiom = axiomIterator.next();
                    OWLClassExpression expression = axiom.getSuperClass();
                    log.info("\t +PROPERTY: ");
                    if (expression.isAnonymous()) {
                        String type = "";
                        String namePro = "";
                        for (OWLObjectProperty property : expression.getObjectPropertiesInSignature()) {
                            type = property.toStringID();
                        }
                        for (OWLClass associatedClass : expression.getClassesInSignature()) {
                            namePro = associatedClass.toStringID();
                        }
                        log.info("\t\t +: " + type + " " + namePro);
                    }
                }
            }
            log.info("Classes");
            log.info("--------------------------------");
            for (OWLClass c : cls) {
                HashMap<String, ArrayList<String>> key2values = new HashMap();
                log.info("+: " + c.getIRI().getShortForm());

                ArrayList<String> subClassList = new ArrayList();
                log.info(" \tSubClass");
                for (OWLSubClassOfAxiom subClass : ontology.getSubClassAxiomsForSubClass(c)) {
                    OWLClassExpression levelOne = subClass.getSuperClass();
                    if (levelOne.isOWLClass()) {
                        OWLClass owl_class = levelOne.asOWLClass();
                        log.info("\t\t +: " + owl_class.getIRI().getShortForm());
                        subClassList.add(owl_class.getIRI().getShortForm());
                    }
                }
                key2values.put("SUPERCLASS", subClassList);

                log.info(" \tObject Property");                
                ArrayList<String> objectPropertyDomainList = new ArrayList();
                log.info(" \tObject Property Domain");
                for (OWLObjectPropertyDomainAxiom op : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
                    if (op.getDomain().equals(cls)) {
                        for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
                            log.debug("\t\t +: " + oop.getIRI().getShortForm());
                            objectPropertyDomainList.add(oop.getIRI().getShortForm());
                        }
                        //System.out.println("\t\t +: " + op.getProperty().getNamedProperty().getIRI().getShortForm());
                    }
                }
                key2values.put("OBJECT_PROPERTY_DOMAIN", objectPropertyDomainList);

                ArrayList<String> dataPropertyDomainList = new ArrayList();
                log.debug(" \tData Property Domain");
                for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
                    if (dp.getDomain().equals(cls)) {

                        for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                            System.out.println("\t\t +: " + odp.getIRI().getShortForm());
                            dataPropertyDomainList.add(odp.getIRI().getShortForm());
                        }
                        //System.out.println("\t\t +:" + dp.getProperty());
                    }
                }
                key2values.put("DATA_PROPERTY_DOMAIN", dataPropertyDomainList);

                ArrayList<String> annotationoPropertyDomainList = new ArrayList();
                log.debug(" \tAnnotation Property Domain");
                for (OWLAnnotationPropertyDomainAxiom ap : ontology.getAxioms(AxiomType.ANNOTATION_PROPERTY_DOMAIN)) {
                    if (ap.getDomain().equals(cls)) {

                        for (OWLDataProperty odp : ap.getDataPropertiesInSignature()) {
                            log.debug("\t\t +: " + odp.getIRI().getShortForm());
                            annotationoPropertyDomainList.add(odp.getIRI().getShortForm());
                        }
                        //System.out.println("\t\t +:" + dp.getProperty());
                    }
                }
                key2values.put("ANNOTATION_PROPERTY_DOMAIN", annotationoPropertyDomainList);
                log.debug(" \tFunctional Data Property");
                m.put(c.getIRI().getShortForm(), key2values);
            }
        } catch (OWLOntologyCreationException ex) {
            log.error(ex);
        }
        return m;
    }

}
