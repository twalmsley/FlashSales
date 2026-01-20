package uk.co.aosd.flash.exc;

public class SaleDurationTooShortException extends RuntimeException {
    public SaleDurationTooShortException(final String msg) {
        super(msg);
    }
}
