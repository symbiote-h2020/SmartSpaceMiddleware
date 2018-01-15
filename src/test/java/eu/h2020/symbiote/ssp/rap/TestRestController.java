package eu.h2020.symbiote.ssp.rap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.rap.interfaces.ResourceAccessRestController;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.ssp.rap.plugin.SpecificPlugin;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;

//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestConfiguration 
public class TestRestController {
    
    @InjectMocks
    @Autowired
    ResourceAccessRestController controller;
    
    @Value("${securityEnabled}")
    private Boolean securityEnabled;
    
    @Value("${rap.enableSpecificPlugin}")
    private Boolean enableSpecificPlugin;
    
    private MockMvc mockMvc;
    
    @Autowired
    private IComponentSecurityHandler securityHandler;
    
    @Autowired
    private ResourcesRepository resourcesRepository;
    
    private static final Logger log = LoggerFactory.getLogger(TestRestController.class);
    
     @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = standaloneSetup(controller)
                //.setSingleView(mockView)
                .build();
    }
    
    @Test
    public void testGet() throws Exception{
        try{
            String resourceId = "1";
            //delete
            resourcesRepository.delete(resourceId);
            mockMvc.perform(get("/rap/Sensor/"+resourceId)
                .headers(getHeader())
                .accept(
                        new MediaType(MediaType.APPLICATION_JSON.getType(),
                                MediaType.APPLICATION_JSON.getSubtype(),
                                Charset.forName("utf8") )
                ))
                .andExpect(status().isNotFound());
                //.andExpect(content().string("Honda Civic"));
                
            //insert
            String platformResourceId = "pl_1";
            List<String> obsProperties = null;
            String pluginId = SpecificPlugin.PLUGIN_ID;
            ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
            assert(resourceInfoResult != null);
            //test get
            ResultActions res = mockMvc.perform(get("/rap/Sensor/"+resourceId)
                .headers(getHeader()));
            if(enableSpecificPlugin){
                res.andExpect(status().isOk());
                String content = res.andReturn().getResponse().getContentAsString();

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                Observation observation = mapper.readValue(content, Observation.class);
                assert(observation != null);
            }
            else{
                res.andExpect(status().isInternalServerError());
            }
            //test security
            if(securityEnabled){
                res = mockMvc.perform(get("/rap/Sensor/"+resourceId)
                    .headers(getHeader(false)));
                res.andExpect(status().isInternalServerError());
            }
            //delete
            resourcesRepository.delete(resourceId);
            List<ResourceInfo> resourceInfoList = resourcesRepository.findByInternalId(platformResourceId);
            assert(resourceInfoList == null || resourceInfoList.isEmpty());
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }
    }
    
    
    @Test
    public void testHistory() throws Exception{
        try{
            String resourceId = "1";
            //delete
            resourcesRepository.delete(resourceId);
            mockMvc.perform(get("/rap/Sensor/"+resourceId+"/history")
                .headers(getHeader())
                .accept(
                        new MediaType(MediaType.APPLICATION_JSON.getType(),
                                MediaType.APPLICATION_JSON.getSubtype(),
                                Charset.forName("utf8") )
                ))
                .andExpect(status().isNotFound());
                //.andExpect(content().string("Honda Civic"));
                
            //insert
            String platformResourceId = "pl_1";
            List<String> obsProperties = null;
            String pluginId = SpecificPlugin.PLUGIN_ID;
            ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
            assert(resourceInfoResult != null);
            //test history
            ResultActions res = mockMvc.perform(get("/rap/Sensor/"+resourceId+"/history")
                .headers(getHeader()));
            if(enableSpecificPlugin){
                res.andExpect(status().isOk());
                String content = res.andReturn().getResponse().getContentAsString();

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                List<Observation> observations = mapper.readValue(content, new TypeReference<List<Observation>>() {});
                assert(observations != null);
                assert(!observations.isEmpty());
            }
            else{
                res.andExpect(status().isInternalServerError());
            }
            //test security
            if(securityEnabled){
                res = mockMvc.perform(get("/rap/Sensor/"+resourceId+"/history")
                    .headers(getHeader(false)));
                res.andExpect(status().isInternalServerError());
            }
            //delete
            resourcesRepository.delete(resourceId);
            List<ResourceInfo> resourceInfoList = resourcesRepository.findByInternalId(platformResourceId);
            assert(resourceInfoList == null || resourceInfoList.isEmpty());
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }
    }
    
    
    @Test
    public void testSet() throws Exception{
        try{
            String resourceId = "1";
            //delete
            resourcesRepository.delete(resourceId);
            mockMvc.perform(post("/rap/Actuator/"+resourceId)
                .headers(getHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("null")
                .accept(
                        new MediaType(MediaType.APPLICATION_JSON.getType(),
                                MediaType.APPLICATION_JSON.getSubtype(),
                                Charset.forName("utf8") )
                ))
                .andExpect(status().isNotFound());
                //.andExpect(content().string("Honda Civic"));
                
            //insert
            String platformResourceId = "pl_1";
            List<String> obsProperties = null;
            String pluginId = SpecificPlugin.PLUGIN_ID;
            ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
            assert(resourceInfoResult != null);
            //test set
            ResultActions res = mockMvc.perform(post("/rap/Actuator/"+resourceId)
                .headers(getHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"));
            res.andExpect(status().isOk());
            String content = res.andReturn().getResponse().getContentAsString();
            if(enableSpecificPlugin){
                assert(content.equals(pluginId));
            }
            else{
                content = res.andReturn().getResponse().getContentAsString();
                assert(content.equals(""));
            }
            //test security
            if(securityEnabled){
                res = mockMvc.perform(post("/rap/Actuator/"+resourceId)
                    .headers(getHeader(false)));
                res.andExpect(status().isInternalServerError());
            }
            //delete
            resourcesRepository.delete(resourceId);
            List<ResourceInfo> resourceInfoList = resourcesRepository.findByInternalId(platformResourceId);
            assert(resourceInfoList == null || resourceInfoList.isEmpty());
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }
    }
    
    
    
    private HttpHeaders getHeader(){
        return getHeader(true);
    }
    
    private HttpHeaders getHeader(Boolean addSecurity){
        Map<String, String> securityRequestHeaders = null;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        if(securityEnabled && addSecurity){
            try {
                SecurityRequest securityRequest = securityHandler.generateSecurityRequestUsingLocalCredentials();
                securityRequestHeaders = securityRequest.getSecurityRequestHeaderParams();

                for (Map.Entry<String, String> entry : securityRequestHeaders.entrySet()) {
                    httpHeaders.add(entry.getKey(), entry.getValue());
                }
                log.info("request headers: " + httpHeaders);

            } catch (SecurityHandlerException | JsonProcessingException e) {
                log.error("Fail to take header",e);
            }
        }
        return httpHeaders;
    }
    
    private ResourceInfo addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId);
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        if(pluginId != null && pluginId.length()>0)
            resourceInfo.setPluginId(pluginId);
        
        ResourceInfo resourceInfoResult = resourcesRepository.save(resourceInfo);
        return resourceInfoResult;
    }
}
