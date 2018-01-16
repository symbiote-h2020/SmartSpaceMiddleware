/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.odata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.rap.exceptions.CustomODataApplicationException;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessInfoMessage;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.resources.RapDefinitions;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.PluginRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.query.Query;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import static eu.h2020.symbiote.ssp.rap.odata.RapEntityCollectionProcessor.setErrorResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */

@Component
public class RapEntityProcessor implements EntityProcessor{
    
    private static final Logger log = LoggerFactory.getLogger(RapEntityProcessor.class);
    
    @Autowired
    private ResourcesRepository resourcesRepo;
    
    @Autowired
    private PluginRepository pluginRepo;
    
    @Autowired        
    private AccessPolicyRepository accessPolicyRepo;
    
    @Autowired
    private IComponentSecurityHandler securityHandler;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    private TopicExchange exchange;
    
    @Value("${symbiote.notification.url}") 
    private String notificationUrl;
    
    private OData odata;
    
    private StorageHelper storageHelper;
    
    @Value("${securityEnabled}")
    private Boolean securityEnabled;
    
    @Value("${rabbit.replyTimeout}")
    private int rabbitReplyTimeout;
    
    @Override
    public void init(OData odata, ServiceMetadata sm) {
        this.odata = odata;   
    //    this.serviceMetadata = sm;
        storageHelper = new StorageHelper(resourcesRepo, pluginRepo, accessPolicyRepo, securityHandler, 
                                        rabbitTemplate, rabbitReplyTimeout, exchange, notificationUrl);
    }
    
    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) 
            throws ODataApplicationException, ODataLibraryException{
        InputStream stream = null;
        
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        
        CustomODataApplicationException customOdataException = null;

        String jsonFilter;
        Integer top = null;
        //TOP
        TopOption topOption = uriInfo.getTopOption();
        if (topOption != null) {
            int topNumber = topOption.getValue();
            if (topNumber >= 0) {
                log.info("Top: " + topNumber);
                top = topNumber;
            } else {
                customOdataException = new CustomODataApplicationException(null,"Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                //throw customOdataException;
                RapEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                return;
            }
        }

        //FILTER
        FilterOption filter = uriInfo.getFilterOption();
        Query filterQuery;
        if (filter != null) {
            Expression expression = filter.getExpression();
            try {
                filterQuery = StorageHelper.calculateFilter(expression);
            } catch (ODataApplicationException odataExc) {
                log.error(odataExc.getMessage());
                customOdataException = new CustomODataApplicationException(null,odataExc.getMessage(),
                        odataExc.getStatusCode(), odataExc.getLocale());
                //throw customOdataException;
                RapEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                return;
            }

            try {
                map.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                jsonFilter = map.writeValueAsString(filterQuery);
                log.info("JsonFilter:");
                log.info(jsonFilter);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        ArrayList<String> typeNameList = new ArrayList();

        // 1st retrieve the requested EntitySet from the uriInfo
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
        if (!(uriResource instanceof UriResourceEntitySet)) {
            customOdataException = new CustomODataApplicationException(null,"Only EntitySet is supported", 
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            //throw customOdataException;
                RapEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                return;
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();
        String typeName = startEdmEntitySet.getEntityType().getName();
        typeNameList.add(typeName);

        if (segmentCount > 1) {
            for (int i = 1; i < segmentCount; i++) {
                UriResource segment = resourceParts.get(i);
                if (segment instanceof UriResourceNavigation) {
                    UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) segment;
                    EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                    EdmEntityType targetEntityType = edmNavigationProperty.getType();
                    String typeNameEntity = targetEntityType.getName();
                    typeNameList.add(typeNameEntity);
                }
            }
        }

        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        ResourceInfo resource = storageHelper.getResourceInfo(keyPredicates);
        if (resource == null) {
            customOdataException = new CustomODataApplicationException(null,"Entity not found.", 
                    HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
            //throw customOdataException;
                RapEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                return;
        }        
        //ArrayList<RequestInfo> requestInfos = storageHelper.getRequestInfoList(typeNameList,keyPredicates);
        
        if(securityEnabled){
        // checking access policies
            try {
                String sid = resource.getSymbioteId();
                if(sid != null && sid.length() > 0)
                    storageHelper.checkAccessPolicies(request, sid);
            } catch (Exception ex) {
                log.error("Access policy check error: " + ex.getMessage());
                customOdataException = new CustomODataApplicationException(resource.getSymbioteId(), ex.getMessage(), 
                        HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
                return;
            }
        }
        
        
        try {
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = map.writeValueAsString(resource);
            stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        } catch (UnsupportedEncodingException ex) {
            log.error(ex.getMessage());
        }
        
        if(customOdataException == null && stream != null)
            storageHelper.sendSuccessfulAccessMessage(resource.getSymbioteId(),
                    SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
        
        // 4th: configure the response object: set the body, headers and status code
        //response.setContent(serializerResult.getContent());
        response.setContent(stream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
    
    @Override
    public void createEntity(ODataRequest odr, ODataResponse odr1, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        CustomODataApplicationException customOdataException = null;
        EdmEntitySet responseEdmEntitySet = null; // for building ContextURL
        EntityCollection responseEntityCollection = null; // for the response body
        String responseString = null;
        InputStream stream = null;
        String body = null;
        
        ArrayList<String> typeNameList = new ArrayList<String>();

        // 1st retrieve the requested EntitySet from the uriInfo
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
        if (!(uriResource instanceof UriResourceEntitySet)) {
            customOdataException = new CustomODataApplicationException(null,"Only EntitySet is supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            RapEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
            return;
        }
        
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();
        EdmEntityType startEntityType = startEdmEntitySet.getEntityType();
        
        
        EdmEntityType targetEntityType = startEntityType;
        String typeName = startEntityType.getName();
        typeNameList.add(typeName);

        if (segmentCount > 1) {
            for (int i = 1; i < segmentCount; i++) {
                UriResource segment = resourceParts.get(i);
                if (segment instanceof UriResourceNavigation) {
                    UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) segment;
                    EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                    targetEntityType = edmNavigationProperty.getType();
                    String typeNameEntity = targetEntityType.getName();
                    typeNameList.add(typeNameEntity);
                }
            }
        }
        
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        try { 
            body = IOUtils.toString(requestInputStream, "UTF-8");
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(RapEntityProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        requestInputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        DeserializerResult result = deserializer.entity(requestInputStream, targetEntityType);
        Entity requestEntity = result.getEntity();
        
        String symbioteId = null;
        ArrayList<ResourceInfo> resourceInfoList = null;
        try{
            resourceInfoList = storageHelper.getResourceInfoList(typeNameList,keyPredicates);
            for(ResourceInfo resourceInfo: resourceInfoList){
                String symbioteIdTemp = resourceInfo.getSymbioteId();
                if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty())
                    symbioteId = symbioteIdTemp;
            }
        }
        catch(ODataApplicationException odataExc){
            log.error(odataExc.getMessage());
            customOdataException = new CustomODataApplicationException(null,"Entity not found.", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
            RapEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
            return;
        }
        
        
        if(securityEnabled){
            // checking access policies
            try {
                for(ResourceInfo resource : resourceInfoList) {
                    String sid = resource.getSymbioteId();
                    if(sid != null && sid.length() > 0)
                        storageHelper.checkAccessPolicies(request, sid);
                }
            } catch (Exception ex) {
                log.error("Access policy check error: " + ex.getMessage());
                customOdataException = new CustomODataApplicationException(symbioteId, ex.getMessage(), 
                        HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
                return;
            }
        }
        
        Object obj = storageHelper.setService(resourceInfoList, body);
        
        responseString = "";
        if(obj != null){
            if (obj instanceof byte[]) {
                try {
                    responseString = new String((byte[]) obj, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    java.util.logging.Logger.getLogger(RapEntityProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                responseString = (String) obj;
            }
        }

        try{
            stream = new ByteArrayInputStream(responseString.getBytes("UTF-8"));
        }
        catch(Exception e){
            log.error(e.getMessage());
        }
        
        if(customOdataException == null && stream != null)
            storageHelper.sendSuccessfulAccessMessage(resourceInfoList.get(0).getSymbioteId(),
                    SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
        
        // 4th: configure the response object: set the body, headers and status code
        //response.setContent(serializerResult.getContent());
        response.setContent(stream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void deleteEntity(ODataRequest odr, ODataResponse odr1, UriInfo ui) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
