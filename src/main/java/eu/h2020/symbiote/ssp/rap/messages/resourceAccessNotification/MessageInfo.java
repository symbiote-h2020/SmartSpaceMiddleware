/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class MessageInfo {
    @JsonProperty("symbIoTeId")
    protected String symbioTeId;
    @JsonProperty("timestamp")
    protected List<Date> timestamp;

    public String getSymbioTeId() {
        return symbioTeId;
    }

    public void setSymbioTeId(String symbioTeId) {
        this.symbioTeId = symbioTeId;
    }

    public List<Date> getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(List<Date> timestamp) {
        this.timestamp = timestamp;
    }
}
