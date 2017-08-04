/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces.odata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.ssp.rap.resources.RequestInfo;
import eu.h2020.symbiote.ssp.rap.resources.PlatformInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import eu.h2020.symbiote.ssp.rap.resources.PlatformRepository;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class StorageHelper {

    private final int TOP_LIMIT = 100;

    private final ResourcesRepository resourcesRepo;
    private final PlatformRepository pluginRepo;
    
    private static final Pattern PATTERN = Pattern.compile(
            "\\p{Digit}{1,4}-\\p{Digit}{1,2}-\\p{Digit}{1,2}"
            + "T\\p{Digit}{1,2}:\\p{Digit}{1,2}(?::\\p{Digit}{1,2})?"
            + "(Z|([-+]\\p{Digit}{1,2}:\\p{Digit}{2}))?");

    public StorageHelper(ResourcesRepository resourcesRepository, PlatformRepository pluginRepository) {
        //initSampleData();
        resourcesRepo = resourcesRepository;
        pluginRepo = pluginRepository;
    }

    public ResourceInfo getResourceInfo(List<UriParameter> keyParams) {
        ResourceInfo resInfo = null;
        if(keyParams != null && !keyParams.isEmpty()){
            final UriParameter key = keyParams.get(0);
            String keyName = key.getName();
            String keyText = key.getText();
            //remove quote
            keyText = keyText.replaceAll("'", "");
            try {
                if (keyName.equalsIgnoreCase("id")) {
                    Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(keyText);
                    if (resInfoOptional.isPresent()) {
                        resInfo = resInfoOptional.get();
                    }
                }
            } catch (Exception e) {
                int a = 0;
            }
        }

        //SOLO MOMENTANEO
        //if (resInfo == null) {
            //List<ResourceInfo> resInfo2 = resourcesRepo.findAll();
            //resInfo = resInfo2.get(0);
        //}
        return resInfo;
    }

    public Object getRelatedObject(ResourceInfo resourceInfo, Integer top, ArrayList<RequestInfo> requestInfoList) throws ODataApplicationException {
        try {
            top = (top == null) ? TOP_LIMIT : top;

//            ResourceAccessMessage msg;
            String pluginId = resourceInfo.getPlatformId();
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPlatformId();
            }
            return null;
            /*
            String routingKey;
            if (top == 1) {
                msg = new ResourceAccessGetMessage(resourceInfo,requestInfoList);
                routingKey =  pluginId + "." + ResourceAccessMessage.AccessType.GET.toString().toLowerCase();
                
            } else {
                msg = new ResourceAccessHistoryMessage(resourceInfo, top, filterQuery,requestInfoList);
                routingKey =  pluginId + "." + ResourceAccessMessage.AccessType.HISTORY.toString().toLowerCase();
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            
           Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            if (obj == null) {
                throw new ODataApplicationException("No response from plugin", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
            }

            String response;
            if (obj instanceof byte[]) {
                response = new String((byte[]) obj, "UTF-8");
            } else {
                response = (String) obj;
            }
            List<Observation> observations = mapper.readValue(response, new TypeReference<List<Observation>>() {
            });
            if (observations == null || observations.isEmpty()) {
                return null;
            }

            if (top == 1) {
                Observation o = observations.get(0);
                Observation ob = new Observation(resourceInfo.getSymbioteId(), o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                return ob;
            } else {
                List<Observation> observationsList = new ArrayList();
                for (Observation o : observations) {
                    Observation ob = new Observation(resourceInfo.getSymbioteId(), o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                    observationsList.add(ob);
                }
                return observationsList;
            }*/

        } catch (Exception e) {
            String err = "Unable to read resource with id: " + resourceInfo.getSymbioteId();
            err += "\n Error:" + e.getMessage();
            //log.error(err + "\n" + e.getMessage());
            throw new ODataApplicationException(err, HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }
    }

    public void setService(ResourceInfo resourceInfo, Entity requestBody, ArrayList<RequestInfo> requestInfoList) throws ODataApplicationException {
        try {
            List<InputParameter> inputParameterList = new ArrayList();
//            ResourceAccessMessage msg;
            String pluginId = resourceInfo.getPlatformId();
            if(pluginId == null) {
                List<PlatformInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPlatformId();
            }
//            String routingKey = pluginId + "." + ResourceAccessMessage.AccessType.SET.toString().toLowerCase();

            Property updateProperty = requestBody.getProperty("inputParameters");
            if (updateProperty != null && updateProperty.isCollection()) {
                List<ComplexValue> name_value = (List<ComplexValue>) updateProperty.asCollection();
                for (ComplexValue complexValue : name_value) {
                    List<Property> properties = complexValue.getValue();
                    String name = null;
                    String value = null;
                    for (Property p : properties) {
                        String pName = p.getName();
                        if (pName.equals("name")) {
                            name = (String) p.getValue();
                        } else if (pName.equals("value")) {
                            value = (String) p.getValue();
                        }
                    }
                    InputParameter ip = new InputParameter(name);
                    ip.setValue(value);
                    inputParameterList.add(ip);
                }
            }
            /*
            List<Property> updatePropertyList = requestBody.getProperties();
            inputParameterList = fromPropertiesToInputParameter(updatePropertyList,inputParameterList);
            
            msg = new ResourceAccessSetMessage(resourceInfo, inputParameterList,requestInfoList);

            String json = "";
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

                json = mapper.writeValueAsString(msg);
            } catch (JsonProcessingException ex) {
                Logger.getLogger(StorageHelper.class.getName()).log(Level.SEVERE, null, ex);
            }

            rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            */
        } catch (Exception e) {
            throw new ODataApplicationException("Internal Error", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
        }
    }
    
    private List<InputParameter> fromPropertiesToInputParameter(List<Property> propertyList, List<InputParameter> inputParameter){
        List<InputParameter> inputParameterNew = new ArrayList();
        inputParameterNew.addAll(inputParameter);
        for(Property p : propertyList){
                if(p.isCollection()){
                    List<ComplexValue> name_value = (List<ComplexValue>) p.asCollection();
                    for (ComplexValue complexValue : name_value) {
                        List<Property> properties = complexValue.getValue();
                        List<InputParameter> inputParameterAdd = fromPropertiesToInputParameter(properties,inputParameterNew);
                        inputParameterNew.addAll(inputParameterAdd);
                    }
                }
                    else{
                String name = p.getName();
                String value = p.getValue().toString();
                InputParameter ip = new InputParameter(name);
                ip.setValue(value);
                inputParameterNew.add(ip);
                            }
            }
        return inputParameterNew;
    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        return getElement(edmEntityType, keyParams);
    }

    private Entity getElement(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException {
        // the list of entities at runtime
        EntityCollection entitySet;
//        if (edmEntityType.getName().equals(RAPEdmProvider.ET_SENSOR_NAME)) {
//            entitySet = getResources();
//        } else if (edmEntityType.getName().equals(RAPEdmProvider.ET_OBSERVATION_NAME)) {
//            entitySet = getObservations();
//        } else {
//            return null;
//        }
        entitySet = null;

        /*  generic approach  to find the requested entity */
        Entity requestedEntity = findEntity(edmEntityType, entitySet, keyParams);

        if (requestedEntity == null) {
            // this variable is null if our data doesn't contain an entity for the requested key
            // Throw suitable exception
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }

    public static Entity findEntity(EdmEntityType edmEntityType,
            EntityCollection rt_entitySet, List<UriParameter> keyParams)
            throws ODataApplicationException {

        List<Entity> entityList = rt_entitySet.getEntities();

        // loop over all entities in order to find that one that matches all keys in request
        // an example could be e.g. contacts(ContactID=1, CompanyID=1)
        for (Entity rt_entity : entityList) {
            boolean foundEntity = entityMatchesAllKeys(edmEntityType, rt_entity, keyParams);
            if (foundEntity) {
                return rt_entity;
            }
        }

        return null;
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }

    public static EdmEntitySet getEdmEntitySet(UriInfoResource uriInfo) throws ODataApplicationException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // To get the entity set we have to interpret all URI segments
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

        return uriResource.getEntitySet();
    }

    public static boolean entityMatchesAllKeys(EdmEntityType edmEntityType, Entity rt_entity, List<UriParameter> keyParams)
            throws ODataApplicationException {

        // loop over all keys
        for (final UriParameter key : keyParams) {
            // key
            String keyName = key.getName();
            String keyText = key.getText();

            //remove quote
            keyText = keyText.replaceAll("'", "");

            // Edm: we need this info for the comparison below
            EdmProperty edmKeyProperty = (EdmProperty) edmEntityType.getProperty(keyName);
            Boolean isNullable = edmKeyProperty.isNullable();
            Integer maxLength = edmKeyProperty.getMaxLength();
            Integer precision = edmKeyProperty.getPrecision();
            Boolean isUnicode = edmKeyProperty.isUnicode();
            Integer scale = edmKeyProperty.getScale();
            // get the EdmType in order to compare
            EdmType edmType = edmKeyProperty.getType();
            // Key properties must be instance of primitive type
            EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType) edmType;

            // Runtime data: the value of the current entity
            Object valueObject = rt_entity.getProperty(keyName).getValue(); // null-check is done in FWK

            // now need to compare the valueObject with the keyText String
            // this is done using the type.valueToString //
            String valueAsString = null;
            try {
                valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable, maxLength,
                        precision, scale, isUnicode);
            } catch (EdmPrimitiveTypeException e) {
                throw new ODataApplicationException("Failed to retrieve String value",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
            }

            if (valueAsString == null) {
                return false;
            }

            boolean matches = valueAsString.equals(keyText);
            if (!matches) {
                // if any of the key properties is not found in the entity, we don't need to search further
                return false;
            }
        }

        return true;
    }

    public static EdmEntitySet getNavigationTargetEntitySet(EdmEntitySet startEdmEntitySet,
            EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException {

        EdmEntitySet navigationTargetEntitySet = null;
        String navPropName  = edmNavigationProperty.getType().getName();
        EdmBindingTarget edmBindingTarget = startEdmEntitySet.getRelatedBindingTarget(navPropName);
        if (edmBindingTarget == null) {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        if (edmBindingTarget instanceof EdmEntitySet) {
            navigationTargetEntitySet = (EdmEntitySet) edmBindingTarget;
        } else {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        return navigationTargetEntitySet;
    }

    public static UriResourceNavigation getLastNavigation(final UriInfoResource uriInfo) {

        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        int navigationCount = 1;
        while (navigationCount < resourcePaths.size()
                && resourcePaths.get(navigationCount) instanceof UriResourceNavigation) {
            navigationCount++;
        }

        return (UriResourceNavigation) resourcePaths.get(--navigationCount);
    }

    

    private static String parseDate(String dateParse) throws ODataApplicationException {

        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        dateFormat1.setTimeZone(zoneUTC);
        DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");
        dateFormat2.setTimeZone(zoneUTC);
        DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        dateFormat3.setTimeZone(zoneUTC);

        dateParse = dateParse.replaceAll("Z", "+00:00");
        Date date = null;
        String parsedData;
        try {
            date = dateFormat3.parse(dateParse);
        } catch (ParseException e3) {
            try {
                date = dateFormat2.parse(dateParse);
            } catch (ParseException e2) {
                try {
                    date = dateFormat.parse(dateParse);
                } catch (ParseException e) {
                    try {
                        date = dateFormat1.parse(dateParse);
                    } catch (ParseException e1) {

                    }
                }
            }
        }

        if (date == null) {
            throw new ODataApplicationException("Data format not correct",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
        }

        parsedData = dateFormat.format(date);
        return parsedData;
    }

    public ArrayList<RequestInfo> getRequestInfoList(ArrayList<String> typeNameList, List<UriParameter> keyPredicates) {
        ArrayList<RequestInfo> requestInfoList = new ArrayList();
        for(int i = 0; i< typeNameList.size(); i++){
            String symbioteId = null;
            String internalId = null;
            if(i < keyPredicates.size()){
                ResourceInfo resInfo = null;
                UriParameter key = keyPredicates.get(i);
                String keyName = key.getName();
                String keyText = key.getText();
                //remove quote
                keyText = keyText.replaceAll("'", "");

                try {
                    if (keyName.equalsIgnoreCase("id")) {
                        symbioteId = keyText;
                        Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(keyText);
                        if (resInfoOptional.isPresent()) {
                            resInfo = resInfoOptional.get();
                        }
                    }
                } catch (Exception e) {
                }
                if (resInfo != null) {
                    internalId = resInfo.getInternalId();
                }
            }
            RequestInfo ri = new RequestInfo(typeNameList.get(i), symbioteId, internalId);
            requestInfoList.add(ri);
        }
        return requestInfoList;
    }
}
