package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;

import eu.h2020.symbiote.ssp.communication.rest.*;
import eu.h2020.symbiote.ssp.innkeeper.model.*;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Created by vasgl on 8/24/2017.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "registrationExpiration=2000000",
                "makeResourceOffline=2000000",
                "symbiote.ssp.database=symbiote-ssp-database-irct"})
@WebAppConfiguration
public class InnkeeperRestControllerTest {

    private static Log log = LogFactory.getLog(InnkeeperRestControllerTest.class);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ResourceRepository resourceRepository;

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));
    private MockMvc mockMvc;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        resourceRepository.deleteAll();
    }

    @After
    public void clear() {
        resourceRepository.deleteAll();
    }

    @Test
    public void joinTest() throws Exception {
        String url = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH;

        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        JoinRequest joinRequest = new JoinRequest("id", "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        MvcResult result = mockMvc.perform(post(url)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("id")))
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        String joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        validateJoinResponse(joinResponseString, joinRequest);

        joinRequest = new JoinRequest(null, "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        result = mockMvc.perform(post(url)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        validateJoinResponse(joinResponseString, joinRequest);

        joinRequest = new JoinRequest("", "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        assertEquals(true, joinRequest.getId().isEmpty());
        result = mockMvc.perform(post(url)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        validateJoinResponse(joinResponseString, joinRequest);
    }

    @Test
    public void invalidDeviceDescriptorTest() throws Exception {
        String url = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH;

        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        Field queryIntervalField = deviceDescriptor.getClass().getDeclaredField("mac");
        queryIntervalField.setAccessible(true);
        queryIntervalField.set(deviceDescriptor, "invalidMac");

        JoinRequest joinRequest = new JoinRequest("id", "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        MvcResult result = mockMvc.perform(post(url)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.INVALID_MAC_ADDRESS_FORMAT.toString())))
                .andReturn();

        String joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

    }

    @Test
    public void listResourcesTest() throws Exception {
        String url = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_LIST_RESOURCES_REQUEST_PATH;

        ObjectMapper mapper = new ObjectMapper();

        MvcResult result = mockMvc.perform(post(url)
                .content(this.json("id"))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andReturn();

        String listResourceString = result.getResponse().getContentAsString();
        log.info("listResourceString = " + listResourceString);

        ListResourcesResponse listResourcesResponse = mapper.readValue(listResourceString, ListResourcesResponse.class);
        assertEquals(0, listResourcesResponse.getInnkeeperListResourceInfoList().size());

        DeviceDescriptor deviceDescriptor1 = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        InnkeeperResource innkeeperResource1 = new InnkeeperResource("id1", "", deviceDescriptor1,
                Arrays.asList("temperature", "humidity"), InnkeeperResourceStatus.ONLINE, null, null);
        DeviceDescriptor deviceDescriptor2 = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        InnkeeperResource innkeeperResource2 = new InnkeeperResource("id2", "", deviceDescriptor2,
                Arrays.asList("temperature", "humidity", "air quality"), InnkeeperResourceStatus.OFFLINE, null, null);

        resourceRepository.save(innkeeperResource1);
        resourceRepository.save(innkeeperResource2);

        result = mockMvc.perform(post(url)
                .content(this.json("id"))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andReturn();

        listResourceString = result.getResponse().getContentAsString();
        log.info("listResourceString = " + listResourceString);

        listResourcesResponse = mapper.readValue(listResourceString, ListResourcesResponse.class);
        assertEquals(2, listResourcesResponse.getInnkeeperListResourceInfoList().size());
        assertEquals(innkeeperResource1.getId(), listResourcesResponse.getInnkeeperListResourceInfoList().get(0).getId());
        assertEquals(innkeeperResource1.getStatus(), listResourcesResponse.getInnkeeperListResourceInfoList().get(0).getStatus());
        assertEquals(innkeeperResource1.getObservesProperty().size(), listResourcesResponse.getInnkeeperListResourceInfoList().get(0).
                getObservesProperty().size());
        assertEquals(innkeeperResource2.getId(), listResourcesResponse.getInnkeeperListResourceInfoList().get(1).getId());
        assertEquals(innkeeperResource2.getStatus(), listResourcesResponse.getInnkeeperListResourceInfoList().get(1).getStatus());
        assertEquals(innkeeperResource2.getObservesProperty().size(), listResourcesResponse.getInnkeeperListResourceInfoList().get(1).
                getObservesProperty().size());
    }

    @Test
    public void keepAliveTest() throws Exception {
        String url = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH;
        String id = "id";

        KeepAliveRequest req = new KeepAliveRequest(id);
        MvcResult result = mockMvc.perform(post(url)
                .content(this.json(req))
                .contentType(contentType))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result", is("The request with id = " + id + " was not registered.")))
                .andReturn();

        String keepAliveResponseString = result.getResponse().getContentAsString();
        log.info("keepAliveResponseString = " + keepAliveResponseString);

        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        InnkeeperResource innkeeperResource1 = new InnkeeperResource(id, "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"), InnkeeperResourceStatus.ONLINE, null, null);

        resourceRepository.save(innkeeperResource1);

        req = new KeepAliveRequest(id);
        result = mockMvc.perform(post(url)
                .content(this.json(req))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is("The keep_alive request from resource with id = " + id +
                        " was received successfully!")))
                .andReturn();

        keepAliveResponseString = result.getResponse().getContentAsString();
        log.info("keepAliveResponseString = " + keepAliveResponseString);

    }

    private void validateJoinResponse(String joinResponseString, JoinRequest joinRequest) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            JoinResponse joinResponse = mapper.readValue(joinResponseString, JoinResponse.class);
            assertNotNull("The id field of the JoinResponse must not be null", joinResponse.getId());
            assertEquals(JoinResponseResult.OK, joinResponse.getResult());

            InnkeeperResource storedResource = resourceRepository.findOne(joinResponse.getId());
            assertNotNull("The new resource should be stored in the database", storedResource);

            if (joinRequest.getId() != null && !joinRequest.getId().isEmpty())
                assertEquals(joinRequest.getId(), storedResource.getId());
            assertEquals(joinRequest.getObservesProperty(), storedResource.getObservesProperty());
            assertEquals(InnkeeperResourceStatus.ONLINE, storedResource.getStatus());

        } catch (IOException e) {
            e.printStackTrace();
            fail("Error occurred during deserializing JoinResponse");
        }
    }

    private String json(Object o) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(o);
    }
}