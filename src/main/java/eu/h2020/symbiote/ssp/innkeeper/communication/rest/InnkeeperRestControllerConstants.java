package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vasgl on 8/24/2017.
 */
public class InnkeeperRestControllerConstants {

    public static final String INNKEEPER_BASE_PATH = "/innkeeper";
    public static final String INNKEEPER_JOIN_REQUEST_PATH = "/join";
    public static final String INNKEEPER_REGISTRY_REQUEST_PATH = "/registry";
    public static final String INNKEEPER_UNREGISTRY_REQUEST_PATH = "/unregistry";

    public static final String INNKEEPER_LIST_RESOURCES_REQUEST_PATH = "/list_resources";
    public static final String INNKEEPER_KEEP_ALIVE_REQUEST_PATH = "/keep_alive";
    public static final String SET_INNK_ONLINE = "/set_innk_online";
    public static final String SET_INNK_OFFLINE = "/set_innk_offline";
    
    public static final List<String> SDEV_REGISTRY_PAYLOAD_VALS= Arrays.asList("id","pluginId","pluginURL","semanticDescription");
    public static final List<String> PLATFORM_REGISTRY_PAYLOAD_VALS= Arrays.asList("id","name","description","url","informationModel");
    public static final List<String> JOIN_RESOURCE_PAYLOAD_VALS= SDEV_REGISTRY_PAYLOAD_VALS;
    
    public static final String REGISTRATION_OK = "OK";
    public static final String REGISTRATION_ALREADY_REGISTERED = "ALREADY_REGISTERED";
    public static final String REGISTRATION_OFFLINE = "OFFLINE";
    public static final String REGISTRATION_REJECTED = "REJECTED";
    public static final String REGISTRATION_ERROR = "ERROR";
    
	public static final String REJECTED_SYMID = "REJECTED";
	public static final String OFFLINE_SYMID = "OFFLINE";
    
}
