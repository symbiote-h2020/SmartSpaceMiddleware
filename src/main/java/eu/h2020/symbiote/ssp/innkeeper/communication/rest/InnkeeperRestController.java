package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import eu.h2020.symbiote.ssp.innkeeper.model.JoinRequest;

import eu.h2020.symbiote.ssp.innkeeper.model.JoinResponse;
import eu.h2020.symbiote.ssp.innkeeper.model.JoinResponseResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    @Qualifier("registrationExpiration")
    private Integer registrationExpiration;

    @PostMapping("/join")
    ResponseEntity<JoinResponse> join(@RequestBody JoinRequest joinRequest) {

        log.info("New join request was received for resource id = " + joinRequest.getId());

        JoinResponse joinResponse = new JoinResponse(JoinResponseResult.OK, joinRequest.getId(),
                "", registrationExpiration);
        return ResponseEntity.ok(joinResponse);
    }
}
