/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces.odata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.cloud.model.resources.Sensor;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.resources.Actuator;
import eu.h2020.symbiote.cloud.model.resources.ActuatingService;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;

import org.semanticweb.owlapi.model.IRI;

/**
 * this class is supposed to declare the metadata of the OData service it is
 * invoked by the Olingo framework e.g. when the metadata document of the
 * service is invoked e.g.
 * http://localhost:8080/ExampleService1/ExampleService1.svc/$metadata
 *
 * @author Matteo Pardi m.pardi@nextworks.it
 */
@Component
public class RAPEdmProvider extends CsdlAbstractEdmProvider {

    private static final Logger log = LoggerFactory.getLogger(RAPEdmProvider.class);

    // Service Namespace
    public static final String NAMESPACE = "OData.Model";
    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Type Names
//    public static final String ET_OBSERVATION_NAME = Observation.class.getSimpleName();
//    public static final FullQualifiedName ET_OBSERVATION_FQN = new FullQualifiedName(NAMESPACE, ET_OBSERVATION_NAME);    
//    public static final String ET_SENSOR_NAME = Sensor.class.getSimpleName();
//    public static final FullQualifiedName ET_SENSOR_FQN = new FullQualifiedName(NAMESPACE, ET_SENSOR_NAME);
//    public static final String ET_ACTUATOR_NAME = Actuator.class.getSimpleName();
//    public static final FullQualifiedName ET_ACTUATOR_FQN = new FullQualifiedName(NAMESPACE, ET_ACTUATOR_NAME);
//    public static final String ET_SERVICE_NAME = ActuatingService.class.getSimpleName();
//    public static final FullQualifiedName ET_SERVICE_FQN = new FullQualifiedName(NAMESPACE, ET_SERVICE_NAME);
//    // Entity Set Names
//    public static final String ES_OBSERVATIONS_NAME =  Observation.class.getSimpleName() + "s";
//    public static final String ES_SENSORS_NAME = Sensor.class.getSimpleName() + "s";    
//    public static final String ES_ACTUATORS_NAME =  Actuator.class.getSimpleName() + "s";
//    public static final String ES_SERVICES_NAME = ActuatingService.class.getSimpleName() + "s";    
    private OwlapiHelp owlApiHelp = null;
    private HashMap<String, HashMap<String, String>> classes;
    private HashSet<String> classesStart;
    private HashMap<String, List<CustomField>> class2field = new HashMap<String, List<CustomField>>();

    public void initialize() {
        int level = 1;
        owlApiHelp = new OwlapiHelp();
        classes = owlApiHelp.fromOwlToClasses();
        classesStart = owlApiHelp.getClassesStart(level);
    }

    public HashSet<String> getClassesStart() {
        //if(classesStart == null){
        //initialize();
        //}
        //return classesStart;
        Set<String> setKey = getClasses().keySet();
        HashSet<String> classes = new HashSet<String>();
        classes.addAll(setKey);
        return classes;
    }

    public HashMap<String, HashMap<String, String>> getClasses() {
        if (classes == null) {
            initialize();
        }
        return classes;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        try {
            // create Schema
            CsdlSchema schema = new CsdlSchema();
            schema.setNamespace(NAMESPACE);

            // add EntityTypes
            List<CsdlEntityType> entityTypes = new ArrayList();
            for (String s : getClassesStart()) {
                IRI iri = IRI.create(s);
                entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, iri.getShortForm())));
            }
            schema.setEntityTypes(entityTypes);

            //add ComplexTypes
            List<CsdlComplexType> complexTypes = new ArrayList();
            Map<String, Class> parentClasses = new HashMap();
            for (String iriString : getClassesStart()) {
                List<CustomField> fields = getAllFields(iriString);
                for (CustomField f : fields) {
                    if (f.typeIsPrimitive()) {
                        continue;
                    }
                    complexTypes.add(getComplexType(new FullQualifiedName(NAMESPACE, getShortClassName(f.getType()))));
                }
            }
            schema.setComplexTypes(complexTypes);
            // add EntityContainer
            schema.setEntityContainer(getEntityContainer());
            // finally
            List<CsdlSchema> schemas = new ArrayList();
            schemas.add(schema);

            return schemas;
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(RAPEdmProvider.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private List<CustomField> getAllFieldsOld(String iriString) {
        List<CustomField> fields = new ArrayList();
        HashMap<String, String> property2type = getClasses().get(iriString);
        if (property2type != null) {
            for (String property : property2type.keySet()) {
                String type = property2type.get(property);
                CustomField cf = new CustomField(type, property);
                fields.add(cf);
            }
        }
        return fields;
    }

    private List<CustomField> getAllFields(String iriString) {
        if (class2field.containsKey(iriString)) {
            return class2field.get(iriString);
        }
        List<CustomField> fields = new ArrayList();
        HashMap<String, String> property2type = getClasses().get(iriString);
        if (property2type != null) {
            for (String property : property2type.keySet()) {
                String type = property2type.get(property);
                if (!CustomField.typeIsPrimitive(getShortClassName(type))) {
                    HashSet<String> subclasses = owlApiHelp.getSubClassesOfClass(type.replace("[]", ""));
                    if (subclasses != null && !subclasses.isEmpty()) {
                        for (String subclass : subclasses) {
                            if (type.contains("[]")) {
                                subclass += "[]";
                            }
                            CustomField cf = new CustomField(subclass, property);
                            fields.add(cf);
                        }
                    }
                }
                CustomField cf = new CustomField(type, property);
                fields.add(cf);
            }
        }
        class2field.put(iriString, fields);
        return fields;
    }

    private String getInternalTypeClass(CustomField f) {
        return getShortClassName(f.getType());
    }

    private String getInternalTypeClassLong(CustomField f) {
        return f.getType().replace("[]", "");
    }

    private String getClassLongName(String simpleName, HashSet<String> fatherList) {
        String classLongName = null;
        Optional<String> classLongNameOp = fatherList.stream().filter(str -> getShortClassName(str).equals(simpleName)).findFirst();
        if (classLongNameOp.isPresent()) {
            classLongName = classLongNameOp.get();
        }
        return classLongName;

        /*String className = null;
        for(String c : fatherList) {
            className = getClassLongName(simpleName, c);
            if(className != null)
                break;
        }
        return className;*/
    }

    private String getClassLongName(String simpleName, String father) {
        try {
            String name = null;
            if (getShortClassName(father).equals(simpleName)) {
                name = father;
                return name;
            }

            List<CustomField> fields = getAllFields(father);
            for (CustomField f : fields) {
                String cl = f.getType();
                if (cl.contains("[]")) {
                    cl = getInternalTypeClassLong(f);
                }
                if ((cl != null) && (getShortClassName(cl).equals(simpleName))) {
                    name = cl;
                    break;
                }
            }
            if (name == null) {
                for (CustomField f : fields) {
                    String cl = f.getType();
                    if (cl.contains("[]")) {
                        cl = getInternalTypeClassLong(f);
                        if (!CustomField.typeIsPrimitive(getShortClassName(cl))) {
                            name = getClassLongName(simpleName, cl);
                        }
                    } else {
                        if (!CustomField.typeIsPrimitive(cl)) {
                            name = getClassLongName(simpleName, f.getType());
                        }
                    }
                    if (name != null) {
                        break;
                    }
                }
            }

            return name;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
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

    private FullQualifiedName getFullQualifiedName(String type) {
        FullQualifiedName fqn = null;
        String genericType = type;

        if (genericType.compareToIgnoreCase("String") == 0
                || genericType.compareToIgnoreCase("langString") == 0
                || genericType.compareToIgnoreCase("Literal") == 0
                || genericType.compareToIgnoreCase("ID") == 0) {
            fqn = EdmPrimitiveTypeKind.String.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("Double") == 0) {
            fqn = EdmPrimitiveTypeKind.Double.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("Long") == 0) {
            fqn = EdmPrimitiveTypeKind.Int64.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("integer") == 0) {
            fqn = EdmPrimitiveTypeKind.Int32.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("Boolean") == 0) {
            fqn = EdmPrimitiveTypeKind.Boolean.getFullQualifiedName();
        }

        return fqn;
    }

    private String getContainerClass(String type, HashSet<String> fatherList) {
        String cl = null;
        for (String c : fatherList) {
            cl = getContainerClass(type, c);
            if (cl != null) {
                break;
            }
        }
        return cl;
    }

    private String getContainerClass(String type, String root) {
        String containingClass = null;

        List<CustomField> fields = getAllFields(root);
        for (CustomField f : fields) {
            String cl = f.getType();
            if (cl.contains("[]")) {
                cl = getInternalTypeClass(f);
            }
            if ((cl != null) && (cl.equalsIgnoreCase(type))) {
                containingClass = root;
                break;
            }
        }
        if (containingClass == null) {
            for (CustomField f : fields) {
                String cl = f.getType();
                if (cl.contains("[]")) {
                    containingClass = getContainerClass(type, getInternalTypeClass(f));
                } else {
                    if (f.typeIsPrimitive()) {
                        containingClass = getContainerClass(type, f.getType());
                    }
                }
                if (containingClass != null) {
                    break;
                }
            }
        }

        return containingClass;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {

        CsdlEntityType entityType = null;
        //create EntityType properties
        try {
            //Class [] classes = {Sensor.class, Actuator.class, ActuatingService.class};
            String className = getClassLongName(entityTypeName.getName(), getClassesStart());
            List<CustomField> fields = getAllFields(className);
            ArrayList<CsdlProperty> lst = new ArrayList();
            List<CsdlNavigationProperty> navPropList = new ArrayList<>();
            String keyEl = "";
            for (CustomField f : fields) {
                String cl;
                boolean isList = false;
                if (f.getType().contains("[]")) {
                    cl = getInternalTypeClass(f);
                    // adding navigation for this collection
                    CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                            .setName(cl + "s")
                            .setType(new FullQualifiedName(NAMESPACE, cl))
                            .setCollection(true)
                            .setPartner(entityTypeName.getName());
                    navPropList.add(navProp);
                    isList = true;
                    log.info("List type -> Name: " + f.getName() + ", Type: " + cl);
                } else {
                    cl = f.getType();
                }
                String shortName = getShortClassName(cl);
                if (CustomField.typeIsPrimitive(cl)) {
                    FullQualifiedName fqn = getFullQualifiedName(cl);
                    CsdlProperty propId = new CsdlProperty()
                            .setName(f.getName())
                            .setType(fqn);
                    lst.add(propId);
                    log.info("Primitive type: " + f.getName() + " - " + fqn.getFullQualifiedNameAsString());
                } else {
                    CsdlProperty propId = new CsdlProperty()
                            .setName(f.getName())
                            //.setName(shortName + "s")
                            .setType(new FullQualifiedName(NAMESPACE, shortName));
                    if (isList) {
                        propId.setCollection(true);
                    }
                    lst.add(propId);
                    log.info("Complex type: " + f.getName() + " - " + shortName);
                }

                if (f.isID()) {
                    String nm = f.getName();
                    keyEl = nm;
                }
            }
            CsdlPropertyRef propertyRef = null;
            if (keyEl.length() > 0) {
                // create CsdlPropertyRef for Key element
                propertyRef = new CsdlPropertyRef();
                propertyRef.setName(keyEl);
            }
            // configure EntityType
            entityType = new CsdlEntityType();
            entityType.setName(entityTypeName.getName());
            entityType.setProperties(lst);
            if (propertyRef != null) {
                entityType.setKey(Collections.singletonList(propertyRef));
            }

            String fath = getContainerClass(className, getClassesStart());
            if (fath != null) {
                CsdlNavigationProperty navProp1 = new CsdlNavigationProperty()
                        .setName(getShortClassName(fath))
                        .setType(new FullQualifiedName(NAMESPACE, getShortClassName(fath)))
                        .setNullable(true)
                        .setPartner(entityTypeName.getName() + "s");
                navPropList.add(navProp1);
            }
            //////////////////////////////////////////////////////////////////////
            entityType.setNavigationProperties(navPropList);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return entityType;
    }

    @Override
    public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) throws ODataException {

        try {
            String className = getClassLongName(complexTypeName.getName(), getClassesStart());
            List<CustomField> fields = getAllFields(className);
            ArrayList<CsdlProperty> propList = new ArrayList();
            for (CustomField f : fields) {
                String name = f.getName();
                if (f.typeIsPrimitive()) {
                    FullQualifiedName fqn = getFullQualifiedName(f.getType());
                    CsdlProperty prop = new CsdlProperty()
                            .setName(name)
                            .setType(fqn);
                    propList.add(prop);
                } else {
                    String shortName = getShortClassName(f.getType());
                    CsdlProperty prop = new CsdlProperty()
                            .setName(name)
                            .setType(new FullQualifiedName(NAMESPACE, shortName));
                    propList.add(prop);
                }
            }
            CsdlComplexType cpx = new CsdlComplexType()
                    .setName(complexTypeName.getName())
                    .setProperties(propList);

            return cpx;
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(RAPEdmProvider.class.getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    //DA RIFARE
    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {

        if (entityContainer.equals(CONTAINER)) {
            HashSet<String> classess = getClassesStart();
            Optional<String> classLongName = classess.stream().filter(str -> (getShortClassName(str) + "s").equalsIgnoreCase(entitySetName)).findFirst();
            if (classLongName.isPresent()) {
                String s = classLongName.get();
                String simpleName = getShortClassName(s);
                String simpleNames = simpleName + "s";
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(simpleNames);
                entitySet.setType(new FullQualifiedName(NAMESPACE, simpleName));

                List<CustomField> fields = getAllFields(s);
                for (CustomField f : fields) {
                    String type = f.getType();
                    if (type.contains("[]") && getClassesStart().contains(type.replace("[]", ""))) {
                        String typeSimpleName = getShortClassName(type) + "s";
                        //String typeSimpleName = f.getName() + "s";
                        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                        navPropBinding.setTarget(typeSimpleName);//target entitySet, where the nav prop points to
                        navPropBinding.setPath(typeSimpleName); // the path from entity type to navigation property
                        List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList();
                        navPropBindingList.add(navPropBinding);
                        entitySet.setNavigationPropertyBindings(navPropBindingList);
                    }
                }
                return entitySet;
            }
        }

        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {

        // create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList();
        for (String s : getClassesStart()) {
            entitySets.add(getEntitySet(CONTAINER, getShortClassName(s) + "s"));
        }
        // create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {

        // This method is invoked when displaying the service document at e.g. http://localhost:8080/DemoService/DemoService.svc
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }

        return null;
    }
}
