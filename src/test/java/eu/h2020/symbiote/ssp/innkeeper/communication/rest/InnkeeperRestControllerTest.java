package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import eu.h2020.symbiote.ssp.innkeeper.model.AgentType;
import eu.h2020.symbiote.ssp.innkeeper.model.DeviceDescriptor;
import eu.h2020.symbiote.ssp.innkeeper.model.JoinRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.JoinResponseResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.context.WebApplicationContext;
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
@SpringBootTest(properties = {"registrationExpiration=100"})
@WebAppConfiguration
public class InnkeeperRestControllerTest {

    private static Log log = LogFactory.getLog(InnkeeperRestControllerTest.class);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private InnkeeperRestController innkeeperRestController;

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));
    private MockMvc mockMvc;
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
        assertNotNull("InnkeeperRestController must not be null", this.innkeeperRestController);
    }

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void joinTest() throws Exception {
        DeviceDescriptor deviceDescriptor = new DeviceDescriptor("00:00:00:00:00:00", true,
                AgentType.SDEV, 100);
        JoinRequest joinRequest = new JoinRequest("id", "", deviceDescriptor,
                Arrays.asList("temperature", "humidity"));

        mockMvc.perform(post("/innkeeper/join")
                .content(this.json(joinRequest))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(JoinResponseResult.OK.toString())));

    }

    private String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}