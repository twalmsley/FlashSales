package uk.co.aosd.flash.exc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Thrown when there isn't enough stock to reserve for a product.
 */
@Getter
@RequiredArgsConstructor
public class InsufficientResourcesException extends RuntimeException {
    private final String id;
}

