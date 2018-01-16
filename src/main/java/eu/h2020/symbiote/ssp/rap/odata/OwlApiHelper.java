/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.odata;

import com.google.common.collect.Multimap;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Capability;
import eu.h2020.symbiote.model.cim.ComplexDatatype;
import eu.h2020.symbiote.model.cim.Datatype;
import eu.h2020.symbiote.model.cim.Device;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.ssp.resources.db.ParameterInfo;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoOData;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoODataRepository;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.manchester.cs.owl.owlapi.OWLDataExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectHasValueImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Component
public class OwlApiHelper {

    private static final Log log = LogFactory.getLog(OwlApiHelper.class);

    private static final String BIM_FILE = "/bim.owl";
    //private static final String PIM_FILE = "/pim.owl";
    private static final String PIM_FILE = "/bim2.owl";
    private static final String PIM_PARTIAL_FILE = "/pim_partial.owl";
    
    @Autowired
    private RegistrationInfoODataRepository infoODataRepo;
    
    private HashMap<String, HashMap<String, String>> map;
    private HashMap<String, HashMap<String, String>> classes;
    private final HashSet<String> classesStart = new HashSet();
    private final HashSet <String> classesRead = new HashSet();
    private List<OWLOntologyID> allOntology;
    private URL ontologyFileURL;
    private boolean addInfoFromDB;
    private boolean addInfoFromRegistration;
    
    private static final String privateUri = "http://www.symbiote-h2020.eu/ontology/pim";

    public OwlApiHelper() throws Exception{
        URL url = OwlApiHelper.class.getResource(PIM_FILE);        
        if(url == null)
            url = OwlApiHelper.class.getResource(BIM_FILE);            
        if(url == null)
            throw new Exception("Not found any pim.owl / bim.owl file");
        
        this.ontologyFileURL = url;
        fromOwlToClasses();
        addInfoFromDB = false;
        addInfoFromRegistration = false;
    }
    
    public boolean haveToRestart(){
        Boolean haveToRestart = addInfoFromRegistration;
        addInfoFromRegistration = false;
        return haveToRestart;
    }
    
    public HashMap<String, HashMap<String, String>> getClasses(){
        if(!addInfoFromDB){
            addRegistrationInfoOdataFromDB();
            try{
                fromMapToClasses();
                addInfoFromDB = true;
            }catch(Exception e){
                log.error(e);
            }
        }
        return this.classes;
    }
    
    public HashMap<String, HashMap<String, String>> getMap(){
        return this.map;
    }
    
    public final HashMap<String, HashMap<String, String>> fromOwlToClasses() throws Exception {     
        map = createMapClass2PropAndSuperclass();
        //infoODataRepo.deleteAll();
        //addRegistrationInfoOdataFromDB();
        fromMapToClasses();
        return classes;
    }
    
    private HashMap<String, HashMap<String, String>> fromMapToClasses() throws Exception { 
        classes = new HashMap();
        //this populate this.classes
        for(String key: map.keySet()){
            HashMap<String,String> attribute2type = fromOwlToClassesPrivate(key,map.get(key),map);
        }
        return classes;
    } 
    
    
    public Boolean addCloudResourceList(List<CloudResource> cloudResourceList){
        Boolean result = false;
        try{
            List<RegistrationInfoOData> registrationInfoOdataList = new ArrayList();
            for(CloudResource cloudResource: cloudResourceList){
                RegistrationInfoOData registrationInfoOdata = saveCloudResourceInDb(cloudResource);
                if(registrationInfoOdata != null)
                    registrationInfoOdataList.add(registrationInfoOdata);
            }
            result = addRegistrationInfoODataList(registrationInfoOdataList);
            if(result){
                fromMapToClasses();
                addInfoFromRegistration = true;
            }
        } catch (Exception ex) {
            log.error(ex);
        }
        return result;
    }
    
    private RegistrationInfoOData saveRegistrationInfoODataInDb(String id, String className, String superClass, List<Parameter> parameters) {
        Set<ParameterInfo> parameterInfoList = new HashSet<>();
        for (Parameter p : parameters) {
            String type = "string";
            Datatype datatype = p.getDatatype();
            if (datatype.getClass().equals(ComplexDatatype.class)) {
                type = ((ComplexDatatype) datatype).getBasedOnClass();
            } else if (datatype.getClass().equals(PrimitiveDatatype.class)) {
                type = ((PrimitiveDatatype) datatype).getBaseDatatype();
            }
            ParameterInfo parameterInfo = new ParameterInfo(type, p.getName(), p.isMandatory());
            parameterInfoList.add(parameterInfo);
        }
        RegistrationInfoOData infoOData = new RegistrationInfoOData(id, className, superClass, parameterInfoList);
        RegistrationInfoOData infoODataNew = infoODataRepo.insertNew(infoOData);
        return infoODataNew;
    }

    private RegistrationInfoOData saveCloudResourceInDb(CloudResource cloudResource) {
        RegistrationInfoOData result = null;
        Resource r = cloudResource.getResource();
        List<Parameter> parameters;
        if (r.getClass().equals(Actuator.class)) {
            Actuator actuator = (Actuator) r;
            for (Capability capability : actuator.getCapabilities()) {
                parameters = capability.getParameters();
                //String className = "GenericCapability";
                //String superClass = Capability.class.getSimpleName();
                String className = Capability.class.getSimpleName();
                String superClass = null;
                result = saveRegistrationInfoODataInDb(r.getId(), className, superClass, parameters);
            }
        } else if (r.getClass().equals(Device.class)) {
            Device device = (Device) r;
            for (Service service : device.getServices()) {
                parameters = service.getParameters();
                //String className = "GenericService";
                //String superClass = Service.class.getSimpleName();
                String className = Service.class.getSimpleName();
                String superClass = null;
                result = saveRegistrationInfoODataInDb(r.getId(), className, superClass, parameters);
            }
        } else if (r.getClass().equals(Service.class)) {
            Service service = (Service) r;
            parameters = service.getParameters();
            String className = Service.class.getSimpleName();
            String superClass = null;
            result = saveRegistrationInfoODataInDb(r.getId(), className, superClass, parameters);
        }
        return result;
    }
    
    private Boolean addRegistrationInfoOdataFromDB(){
        List<RegistrationInfoOData> registrationInfoODataList = infoODataRepo.findAll();
        return addRegistrationInfoODataList(registrationInfoODataList);
    }
    
    private Boolean addRegistrationInfoODataList(List<RegistrationInfoOData> registrationInfoODataList){
        Boolean add = false;
        try {
            for(RegistrationInfoOData regInfo: registrationInfoODataList){
                String superClassRegInfo = regInfo.getSuperClass();
                String superClassCompelete = "";
                if(superClassRegInfo != null && !superClassRegInfo.isEmpty()){
                    superClassCompelete = getClassLongName(regInfo.getSuperClass(), map.keySet());
                    if(superClassCompelete == null){
                        superClassCompelete = privateUri + "#" + superClassRegInfo;
                    }
                }
                
                String classNameRegInfo = regInfo.getClassName();
                String classNameComplete = getClassLongName(classNameRegInfo,map.keySet());
                if(classNameComplete == null){
                    classNameComplete = privateUri + "#" + classNameRegInfo;
                    HashMap<String, String> class2type = new HashMap<>();
                    class2type.put("Superclass", superClassCompelete);
                    for(CustomField cf: regInfo.getParametersComplete()){
                        class2type.put(cf.getName(), cf.getType());
                    }
                    map.put(classNameComplete, class2type);
                }
                else{
                    HashMap<String, String> class2type = map.get(classNameComplete);
                    if(superClassCompelete != null && !superClassCompelete.isEmpty()){
                        String superClass = "";
                        if(class2type.containsKey("Superclass"))
                            superClass = class2type.get("Superclass");
                        if(superClass != null && !superClass.isEmpty())
                            superClass += ",";
                        superClass += superClassCompelete;
                        class2type.put("Superclass", superClass);
                    }
                    for(CustomField cf: regInfo.getParametersComplete()){
                        class2type.put(cf.getName(), cf.getType());
                    }
                }
            }
            add = true;
        } catch (Exception ex) {
            log.error(ex);
        }
        return add;
    }
    
    
    private String getClassLongName(String simpleName, Set<String> fatherList) {
        String classLongName = null;
        Optional<String> classLongNameOp = fatherList.stream().filter(str -> getShortClassName(str).equals(simpleName)).findFirst();
        if (classLongNameOp.isPresent()) {
            classLongName = classLongNameOp.get();
        }
        return classLongName;
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
    
    
    
    public HashSet<String> getSubClassesOfClass(String classStart){
        HashSet<String> subClasses = new HashSet<String>();
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
    
    
    private HashMap<String,String> fromOwlToClassesPrivate(String className, HashMap<String, String> key2value, HashMap<String, HashMap<String, String>> map){
        HashMap<String,String> attribute2type = new HashMap<String,String>();
        try{
            for(String key: key2value.keySet()){
                //prendere attributi delle superclass
                String value = key2value.get(key);
                if(key.equals("Superclass")){
                    String[] superClassArray = value.split(",");
                    for(String superClass: superClassArray){
                        if(!superClass.isEmpty() && !superClass.equals(className) && map.containsKey(superClass)){
                            HashMap<String,String> attribute2typeNew;
                            if(classes.containsKey(superClass))
                                attribute2typeNew = classes.get(superClass);
                            else
                                attribute2typeNew = fromOwlToClassesPrivate(superClass,map.get(superClass),map);

                            attribute2type.putAll(attribute2typeNew);
                        }
                    }
                }
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
            OWLOntologyID ontologyId = ontologyImport.getOntologyID();
            if(!allOntology.contains(ontologyId)){
                log.info("Add ontology: "+ontologyId);
                Stream<OWLAxiom> axiomStream = ontologyImport.axioms();
                Iterator<OWLAxiom> axiomIterator = axiomStream.iterator();
                while (axiomIterator.hasNext()) {                        
                    OWLAxiom axiom = axiomIterator.next();
                    ontology.addAxiom(axiom);
                }
                allOntology.add(ontologyImport.getOntologyID());
                addOntologyImport(ontologyImport);
            }
        }
        return ontology;
    }

    
    private HashMap<String, HashMap<String, String>> createMapClass2PropAndSuperclass() throws Exception {
        HashMap<String, HashMap<String, String>> localMap = new HashMap();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        allOntology = new ArrayList<>();
        try {
            log.debug("Reading ontology from file: " + ontologyFileURL.getFile());
            InputStream is = ontologyFileURL.openStream();
            ontology = manager.loadOntologyFromOntologyDocument(is);
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
                            //TAKE owl:Restriction 
                            else if(typeOfSuperClass.equals(OWLObjectHasValueImpl.class.getName())){
                                OWLObjectHasValueImpl owlObjectHasValueImpl = (OWLObjectHasValueImpl) superClassExpression;
                                OWLIndividual individual = owlObjectHasValueImpl.getFiller();
                                
                                Multimap<OWLDataPropertyExpression,OWLLiteral> exprLiteral = EntitySearcher.getDataPropertyValues(individual, ontology);
                                for(OWLDataPropertyExpression exp : exprLiteral.keys()){
                                    IRI iriDataPropertyImpl = null;
                                    try{
                                        OWLDataPropertyImpl dataPropertyImpl = (OWLDataPropertyImpl) exp;
                                        iriDataPropertyImpl = dataPropertyImpl.getIRI();
                                    }catch(Exception e){}
                                    
                                    if(iriDataPropertyImpl != null && iriDataPropertyImpl.getShortForm().contains("name") ){
                                        Collection<OWLLiteral> literalC = exprLiteral.get(exp);
                                        for(OWLLiteral literal : literalC){
                                            namePro = literal.getLiteral();
                                        }
                                    }
                                }
                                
                                Multimap<OWLObjectPropertyExpression,OWLIndividual> exprIndividual = EntitySearcher.getObjectPropertyValues(individual, ontology);
                                for(OWLObjectPropertyExpression exp : exprIndividual.keys()){
                                    IRI iriObjectPropertyImpl = null;
                                    try{
                                        OWLObjectPropertyImpl objectPropertyImpl = (OWLObjectPropertyImpl) exp;
                                        iriObjectPropertyImpl = objectPropertyImpl.getIRI();
                                    }catch(Exception e){}                                   
                                    
                                    if(iriObjectPropertyImpl != null && iriObjectPropertyImpl.getShortForm().contains("hasDatatype") && !namePro.isEmpty()){
                                        Collection<OWLIndividual> individualC = exprIndividual.get(exp);
                                        for(OWLIndividual individual0 : individualC){
                                            try{
                                                OWLNamedIndividualImpl namedIndividualImpl = (OWLNamedIndividualImpl) individual0;
                                                typeClass = namedIndividualImpl.getIRI().getShortForm();
                                                isArray = false;
                                            }catch(Exception e){}
                                        }
                                    }
                                }
                            }
                        }                        
                        
                        if(!typeClass.isEmpty()){
                            if (namePro.isEmpty()) {
                                if(superclass.isEmpty())
                                    superclass = typeClass;
                                else
                                    superclass += ","+typeClass;
                            } else {
                                classesRead.add(typeClass);
                                if(isArray)
                                    typeClass += "[]";
                                prop2type.put(namePro, typeClass);
                            }
                            log.info("\t\t +: " + typeClass + " " + namePro);
                        }
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
                localMap.put(className, prop2type);
            }
        } catch (OWLOntologyCreationException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
        catch (UnloadableImportException ie){
            log.error(ie);
            URL url = OwlApiHelper.class.getResource(PIM_PARTIAL_FILE);
            String filePathPartial = url.getPath();
            if(ie.getMessage().contains("<http://purl.org/dc/terms/>") && !this.ontologyFileURL.equals(filePathPartial)){
                this.ontologyFileURL = url;
                localMap = createMapClass2PropAndSuperclass();
                log.info("Load pim partial ");
            }
        }
        return localMap;
    }    
  
}