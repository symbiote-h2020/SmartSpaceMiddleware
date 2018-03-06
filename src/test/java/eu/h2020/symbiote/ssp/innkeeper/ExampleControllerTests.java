package eu.h2020.symbiote.ssp.innkeeper;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.ssp.innkeeper.services.AuthorizationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class ExampleControllerTests {
    private static Log log = LogFactory.getLog(ExampleControllerTests.class);

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected AuthorizationService authorizationService;

    private MockMvc mockMvc;
    private String testServiceResponse = "testServiceResponse";

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void serviceResponseGenerationFailed() throws Exception {
        // Here we mock the generateServiceResponse to make it fail
        doReturn(new ResponseEntity<>("Failed to generate a service response", HttpStatus.INTERNAL_SERVER_ERROR))
                .when(authorizationService).generateServiceResponse();

        mockMvc.perform(get("/innkeeper/example"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to generate a service response"));
    }

    @Test
    public void securityRequestVerificationFailed() throws Exception {
        // Here we mock the generateServiceResponse to make it pass
        doReturn(new ResponseEntity<>("testServiceResponse", HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();

        // Here we mock the checkExampleRequest to make it fail
        doReturn(new ResponseEntity<>("The stored resource access policy was not satisfied", HttpStatus.UNAUTHORIZED))
                .when(authorizationService).checkExampleRequest(any(), any());

        mockMvc.perform(get("/innkeeper/example"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("The stored resource access policy was not satisfied"));
    }

    @Test
    public void exampleRequestSuccessfulTest() throws Exception {

        // Here we mock the generateServiceResponse to make it pass
        doReturn(new ResponseEntity<>(testServiceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();

        // Here we mock the checkExampleRequest to make it fail
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkExampleRequest(any(), any());

        mockMvc.perform(get("/innkeeper/example"))
                .andExpect(status().isOk());
    }

}
