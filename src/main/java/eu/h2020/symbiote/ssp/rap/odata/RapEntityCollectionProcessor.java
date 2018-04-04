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
import eu.h2020.symbiote.ssp.rap.RapConfig;
import eu.h2020.symbiote.ssp.rap.exceptions.CustomODataApplicationException;
import eu.h2020.symbiote.ssp.rap.interfaces.RapCommunicationHandler;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessInfoMessage;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.query.Query;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Locale;
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
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Component
public class RapEntityCollectionProcessor implements EntityCollectionProcessor {

    private static final Logger log = LoggerFactory.getLogger(RapEntityCollectionProcessor.class);
    
    @Autowired
    private ResourcesRepository resourcesRepo;
    
    @Autowired
    private SessionsRepository sessionsRepo;
    
    @Autowired
    private RapCommunicationHandler communicationHandler;
    
    @Autowired
    private RestTemplate restTemplate;

    private StorageHelper storageHelper;
    
        
    @Override
    public void init(OData odata, ServiceMetadata sm) {
    //    this.odata = odata;
    //    this.serviceMetadata = sm;
        storageHelper = new StorageHelper(resourcesRepo, sessionsRepo, communicationHandler,
                                        restTemplate, RapConfig.JSON_PROPERTY_CLASS_NAME);
    }

    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        try {
            Object obj;
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
                    log.error("Invalid value for $top");
                    customOdataException = new CustomODataApplicationException(null, "Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                    //throw customOdataException;
                    setErrorResponse(response, customOdataException, responseFormat);
                    return;
                }
            }

            //FILTER
            FilterOption filter = uriInfo.getFilterOption();
            Query filterQuery = null;
            if (filter != null) {
                Expression expression = filter.getExpression();
                try {
                    filterQuery = StorageHelper.calculateFilter(expression);
                } catch (ODataApplicationException odataExc) {
                    log.error("Error while reading filters: " + odataExc.getMessage());
                    customOdataException = new CustomODataApplicationException(null,odataExc.getMessage(),
                            odataExc.getStatusCode(), odataExc.getLocale());
                    //throw customOdataException;
                    setErrorResponse(response, customOdataException, responseFormat);
                    return;
                }

                try {
                    map.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    jsonFilter = map.writeValueAsString(filterQuery);
                    log.info("Filter:");
                    log.info(jsonFilter);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            ArrayList<String> typeNameList = new ArrayList();

            // 1st retrieve the requested EntitySet from the uriInfo
            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            int segmentCount = resourceParts.size();

            UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
            if (!(uriResource instanceof UriResourceEntitySet)) {
                log.error("Only EntitySet is supported");
                customOdataException = new CustomODataApplicationException(null, "Only EntitySet is supported", 
                                                                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
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
            String symbioteId = null;
            ArrayList<ResourceInfo> resourceInfoList;
            try {
                resourceInfoList = storageHelper.getResourceInfoList(typeNameList,keyPredicates);
                for(ResourceInfo resourceInfo: resourceInfoList){
                    String symbioteIdTemp = resourceInfo.getSymIdResource();
                    if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty()) {
                        symbioteId = symbioteIdTemp;
                        break;
                    }
                }
            } catch(ODataApplicationException odataExc){
                log.error("Entity not found: " + odataExc.getMessage(), odataExc);
                customOdataException = new CustomODataApplicationException(null,
                        "Entity not found", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
                return;
            }

            if(!storageHelper.checkAccessPolicies(request, symbioteId)) {
                log.error("Access policy check error" );
                customOdataException = new CustomODataApplicationException(symbioteId, "Access policy check error",
                        HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
                return;
            }

            try {
                obj = storageHelper.getRelatedObject(resourceInfoList, top, filterQuery);
            } catch(ODataApplicationException odataExc) {
                log.error(odataExc.getMessage(), odataExc);
                customOdataException = new CustomODataApplicationException(symbioteId,odataExc.getMessage(), 
                        odataExc.getStatusCode(), odataExc.getLocale());
                setErrorResponse(response, customOdataException, responseFormat);
                return;
            }       

            try {

                map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                String json = map.writeValueAsString(obj);
                stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage(), e);
            } catch (UnsupportedEncodingException ex) {
                log.error(ex.getMessage(), ex);
            }

            if(customOdataException == null && stream != null)
                communicationHandler.sendSuccessfulAccessMessage(symbioteId, SuccessfulAccessInfoMessage.AccessType.NORMAL.name());

            // 4th: configure the response object: set the body, headers and status code
            //response.setContent(serializerResult.getContent());
            response.setContent(stream);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
        }
    }
    
    
    public static ODataResponse setErrorResponse(ODataResponse response, 
            CustomODataApplicationException customOdataException, ContentType responseFormat){
        InputStream stream = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = map.writeValueAsString(customOdataException);
            stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        } catch (UnsupportedEncodingException ex) {
            log.error(ex.getMessage());
        }
        response.setContent(stream);
        response.setStatusCode(customOdataException.getStatusCode());
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        
        return response;
    }
}
