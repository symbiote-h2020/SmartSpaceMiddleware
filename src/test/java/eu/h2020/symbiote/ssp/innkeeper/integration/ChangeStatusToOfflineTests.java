package eu.h2020.symbiote.ssp.innkeeper.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.ssp.communication.rest.*;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResource;
import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledResourceOfflineTimerTask;
import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledUnregisterTimerTask;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by vasgl on 8/24/2017.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "registrationExpiration=2000000",
                "makeResourceOffline=500",
                "symbiote.ssp.database=symbiote-ssp-database-irct"})
@WebAppConfiguration
public class ChangeStatusToOfflineTests {

    private static Log log = LogFactory.getLog(ChangeStatusToOfflineTests.class);

    @Autowired
    @Qualifier("makeResourceOffline")
    private Integer makeResourceOffline;

    @Autowired
    @Qualifier("unregisteringTimerTaskMap")
    private Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap;

    @Autowired
    @Qualifier("offlineTimerTaskMap")
    private Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap;

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
    public void changeStatusToOfflineTest() throws Exception {
        String joinUrl = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH;
        ObjectMapper mapper = new ObjectMapper();
        String id = "id";

        // Register the resource
        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        JoinRequest joinRequest = new JoinRequest(id, "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        MvcResult result = mockMvc.perform(post(joinUrl)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        String joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        JoinResponse joinResponse = mapper.readValue(joinResponseString, JoinResponse.class);
        InnkeeperResource storedResource = resourceRepository.findOne(joinResponse.getId());

        assertNotNull("The new resource should be saved in the database", storedResource);
        assertEquals(InnkeeperResourceStatus.ONLINE, storedResource.getStatus());
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertEquals(offlineTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getOfflineEventTime());

        TimeUnit.MILLISECONDS.sleep(makeResourceOffline);

        // Make sure that the status of the resource has changed after {makeResourceOffline} ms have passed
        storedResource = resourceRepository.findOne(id);
        assertNotNull("The new resource should be saved in the database", storedResource);
        assertEquals(InnkeeperResourceStatus.OFFLINE, storedResource.getStatus());
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertNull("The offlineTimerTask should be removed", offlineTimerTaskMap.get(id));
    }

    @Test
    public void changeStatusAfterKeepAliveTest() throws Exception {
        String joinUrl = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH;
        String keepAliveUrl = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH;
        ObjectMapper mapper = new ObjectMapper();
        String id = "id";
        // Register the resource
        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        JoinRequest joinRequest = new JoinRequest(id, "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        MvcResult result = mockMvc.perform(post(joinUrl)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        String joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        JoinResponse joinResponse = mapper.readValue(joinResponseString, JoinResponse.class);
        assertEquals(id, joinResponse.getId());

        InnkeeperResource storedResource = resourceRepository.findOne(id);
        assertNotNull("The new resource should be saved in the database", storedResource);
        assertEquals(InnkeeperResourceStatus.ONLINE, storedResource.getStatus());
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertEquals(offlineTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getOfflineEventTime());

        TimeUnit.MILLISECONDS.sleep((long) (0.5 * makeResourceOffline));

        // Send a KeepAlive message
        KeepAliveRequest req = new KeepAliveRequest(id);
        result = mockMvc.perform(post(keepAliveUrl)
                .content(this.json(req))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andReturn();

        String keepAliveResponseString = result.getResponse().getContentAsString();
        log.info("keepAliveResponseString = " + keepAliveResponseString);
        KeepAliveResponse keepAliveResponse = mapper.readValue(keepAliveResponseString, KeepAliveResponse.class);
        assertEquals("The keep_alive request from resource with id = " + id +
                " was received successfully!", keepAliveResponse.getResult());

        TimeUnit.MILLISECONDS.sleep((long) (0.5 * makeResourceOffline));

        // Make sure that the status in still ONLINE, since there was a KeepAlive request
        storedResource = resourceRepository.findOne(id);
        assertNotNull("The new resource should be saved in the database", storedResource);
        assertEquals(InnkeeperResourceStatus.ONLINE, storedResource.getStatus());
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertEquals(offlineTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getOfflineEventTime());

        TimeUnit.MILLISECONDS.sleep((long) (0.75 * makeResourceOffline));

        // Make sure that the status of the resource has changed after {makeResourceOffline} ms have passed
        // from the KeepAlive request
        storedResource = resourceRepository.findOne(id);
        assertNotNull("The new resource should be saved in the database", storedResource);
        assertEquals(InnkeeperResourceStatus.OFFLINE, storedResource.getStatus());
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertNull("The offlineTimerTask should be removed", offlineTimerTaskMap.get(id));
    }

    private String json(Object o) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(o);
    }
}