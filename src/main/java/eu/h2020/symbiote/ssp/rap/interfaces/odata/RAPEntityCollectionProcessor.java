/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces.odata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.rap.exceptions.CustomODataApplicationException;
import eu.h2020.symbiote.ssp.rap.resources.RequestInfo;
import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
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
import eu.h2020.symbiote.ssp.rap.resources.PlatformRepository;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Component
public class RAPEntityCollectionProcessor implements EntityCollectionProcessor {

    private static final Logger log = LoggerFactory.getLogger(RAPEntityCollectionProcessor.class);
    
    @Autowired
    ResourcesRepository resourcesRepo;
    
    @Autowired
    PlatformRepository pluginRepo;
    
    private StorageHelper storageHelper;

    @Override
    public void init(OData odata, ServiceMetadata sm) {
    //    this.odata = odata;
    //    this.serviceMetadata = sm;
        storageHelper = new StorageHelper(resourcesRepo, pluginRepo);
    }

    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        Object obj;
        InputStream stream = null;
        
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        
        CustomODataApplicationException customOdataException = null;
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
                setErrorResponse(response, customOdataException, responseFormat);
                return;
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
        ResourceInfo resource = storageHelper.getResourceInfo(keyPredicates);
        if (resource == null) {
            customOdataException = new CustomODataApplicationException(null,"Entity not found.", 
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
            //throw customOdataException;
                setErrorResponse(response, customOdataException, responseFormat);
                return;
        }
        
        ArrayList<RequestInfo> requestInfos = storageHelper.getRequestInfoList(typeNameList,keyPredicates);
        
        try{
            obj = storageHelper.getRelatedObject(resource, top, requestInfos);
        }
        catch(ODataApplicationException odataExc){
            log.error(odataExc.getMessage());
            customOdataException = new CustomODataApplicationException(resource.getSymbioteId(),odataExc.getMessage(), 
                    odataExc.getStatusCode(), odataExc.getLocale());
            //throw customOdataException;
                setErrorResponse(response, customOdataException, responseFormat);
                return;
        }

        try {
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = map.writeValueAsString(obj);
            stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        } catch (UnsupportedEncodingException ex) {
            log.error(ex.getMessage());
        }
        
        // 4th: configure the response object: set the body, headers and status code
        //response.setContent(serializerResult.getContent());
        response.setContent(stream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
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
