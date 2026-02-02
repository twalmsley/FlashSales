package uk.co.aosd.flash.exc;

/**
 * Thrown when the user supplies an incorrect current password when updating
 * profile or changing password. Mapped to HTTP 400 (Bad Request) to distinguish
 * from login failure (401).
 */
public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }

    public InvalidCurrentPasswordException(final String message) {
        super(message);
    }
}
