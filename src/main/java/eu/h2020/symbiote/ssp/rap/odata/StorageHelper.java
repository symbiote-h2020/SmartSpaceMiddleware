/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.odata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.ssp.rap.interfaces.ResourceAccessNotification;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessInfoMessage;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.query.Comparison;
import eu.h2020.symbiote.ssp.rap.resources.query.Filter;
import eu.h2020.symbiote.ssp.rap.resources.query.Operator;
import eu.h2020.symbiote.ssp.rap.resources.query.Query;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.PluginInfo;
import eu.h2020.symbiote.ssp.resources.db.PluginRepository;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class StorageHelper {
    private static final Logger log = LoggerFactory.getLogger(StorageHelper.class);
    
    private final int TOP_LIMIT = 100;
    
    private final IComponentSecurityHandler securityHandler;
    private final AccessPolicyRepository accessPolicyRepo;
    private final ResourcesRepository resourcesRepo;
    private final PluginRepository pluginRepo;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;
    private String notificationUrl;

    private static final Pattern PATTERN = Pattern.compile(
            "\\p{Digit}{1,4}-\\p{Digit}{1,2}-\\p{Digit}{1,2}"
            + "T\\p{Digit}{1,2}:\\p{Digit}{1,2}(?::\\p{Digit}{1,2})?"
            + "(Z|([-+]\\p{Digit}{1,2}:\\p{Digit}{2}))?");

    public StorageHelper(ResourcesRepository resourcesRepository, PluginRepository pluginRepository,
                        AccessPolicyRepository accessPolicyRepository, IComponentSecurityHandler securityHandlerComponent,
                         RabbitTemplate rabbit, TopicExchange topicExchange, String notificationUrl) {
        //initSampleData();
        resourcesRepo = resourcesRepository;
        pluginRepo = pluginRepository;
        accessPolicyRepo = accessPolicyRepository;
        securityHandler = securityHandlerComponent;
        rabbitTemplate = rabbit;
        exchange = topicExchange;
        this.notificationUrl = notificationUrl;
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

        return resInfo;
    }

    public Object getRelatedObject(ArrayList<ResourceInfo> resourceInfoList, Integer top, Query filterQuery) throws ODataApplicationException {
        String symbioteId = null;
        Object response = null;
        try {
            top = (top == null) ? TOP_LIMIT : top;
            ResourceAccessMessage msg;
            
            String pluginId = null;
            for(ResourceInfo resourceInfo: resourceInfoList){
                String symbioteIdTemp = resourceInfo.getSymbioteId();
                if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty())
                    symbioteId = symbioteIdTemp;
                String pluginIdTemp = resourceInfo.getPluginId();
                if(pluginIdTemp != null && !pluginIdTemp.isEmpty())
                    pluginId = pluginIdTemp;
            }
            if(pluginId == null) {
                List<PluginInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPluginId();
            }
            String routingKey;
            if (top == 1) {
                msg = new ResourceAccessGetMessage(resourceInfoList);
                routingKey =  pluginId + "." + ResourceAccessMessage.AccessType.GET.toString().toLowerCase();
                
            } else {
                msg = new ResourceAccessHistoryMessage(resourceInfoList, top, filterQuery);
                routingKey =  pluginId + "." + ResourceAccessMessage.AccessType.HISTORY.toString().toLowerCase();
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            log.debug("Message: ");
            log.debug(json);
            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            if (obj == null) {
                log.error("No response from plugin");
                throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
            }

            if (obj instanceof byte[]) {
                response = new String((byte[]) obj, "UTF-8");
            } else {
                response = obj;
            }
            
            try {
                List<Observation> observations = mapper.readValue(response.toString(), new TypeReference<List<Observation>>() {});
                if (observations == null || observations.isEmpty()) {
                    log.error("No observations for resource " + symbioteId);
                    return null;
                }            

                if (top == 1) {
                    Observation o = observations.get(0);
                    Observation ob = new Observation(symbioteId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                    response = ob;
                } else {
                    List<Observation> observationsList = new ArrayList();
                    for (Observation o : observations) {
                        Observation ob = new Observation(symbioteId, o.getLocation(), o.getResultTime(), o.getSamplingTime(), o.getObsValues());
                        observationsList.add(ob);
                    }
                    response = observationsList;
                }
            } catch (Exception e) {
            }
            return response;
        } catch (Exception e) {
            String err = "Unable to read resource " + symbioteId;
            err += "\n Error: " + e.getMessage();
            log.error(err);
            throw new ODataApplicationException(err, HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ROOT);
        }
    }

    public Object setService(ArrayList<ResourceInfo> resourceInfoList, String requestBody) throws ODataApplicationException {
        Object obj = null;
        try {
            ResourceAccessMessage msg;
            String pluginId = null;
            for(ResourceInfo resourceInfo: resourceInfoList){
                pluginId = resourceInfo.getPluginId();
                if(pluginId != null)
                    break;
            }
            if(pluginId == null) {
                List<PluginInfo> lst = pluginRepo.findAll();
                if(lst == null || lst.isEmpty())
                    throw new Exception("No plugin found");
                
                pluginId = lst.get(0).getPluginId();
            }
            String routingKey = pluginId + "." + ResourceAccessMessage.AccessType.SET.toString().toLowerCase();
            
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
            log.info("Message Set: " + json);
            obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
            
        } catch (Exception e) {
            throw new ODataApplicationException("Internal Error", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
        }
        return obj;
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
                    } catch (ParseException e1) {

                    }
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
                        resInfo.setSymbioteId(keyText);
                        Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(keyText);
                        if (resInfoOptional.isPresent()) {
                            noResourceFound = false;
                            resInfo.setInternalId(resInfoOptional.get().getInternalId());
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
        log.debug("Checking access policies for resource " + resourceId);
        Map<String,List<String>> headers = request.getAllHeaders();
        Map<String, String> secHdrs = new HashMap();
        for(String key : headers.keySet()) {
            secHdrs.put(key, request.getHeader(key));
        }
        log.info("Headers: " + secHdrs);
        SecurityRequest securityReq = new SecurityRequest(secHdrs);

        checkAuthorization(securityReq, resourceId);
        
        return true;
    }
    
    private void checkAuthorization(SecurityRequest request, String resourceId) throws Exception {
        log.debug("Received a security request : " + request.toString());
         // building dummy access policy
        Map<String, IAccessPolicy> accessPolicyMap = new HashMap<>();
        // to get policies here
        Optional<AccessPolicy> accPolicy = accessPolicyRepo.findById(resourceId);
        if(accPolicy == null) {
            log.error("No access policies for resource");
            throw new Exception("No access policies for resource");
        }
        
        accessPolicyMap.put(resourceId, accPolicy.get().getPolicy());
        String mapString = accessPolicyMap.entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue())
                .collect(Collectors.joining(", "));
        log.info("accessPolicyMap: " + mapString);
        log.info("request: " + request.toString());

        Set<String> ids = null;
        try {
            ids = securityHandler.getSatisfiedPoliciesIdentifiers(accessPolicyMap, request);
        } catch (Exception e) {
            log.error("Exception thrown during checking policies:", e);
            throw new Exception(e.getMessage());
        }
        if(!ids.contains(resourceId)) {
            log.error("Security Policy is not valid");
            throw new Exception("Security Policy is not valid");
        }
    }
    
    public void sendSuccessfulAccessMessage(String symbioteId, String accessType){
        try{
            String jsonNotificationMessage = null;
            if(accessType == null || accessType.isEmpty())
                accessType = SuccessfulAccessInfoMessage.AccessType.NORMAL.name();
            ObjectMapper map = new ObjectMapper();
            map.configure(SerializationFeature.INDENT_OUTPUT, true);
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            List<Date> dateList = new ArrayList<>();
            dateList.add(new Date());
            ResourceAccessNotification notificationMessage = new ResourceAccessNotification(securityHandler,notificationUrl);

            try{
                notificationMessage.SetSuccessfulAttempts(symbioteId, dateList, accessType);
                jsonNotificationMessage = map.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException e) {
                log.error(e.toString(), e);
            }
            notificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
        }catch(Exception e){
            log.error("Error to send SetSuccessfulAttempts to CRAM");
            log.error(e.getMessage(),e);
        }
    }
}
