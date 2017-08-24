package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResource;
import eu.h2020.symbiote.ssp.innkeeper.model.JoinResponse;
import eu.h2020.symbiote.ssp.innkeeper.model.JoinResponseResult;

import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by vasgl on 8/24/2017.
 */

@RestController
@RequestMapping("/innkeeper")
public class InnkeeperRestController {

    private static Log log = LogFactory.getLog(InnkeeperRestController.class);

    private Integer registrationExpiration;

    private ResourceRepository resourceRepository;

    @Autowired
    public InnkeeperRestController(ResourceRepository resourceRepository,
                                   @Qualifier("registrationExpiration") Integer registrationExpiration) {

        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(registrationExpiration,"registrationExpiration can not be null!");
        this.registrationExpiration = registrationExpiration;
    }

    @PostMapping("/join")
    ResponseEntity<JoinResponse> join(@RequestBody InnkeeperResource resource) {

        log.info("New join request was received for resource id = " + resource.getId());

        InnkeeperResource newResource = resourceRepository.save(new InnkeeperResource(resource));
        log.debug("newResource.getId() = " + newResource.getId());

        JoinResponse joinResponse = new JoinResponse(JoinResponseResult.OK, newResource.getId(),
                "", registrationExpiration);
        return ResponseEntity.ok(joinResponse);
    }
}
