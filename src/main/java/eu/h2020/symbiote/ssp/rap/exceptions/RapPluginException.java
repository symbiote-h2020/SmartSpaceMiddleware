package eu.h2020.symbiote.ssp.rap.exceptions;

import eu.h2020.symbiote.ssp.rap.messages.plugin.RapPluginErrorResponse;

public class RapPluginException extends RuntimeException {
    private static final long serialVersionUID = -4411730401088873145L;

    private RapPluginErrorResponse response;

    public RapPluginException(int responseCode, String message) {
        super(message);
        response = new RapPluginErrorResponse(responseCode, message);
    }

    public RapPluginException(int responseCode, String message, Throwable reason) {
        super(message, reason);
        response = new RapPluginErrorResponse(responseCode, message);
    }

    public RapPluginException(int responseCode, Throwable reason) {
        super(reason);
        response = new RapPluginErrorResponse(responseCode, reason.getMessage());
    }
    
    public RapPluginErrorResponse getResponse() {
        return response;
    }
}
