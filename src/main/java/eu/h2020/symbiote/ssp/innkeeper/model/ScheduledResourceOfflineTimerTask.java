package eu.h2020.symbiote.ssp.innkeeper.model;

import eu.h2020.symbiote.ssp.communication.rest.InnkeeperResourceStatus;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Map;
import java.util.TimerTask;

/**
 * Created by vasgl on 7/2/2017.
 */
public class ScheduledResourceOfflineTimerTask extends TimerTask {

    private static Log log = LogFactory.getLog(ScheduledResourceOfflineTimerTask.class);

    private static ResourceRepository resourceRepository;
    private String resourceId;
    private Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap;

    public ScheduledResourceOfflineTimerTask(ResourceRepository resourceRepository, String resourceId,
                                             Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap) {
        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(resourceId,"resourceId can not be null!");
        this.resourceId = resourceId;

        Assert.notNull(offlineTimerTaskMap,"offlineTimerTaskMap can not be null!");
        this.offlineTimerTaskMap = offlineTimerTaskMap;
    }

    public void run() {
        log.debug("Periodic resource unregister task for resourceId = " + resourceId +
                " STARTED :" + new Date(new Date().getTime()));

        InnkeeperResource resource = resourceRepository.findOne(resourceId);

        if (resource == null) {
            log.info("The resource with id = " + resourceId + " is not registered.");
        } else {
            log.info("The status of resource with id = " + resourceId + " has turned to " +
                    InnkeeperResourceStatus.OFFLINE);
            resource.setStatus(InnkeeperResourceStatus.OFFLINE);
            resourceRepository.save(resource);
            offlineTimerTaskMap.remove(resourceId);
        }

        log.debug("Periodic resource unregister task for resourceId = " + resourceId +
                " FINISHED :" + new Date(new Date().getTime()));
    }
}
