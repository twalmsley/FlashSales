package uk.co.aosd.flash.errorhandling;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ErrorMapper {

    /**
     * Creates map with key: "message" and value: exception's message.
     *
     * @param e - the thrown exception
     * @return the created map
     */
    public Map<String, String> createErrorMap(final Throwable e) {
        final Map<String, String> errorMsg = new HashMap<>();
        errorMsg.put("message", e.getMessage());

        return errorMsg;
    }

    public Map<String, String> createErrorMap(final String message) {
        final Map<String, String> errorMsg = new HashMap<>();
        errorMsg.put("message", message);

        return errorMsg;
    }
}
