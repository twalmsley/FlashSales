package uk.co.aosd.flash.errorhandling;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Error mapping.
 */
@Component
public class ErrorMapper {

    /**
     * Error mapping.
     *
     * @param message the error message String
     * @return a Map of String to String
    */
    public Map<String, String> createErrorMap(final String message) {
        final Map<String, String> errorMsg = new HashMap<>();
        errorMsg.put("message", message);

        return errorMsg;
    }
}
