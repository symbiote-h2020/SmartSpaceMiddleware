package eu.h2020.symbiote.ssp.rap;

import eu.h2020.symbiote.ssp.rap.interfaces.NorthboundRestController;
import eu.h2020.symbiote.ssp.rap.managers.AuthorizationManager;
import eu.h2020.symbiote.ssp.rap.managers.AuthorizationResult;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
public class TestRestController {
    
    @InjectMocks
    @Autowired
    NorthboundRestController controller;

    private MockMvc mockMvc;
    
    @Autowired
    private AuthorizationManager authorizationManager;
    
    @Autowired
    private ResourcesRepository resourcesRepository;
    
    private static final Logger log = LoggerFactory.getLogger(TestRestController.class);
    
     @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = standaloneSetup(controller)
                //.setSingleView(mockView)
                .build();
        doReturn(new AuthorizationResult("Validated", true)).when(authorizationManager)
                .checkResourceUrlRequest(any(), any());
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
            String pluginId = "plugin_01";
            ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
            assert(resourceInfoResult != null);
            //test get
            ResultActions res = mockMvc.perform(get("/rap/Sensor/"+resourceId)
                .headers(getHeader()));

            res.andExpect(status().isInternalServerError());
            //test security
            res = mockMvc.perform(get("/rap/Sensor/"+resourceId)
                .headers(getHeader()));
            res.andExpect(status().isInternalServerError());
            
            //delete
            resourcesRepository.delete(resourceId);
            List<ResourceInfo> resourceInfoList = resourcesRepository.findByInternalIdResource(platformResourceId);
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
            String pluginId = "plugin_01";
            ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
            assert(resourceInfoResult != null);
            //test history
            ResultActions res = mockMvc.perform(get("/rap/Sensor/"+resourceId+"/history")
                .headers(getHeader()));

            res.andExpect(status().isInternalServerError());
            //test security            
            res = mockMvc.perform(get("/rap/Sensor/"+resourceId+"/history")
                .headers(getHeader()));
            res.andExpect(status().isInternalServerError());

            //delete
            resourcesRepository.delete(resourceId);
            List<ResourceInfo> resourceInfoList = resourcesRepository.findByInternalIdResource(platformResourceId);
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
            String pluginId = "plugin_01";
            ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
            assert(resourceInfoResult != null);
            //test set
            ResultActions res = mockMvc.perform(post("/rap/Actuator/"+resourceId)
                .headers(getHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"));
            res.andExpect(status().isOk());
            String content = res.andReturn().getResponse().getContentAsString();
            assert(content.equals(""));
            //test security
            res = mockMvc.perform(post("/rap/Actuator/"+resourceId)
                .headers(getHeader()));
            res.andExpect(status().isInternalServerError());
            //delete
            resourcesRepository.delete(resourceId);
            List<ResourceInfo> resourceInfoList = resourcesRepository.findByInternalIdResource(platformResourceId);
            assert(resourceInfoList == null || resourceInfoList.isEmpty());
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }
    }
        
    private HttpHeaders getHeader(){
        return authorizationManager.getServiceRequestHeaders().getServiceRequestHeaders();
    }
    
    private ResourceInfo addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, "", platformResourceId, "", "");
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        if(pluginId != null && pluginId.length()>0)
            resourceInfo.setPluginUrl(pluginId);
        
        ResourceInfo resourceInfoResult = resourcesRepository.save(resourceInfo);
        return resourceInfoResult;
    }
}
