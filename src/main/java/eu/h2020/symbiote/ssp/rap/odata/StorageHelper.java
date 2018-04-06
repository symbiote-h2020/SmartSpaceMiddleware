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
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.ssp.rap.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.ssp.rap.exceptions.RapPluginException;
import eu.h2020.symbiote.ssp.rap.interfaces.RapCommunicationHandler;
import eu.h2020.symbiote.ssp.rap.messages.plugin.RapPluginErrorResponse;
import eu.h2020.symbiote.ssp.rap.messages.plugin.RapPluginOkResponse;
import eu.h2020.symbiote.ssp.rap.messages.plugin.RapPluginResponse;
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

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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
import org.springframework.http.*;
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

    public RapPluginResponse getRelatedObject(ArrayList<ResourceInfo> resourceInfoList, Integer top, Query filterQuery) throws ODataApplicationException {
        String symbioteId = null;
        try {
            top = (top == null) ? TOP_LIMIT : top;
            ResourceAccessMessage msg;

            SessionInfo sessionInfo = null;
            for(ResourceInfo resourceInfo: resourceInfoList) {
                log.info("resourceInfo:\n" + new ObjectMapper().writeValueAsString(resourceInfo));
                String symbioteIdTemp = resourceInfo.getSymIdResource();
                if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty()) {
                    symbioteId = symbioteIdTemp;
                } else {
                    String sspIdRes = resourceInfo.getSspIdResource();
                    if (sspIdRes != null && !sspIdRes.isEmpty()) {
                        symbioteId = sspIdRes;
                    }
                }
                String sspIdParent = resourceInfo.getSspIdParent();
                if(sspIdParent != null && !sspIdParent.isEmpty()) {
                    sessionInfo = sessionsRepo.findBySspId(sspIdParent);
                } else {
                    String symIdPar = resourceInfo.getSymIdParent();
                    if (symIdPar != null && !symIdPar.isEmpty()) {
                        sessionInfo = sessionsRepo.findBySymId(symIdPar);
                    } else {
                        log.error("No parent id associated to resource " + symbioteId);
                    }
                }
            }
            if(sessionInfo == null) {
                log.error("No session associated to resource with id " + symbioteId);
                throw new Exception("No session associated to resource with id " + symbioteId);
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
            ResponseEntity<?> responseEntity = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, byte[].class);
            if (responseEntity == null) {
                log.error("No response from plugin");
                throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
            }
            if (responseEntity.getStatusCode() != HttpStatus.ACCEPTED && responseEntity.getStatusCode() != HttpStatus.OK) {
                log.error("Error response from plugin: " + responseEntity.getStatusCodeValue() + " " + responseEntity.getStatusCode().toString());
                log.error("Body:\n" + responseEntity.getBody());
                throw new Exception("Error response from plugin");
            }
            log.info("response:\n" + responseEntity.getBody());
            RapPluginResponse response = extractRapPluginResponse(responseEntity.getBody());
            if(response!=null) {
                if (response instanceof RapPluginOkResponse) {
                    RapPluginOkResponse okResponse = (RapPluginOkResponse) response;
                    if (okResponse.getBody() != null) {
                        try {
                            // need to clean up response if top 1 is used and RAP plugin does not support filtering
                            if (top == 1) {
                                Observation internalObservation;
                                if (okResponse.getBody() instanceof List) {
                                    List<?> list = (List<?>) okResponse.getBody();
                                    if (list.size() != 0 && list.get(0) instanceof Observation) {
                                        @SuppressWarnings("unchecked")
                                        List<Observation> observations = (List<Observation>) list;
                                        internalObservation = observations.get(0);
                                        Observation observation = new Observation(symbioteId, internalObservation.getLocation(),
                                                internalObservation.getResultTime(), internalObservation.getSamplingTime(),
                                                internalObservation.getObsValues());
                                        okResponse.setBody(Arrays.asList(observation));
                                    }
                                } else if (okResponse.getBody() instanceof Observation) {
                                    internalObservation = (Observation) okResponse.getBody();
                                    Observation observation = new Observation(symbioteId, internalObservation.getLocation(),
                                            internalObservation.getResultTime(), internalObservation.getSamplingTime(),
                                            internalObservation.getObsValues());
                                    okResponse.setBody(Arrays.asList(observation));
                                } else if (okResponse.getBody() instanceof Map) {
                                    String jsonBody = mapper.writeValueAsString(okResponse.getBody());
                                    try {
                                        internalObservation = mapper.readValue(jsonBody, Observation.class);
                                        Observation observation = new Observation(symbioteId, internalObservation.getLocation(),
                                                internalObservation.getResultTime(), internalObservation.getSamplingTime(),
                                                internalObservation.getObsValues());
                                        okResponse.setBody(Arrays.asList(observation));
                                    } catch (Exception e) { /* do nothing*/ }
                                }
                            } else {
                                // top is not 1
                                if (okResponse.getBody() instanceof List) {
                                    List<?> list = (List<?>) okResponse.getBody();
                                    if (list.size() != 0 && list.get(0) instanceof Observation) {
                                        @SuppressWarnings("unchecked")
                                        List<Observation> internalObservations = (List<Observation>) list;

                                        List<Observation> observationsList = new ArrayList<>();
                                        int i = 0;
                                        for (Observation o : internalObservations) {
                                            i++;
                                            if (i > top) {
                                                break;
                                            }
                                            Observation ob = new Observation(symbioteId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                                            observationsList.add(ob);
                                        }
                                        okResponse.setBody(observationsList);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new ODataApplicationException("Can not parse returned object from RAP plugin.\nCause: " + e.getMessage(),
                                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
                                    Locale.ROOT,
                                    e);
                        }
                    }
                } else {
                    return response;
                }
            } else {
                response = new RapPluginOkResponse(200, responseEntity.getBody());
            }

            log.info("RapPluginResponse object:\n" + response);
            return response;
        } catch (Exception e) {
            String err = "Unable to read resource " + symbioteId;
            err += "\n Error: " + e.getMessage();
            log.error(err, e);
            throw new ODataApplicationException(err, HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
        }
    }

    public RapPluginResponse setService(ArrayList<ResourceInfo> resourceInfoList, String requestBody) throws ODataApplicationException {
        String symbioteId = null;
        try {
            ResourceAccessMessage msg;
            SessionInfo sessionInfo = null;
            for(ResourceInfo resourceInfo: resourceInfoList) {
                log.info("resourceInfo:\n" + new ObjectMapper().writeValueAsString(resourceInfo));
                String symbioteIdTemp = resourceInfo.getSymIdResource();
                if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty()) {
                    symbioteId = symbioteIdTemp;
                } else {
                    String sspIdRes = resourceInfo.getSspIdResource();
                    if (sspIdRes != null && !sspIdRes.isEmpty()) {
                        symbioteId = sspIdRes;
                    }
                }
                String sspIdParent = resourceInfo.getSspIdParent();
                if(sspIdParent != null && !sspIdParent.isEmpty()) {
                    sessionInfo = sessionsRepo.findBySspId(sspIdParent);
                } else {
                    String symIdPar = resourceInfo.getSymIdParent();
                    if (symIdPar != null && !symIdPar.isEmpty()) {
                        sessionInfo = sessionsRepo.findBySymId(symIdPar);
                    } else {
                        log.error("No parent id associated to resource " + symbioteId);
                    }
                }
            }
            if(sessionInfo == null) {
                log.error("No session associated to resource with id " + symbioteId);
                throw new Exception("No session associated to resource with id " + symbioteId);
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
            ResponseEntity<?> obj = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, byte[].class);
            if (obj.getStatusCode() != HttpStatus.ACCEPTED && obj.getStatusCode() != HttpStatus.OK) {
                log.error("Error response from plugin: " + obj.getStatusCodeValue() + " " + obj.getStatusCode().toString());
                log.error("Body:\n" + obj.getBody());
                throw new Exception("Error response from plugin");
            }
            RapPluginResponse rpResponse = extractRapPluginResponse(obj.getBody());
            if(rpResponse != null) {
                if (rpResponse instanceof RapPluginErrorResponse){
                    RapPluginErrorResponse errorResponse = (RapPluginErrorResponse) rpResponse;
                    throw new ODataApplicationException(errorResponse.getMessage(), errorResponse.getResponseCode(), null);
                }

            } else {
                rpResponse = new RapPluginOkResponse(200, obj.getBody());
            }
            return rpResponse;
        } catch (Exception e) {
            String err = "Unable to write resource " + symbioteId;
            err += "\n Error: " + e.getMessage();
            log.error(err, e);
            throw new ODataApplicationException("Internal Error", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
        }
    }

    private RapPluginResponse extractRapPluginResponse(Object obj)
            throws ODataApplicationException, UnsupportedEncodingException {
        ObjectMapper mapper = new ObjectMapper();
        if (obj == null) {
            log.error("No response from plugin");
            throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
        }

        String rawObj;
        if (obj instanceof byte[]) {
            rawObj = new String((byte[]) obj, "UTF-8");
        } else if (obj instanceof String) {
            rawObj = (String) obj;
        } else {
            throw new ODataApplicationException("Can not parse response from RAP plugin. Expected byte[] or String but got " + obj.getClass().getName(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
                    Locale.ROOT);
        }

        try {
            JsonNode jsonObj = mapper.readTree(rawObj);
            if (!jsonObj.has(jsonPropertyClassName)) {
                log.error("Field " + jsonPropertyClassName + " is mandatory");
            }
            RapPluginResponse resp = null;
            try {
                resp = mapper.readValue(rawObj, RapPluginResponse.class);
            } catch (Exception ex) {
                log.warn("Can not parse response from RAP to RapPluginResponse.\n Cause: " + ex.getMessage());
            }
            return resp;
        } catch (Exception e) {
            throw new ODataApplicationException("Can not parse response from RAP to JSON.\n Cause: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
                    Locale.ROOT,
                    e);
        }
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
            if(i < keyPredicates.size()){
                UriParameter key = keyPredicates.get(i);
                String keyName = key.getName();
                String keyText = key.getText();
                //remove quote
                keyText = keyText.replaceAll("'", "");

                try {
                    if (keyName.equalsIgnoreCase("id")) {
                        resInfo.setSymIdResource(keyText);
                        Optional<ResourceInfo> resInfoOptional = resourcesRepo.findBySymIdResource(keyText);
                        if (resInfoOptional== null || !resInfoOptional.isPresent()) {
                            Optional<ResourceInfo> tmp = resourcesRepo.findById(keyText);
                            if(tmp != null && tmp.isPresent()) {
                                // check if symbioteId is empty, otherwise using sspId is not valid (it could be a mismatch)
                                String symId = tmp.get().getSymIdResource();
                                if(symId == null || symId.length()<1) {
                                    resInfoOptional = tmp;
                                } else {
                                    log.error("Resource with local id " + keyText + " has a valid symbioteId");
                                    throw new EntityNotFoundException(keyText);
                                }
                            }
                        }
                        noResourceFound = false;
                        //resInfo.setInternalIdResource(resInfoOptional.get().getInternalIdResource());
                        resInfo = resInfoOptional.get();
                    }
                } catch (Exception e) {
                }
            }
            resInfo.setType(typeNameList.get(i));   // DO NOT MOVE FROM HERE
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

