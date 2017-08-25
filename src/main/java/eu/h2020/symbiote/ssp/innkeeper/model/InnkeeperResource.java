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

    private InnkeeperResourceStatus status;
    private Long unregisterEventTime;
    private Long offlineEventTime;

    public InnkeeperResource() {
        // empty constructor
    }

    public InnkeeperResource(String id, String hash, DeviceDescriptor deviceDescriptor,
                             List<String> observesProperty, InnkeeperResourceStatus status,
                             Long unregisterEventTime,
                             Long offlineEventTime) {
        super(id, hash, deviceDescriptor, observesProperty);
        setStatus(status);
        setUnregisterEventTime(unregisterEventTime);
        setOfflineEventTime(offlineEventTime);
    }

    public InnkeeperResource(JoinRequest joinRequest, ScheduledUnregisterTimerTask unregisterTimerTask,
                             ScheduledResourceOfflineTimerTask offlineTimerTask) {
        super(joinRequest);
        setStatus(InnkeeperResourceStatus.ONLINE);
        setUnregisterEventTime(unregisterTimerTask.scheduledExecutionTime());

        // ToDo: Change in SYM-568
        // setOfflineEventTime(offlineTimerTask.scheduledExecutionTime());
        setOfflineEventTime(null);
    }

    public InnkeeperResource(InnkeeperResource resource) {
        super((InnkeeperResource) resource);
        setStatus(resource.getStatus());
        setUnregisterEventTime(resource.getUnregisterEventTime());
        setOfflineEventTime(resource.getOfflineEventTime());
    }

    public InnkeeperResourceStatus getStatus() { return status; }
    public void setStatus(InnkeeperResourceStatus status) { this.status = status; }

    public Long getUnregisterEventTime() { return unregisterEventTime; }
    public void setUnregisterEventTime(Long unregisterEventTime) { this.unregisterEventTime = unregisterEventTime; }

    public Long getOfflineEventTime() { return offlineEventTime; }
    public void setOfflineEventTime(Long offlineEventTime) { this.offlineEventTime = offlineEventTime; }
}
