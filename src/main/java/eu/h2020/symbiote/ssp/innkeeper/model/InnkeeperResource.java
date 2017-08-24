package eu.h2020.symbiote.ssp.innkeeper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.ssp.communication.rest.DeviceDescriptor;
import eu.h2020.symbiote.ssp.communication.rest.InnkeeperResourceStatus;
import eu.h2020.symbiote.ssp.communication.rest.JoinRequest;


import java.util.List;

/**
 * Created by vasgl on 8/24/2017.
 */
public class InnkeeperResource extends JoinRequest {

    @JsonProperty("status")
    private InnkeeperResourceStatus status;

    public InnkeeperResource() {
        // empty constructor
    }

    public InnkeeperResource(String id, String hash, DeviceDescriptor deviceDescriptor,
                             List<String> observesProperty, InnkeeperResourceStatus status) {
        super(id, hash, deviceDescriptor, observesProperty);
        setStatus(status);
    }

    public InnkeeperResource(JoinRequest joinRequest) {
        super(joinRequest);
        setStatus(InnkeeperResourceStatus.ONLINE);
    }

    public InnkeeperResource(InnkeeperResource resource) {
        super((InnkeeperResource) resource);
        setStatus(resource.getStatus());
    }

    public InnkeeperResourceStatus getStatus() { return status; }
    public void setStatus(InnkeeperResourceStatus status) { this.status = status; }
}
