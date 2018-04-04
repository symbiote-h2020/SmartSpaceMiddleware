/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.odata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.rap.interfaces.RapCommunicationHandler;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.query.Comparison;
import eu.h2020.symbiote.ssp.rap.resources.query.Filter;
import eu.h2020.symbiote.ssp.rap.resources.query.Operator;
import eu.h2020.symbiote.ssp.rap.resources.query.Query;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class StorageHelper {
    private static final Logger log = LoggerFactory.getLogger(StorageHelper.class);
    
    private final int TOP_LIMIT = 100;
    
    private final RapCommunicationHandler communicationHandler;
    private final ResourcesRepository resourcesRepo;
    private final SessionsRepository sessionsRepo;
    private final RestTemplate restTemplate;
    private final String jsonPropertyClassName;
    
    private static final Pattern PATTERN = Pattern.compile(
            "\\p{Digit}{1,4}-\\p{Digit}{1,2}-\\p{Digit}{1,2}"
            + "T\\p{Digit}{1,2}:\\p{Digit}{1,2}(?::\\p{Digit}{1,2})?"
            + "(Z|([-+]\\p{Digit}{1,2}:\\p{Digit}{2}))?");

    public StorageHelper(ResourcesRepository resourcesRepository, SessionsRepository sessionsRepo,
                         RapCommunicationHandler communicationHandler, RestTemplate restTemplate,
                         String jsonPropertyClassName) {
        this.resourcesRepo = resourcesRepository;
        this.sessionsRepo = sessionsRepo;
        this.communicationHandler= communicationHandler;
        this.restTemplate = restTemplate;
        this.jsonPropertyClassName = jsonPropertyClassName;
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
            }
        }

        return resInfo;
    }

    public Object getRelatedObject(ArrayList<ResourceInfo> resourceInfoList, Integer top, Query filterQuery) throws ODataApplicationException {
        String symbioteId = null;
        try {
            top = (top == null) ? TOP_LIMIT : top;
            ResourceAccessMessage msg;
            
            String sspIdParent = null;
            for(ResourceInfo resourceInfo: resourceInfoList) {
                String symbioteIdTemp = resourceInfo.getSymIdResource();
                if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty())
                    symbioteId = symbioteIdTemp;
                String sspIdParentTemp = resourceInfo.getSspIdParent();
                if(sspIdParent != null && !sspIdParent.isEmpty())
                    sspIdParent = sspIdParentTemp;
            }
            
            if(sspIdParent == null) {
                log.error("No plugin url associated with resource");
                throw new Exception("No plugin url associated with resource");
            }
            SessionInfo sessionInfo = sessionsRepo.findBySspId(sspIdParent);
            if(sessionInfo == null) {
                log.error("No session associated to resource owner with id " + sspIdParent);
                throw new Exception("No session associated to resource owner with id " + sspIdParent);
            }
            String pluginUrl = sessionInfo.getPluginURL();
            
            if (top == 1) {
                msg = new ResourceAccessGetMessage(resourceInfoList);
            } else {
                msg = new ResourceAccessHistoryMessage(resourceInfoList, top, filterQuery);
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            log.info("Sending POST request to " + pluginUrl);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            ResponseEntity<?> responseEntity = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, Object.class);
            if (responseEntity == null) {
                log.error("No response from plugin");
                throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
            }

            Object obj = responseEntity.getBody();
            String responseString = (obj instanceof byte[]) ? new String((byte[]) obj, "UTF-8") : obj.toString();
            // checking if plugin response is a valid json
            try {
                JsonNode jsonObj = mapper.readTree(responseString);
                if(!jsonObj.has(jsonPropertyClassName)) {
                    log.error("Field " + jsonPropertyClassName + " is mandatory in plugin response");
                    //    throw new Exception("Field " + JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                }
            } catch (Exception ex){
                log.error("Response from plugin is not a valid json", ex);
                throw new Exception("Response from plugin is not a valid json");
            }
            
            return responseEntity;
        } catch (Exception e) {
            String err = "Unable to read resource " + symbioteId;
            err += "\n Error: " + e.getMessage();
            log.error(err, e);
            throw new ODataApplicationException(err, HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
        }
    }

    public ResponseEntity<?> setService(ArrayList<ResourceInfo> resourceInfoList, String requestBody) throws ODataApplicationException {
        ResponseEntity<?> responseEntity = new ResponseEntity("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR);
        try {
            ResourceAccessMessage msg;
            String sspIdParent = null;
            for(ResourceInfo resourceInfo: resourceInfoList){
                sspIdParent = resourceInfo.getSspIdParent();
                if(sspIdParent != null)
                    break;
            }
            if(sspIdParent == null) {
                log.error("No plugin url associated with resource");
                throw new Exception("No plugin url associated with resource");
            }

            SessionInfo sessionInfo = sessionsRepo.findBySspId(sspIdParent);
            if(sessionInfo == null) {
                log.error("No session associated to SDEV " + sspIdParent);
                throw new Exception("No session associated to SDEV " + sspIdParent);
            }
            String pluginUrl = sessionInfo.getPluginURL();
            msg = new ResourceAccessSetMessage(resourceInfoList, requestBody);            
            String json = "";
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

                json = mapper.writeValueAsString(msg);
            } catch (JsonProcessingException ex) {
                log.error("JSon processing exception: " + ex.getMessage());
            }
            log.info("Sending POST request to " + pluginUrl);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            responseEntity = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, Object.class);

            Object obj = responseEntity.getBody();
            if(obj != null) {
                String responseString = (obj instanceof byte[]) ? new String((byte[]) obj, "UTF-8") : obj.toString();
                // checking if plugin response is a valid json
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonObj = mapper.readTree(responseString);
                    if (!jsonObj.has(jsonPropertyClassName)) {
                        log.error("Field " + jsonPropertyClassName + " is mandatory in plugin response");
                        //    throw new Exception("Field " + JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                    }
                } catch (Exception ex) {
                    log.error("Response from plugin is not a valid json", ex);
                    throw new Exception("Response from plugin is not a valid json");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new ODataApplicationException("Internal Error", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
        }
        return responseEntity;
    }
    
    public static Query calculateFilter(Expression expression) throws ODataApplicationException {

        if (expression instanceof Binary) {
            Expression left = ((Binary) expression).getLeftOperand();
            BinaryOperatorKind operator = ((Binary) expression).getOperator();
            Expression right = ((Binary) expression).getRightOperand();

            if (left instanceof Binary && right instanceof Binary) {
                ArrayList<Query> exprs = new ArrayList();
                Operator op = null;
                try {
                    op = new Operator(operator.name());
                } catch (Exception ex) {
                    throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                }

                Query leftQuery = calculateFilter(left);
                exprs.add(0, leftQuery);

                Query rightQuery = calculateFilter(right);
                exprs.add(1, (Query) rightQuery);

                Filter f = new Filter(op.getLop(), exprs);
                return f;
            } else if (left instanceof Member && right instanceof Literal) {
                Member member = (Member) left;
                String key = member.toString();

                Literal literal = (Literal) right;
                String value = literal.getText();
                if (literal.getType() instanceof EdmString) {
                    value = value.substring(1, value.length() - 1);
                }

                if (key.contains("resultTime") || key.contains("samplingTime")) {
                    Matcher matcher = PATTERN.matcher(value);
                    if (!matcher.matches()) {
                        throw new ODataApplicationException("Data format not correct",
                                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                    }
                    value = parseDate(value);

                }

                Comparison cmp;
                try {
                    cmp = new Comparison(operator.name());
                } catch (Exception ex) {
                    throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                }
                eu.h2020.symbiote.ssp.rap.resources.query.Expression expr = new eu.h2020.symbiote.ssp.rap.resources.query.Expression(key, cmp.getCmp(), value);

                return expr;
            } else {
                log.error("Not implemented");
                throw new ODataApplicationException("Not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }
        }
        return null;
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
                    } catch (ParseException e1) { }
                }
            }
        }

        if (date == null) {
            log.error("Incorrect data format");
            throw new ODataApplicationException("Data format not correct",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
        }

        parsedData = dateFormat.format(date);
        return parsedData;
    }

    public ArrayList<ResourceInfo> getResourceInfoList(ArrayList<String> typeNameList, List<UriParameter> keyPredicates) throws ODataApplicationException {
        Boolean noResourceFound = true;
        ArrayList<ResourceInfo> resourceInfoList = new ArrayList();
        for(int i = 0; i< typeNameList.size(); i++){
            ResourceInfo resInfo = new ResourceInfo();
            resInfo.setType(typeNameList.get(i));
            if(i < keyPredicates.size()){
                UriParameter key = keyPredicates.get(i);
                String keyName = key.getName();
                String keyText = key.getText();
                //remove quote
                keyText = keyText.replaceAll("'", "");

                try {
                    if (keyName.equalsIgnoreCase("id")) {
                        resInfo.setSymIdResource(keyText);
                        Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(keyText);
                        if (resInfoOptional.isPresent()) {
                            noResourceFound = false;
                            resInfo.setInternalIdResource(resInfoOptional.get().getInternalIdResource());
                        }
                    }
                } catch (Exception e) {
                }
            }
            resourceInfoList.add(resInfo);
        }
        if(noResourceFound) {
            log.error("No entity found with id specified in request");
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
        }
        return resourceInfoList;
    }

    public boolean checkAccessPolicies(ODataRequest request, String resourceId) throws Exception {
        return communicationHandler.checkAccessPolicies(request, resourceId);
    }
}

