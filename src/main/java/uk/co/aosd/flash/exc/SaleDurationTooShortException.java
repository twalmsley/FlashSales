package uk.co.aosd.flash.exc;

import lombok.Getter;

/**
 * Thrown when a flash sale duration is shorter than the minimum required
 * duration.
 */
@Getter
public class SaleDurationTooShortException extends RuntimeException {
    private final float actualDuration;
    private final float minimumDuration;

    /**
     * Constructor with detailed duration information.
     * 
     * @param message
     *            the error message
     * @param actualDuration
     *            the actual duration in minutes
     * @param minimumDuration
     *            the minimum required duration in minutes
     */
    public SaleDurationTooShortException(final String message, final float actualDuration, final float minimumDuration) {
        super(message);
        this.actualDuration = actualDuration;
        this.minimumDuration = minimumDuration;
    }

    /**
     * Constructor for backward compatibility.
     * 
     * @param message
     *            the error message
     */
    public SaleDurationTooShortException(final String message) {
        super(message);
        this.actualDuration = 0.0f;
        this.minimumDuration = 0.0f;
    }
}
