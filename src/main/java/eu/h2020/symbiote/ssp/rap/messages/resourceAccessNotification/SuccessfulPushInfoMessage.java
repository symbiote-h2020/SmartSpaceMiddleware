/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification;

import java.util.Date;
import java.util.List;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class SuccessfulPushInfoMessage extends AccessInfoMessage{

    public SuccessfulPushInfoMessage(String symbioTeId, List<Date> timestamp) {
        this.symbioTeId = symbioTeId;
        this.timestamp = timestamp;
    }
    
}
