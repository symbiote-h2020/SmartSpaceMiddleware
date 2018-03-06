package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import eu.h2020.symbiote.ssp.innkeeper.services.ExampleService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * This is an example controller. The requests are handler by the ExampleService
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
@Controller
@RequestMapping("/innkeeper")
@CrossOrigin
public class ExampleController {
    private static Log log = LogFactory.getLog(ExampleController.class);

    private ExampleService exampleService;

    /**
     * Constructor
     *
     * @param exampleService the service handling the requests
     */
    @Autowired
    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    /**
     * An example endpoint
     *
     * @param httpHeaders the HTTP headers of the client's request
     * @return the response to the client
     */
    @GetMapping("/example")
    public ResponseEntity exampleRequest(@RequestHeader HttpHeaders httpHeaders) {
        log.trace("Request to /example");
        return exampleService.handleExampleRequest(httpHeaders);
    }
}
