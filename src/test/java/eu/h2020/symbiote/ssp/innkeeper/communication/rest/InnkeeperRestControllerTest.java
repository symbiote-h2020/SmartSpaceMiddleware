package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.ssp.innkeeper.model.*;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.nio.charset.Charset;
import java.util.Arrays;

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
        properties = {"registrationExpiration=100",
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
    }

    @Test
    public void joinTest() throws Exception {
        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        InnkeeperResource resource = new InnkeeperResource("id", "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        MvcResult result = mockMvc.perform(post("/innkeeper/join")
                .content(this.json(resource))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        String joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        validateJoinResponse(joinResponseString, resource);

        resource = new InnkeeperResource(null, "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        result = mockMvc.perform(post("/innkeeper/join")
                .content(this.json(resource))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        validateJoinResponse(joinResponseString, resource);

        resource = new InnkeeperResource("", "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        result = mockMvc.perform(post("/innkeeper/join")
                .content(this.json(resource))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())))
                .andReturn();

        joinResponseString = result.getResponse().getContentAsString();
        log.info("JoinResponse = " + joinResponseString);

        validateJoinResponse(joinResponseString, resource);
    }

    private void validateJoinResponse(String joinResponseString, InnkeeperResource resource) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            JoinResponse joinResponse = mapper.readValue(joinResponseString, JoinResponse.class);
            assertNotNull("The id field of the JoinResponse must not be null", joinResponse.getId());

            InnkeeperResource storedResource = resourceRepository.findOne(joinResponse.getId());
            assertNotNull("The new resource should be saved in the database", storedResource);

            if (resource.getId() != null && !resource.getId().isEmpty())
                assertEquals(resource.getId(), storedResource.getId());
            assertEquals(resource.getObservesProperty(), storedResource.getObservesProperty());

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