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
                "registrationExpiration=500",
                "makeResourceOffline=2000000",
                "symbiote.ssp.database=symbiote-ssp-database-irct"})
@WebAppConfiguration
public class CheckUnregistrationTests {

    private static Log log = LogFactory.getLog(CheckUnregistrationTests.class);

    @Autowired
    @Qualifier("registrationExpiration")
    private Integer registrationExpiration;

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
    public void unregistrationTest() throws Exception {
        String url = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH;
        String id = "id";

        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        JoinRequest joinRequest = new JoinRequest(id, "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        MvcResult result = mockMvc.perform(post(url)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        String joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        InnkeeperResource storedResource = resourceRepository.findOne(id);
        assertNotNull("The new resource should be saved in the database", storedResource);
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertEquals(offlineTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getOfflineEventTime());

        TimeUnit.MILLISECONDS.sleep(registrationExpiration);

        // Make sure that the resource is deleted from the database after {registrationExpiration} ms have passed
        storedResource = resourceRepository.findOne(id);
        assertEquals(null, storedResource);
        assertNull("The unregistrationTimerTask should be removed along with the resource",
                unregisteringTimerTaskMap.get(id));
        assertNull("The offlineTimerTask should be removed along with the resource",
                offlineTimerTaskMap.get(id));
    }

    @Test
    public void reRegistrationTest() throws Exception {
        // Send a unregistration request before the resource is unregistered

        String joinUrl = InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH;
        String id = "id";

        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        JoinRequest joinRequest = new JoinRequest(id, "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        MvcResult result = mockMvc.perform(post(joinUrl)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        String joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        InnkeeperResource storedResource = resourceRepository.findOne(id);
        assertNotNull("The new resource should be stored in the database", storedResource);
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertEquals(offlineTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getOfflineEventTime());

        TimeUnit.MILLISECONDS.sleep((long) (0.5 * registrationExpiration));
        result = mockMvc.perform(post(joinUrl)
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.ALREADY_REGISTERED.toString())))
                .andReturn();

        joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        TimeUnit.MILLISECONDS.sleep((long) (0.5 * registrationExpiration));

        // Make sure that the resource is not deleted from the database, since there was a 2nd JoinRequest
        storedResource = resourceRepository.findOne(id);
        assertNotNull("The new resource should be stored in the database after the re-registration event",
                storedResource);
        assertEquals(unregisteringTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getUnregisterEventTime());
        assertEquals(offlineTimerTaskMap.get(id).scheduledExecutionTime(), (long) storedResource.getOfflineEventTime());

        TimeUnit.MILLISECONDS.sleep((long) (0.75 * registrationExpiration));

        // Make sure that the resource is deleted from the database after {registrationExpiration} ms have passed from
        // the 2nd JoinRequest
        storedResource = resourceRepository.findOne(id);
        assertEquals(null, storedResource);
        assertNull("The unregistrationTimerTask should be removed along with the resource",
                unregisteringTimerTaskMap.get(id));
        assertNull("The offlineTimerTask should be removed along with the resource",
                offlineTimerTaskMap.get(id));

    }

    private String json(Object o) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(o);
    }
}