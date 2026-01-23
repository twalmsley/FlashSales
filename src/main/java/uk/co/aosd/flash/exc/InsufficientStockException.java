package uk.co.aosd.flash.exc;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InsufficientStockException extends RuntimeException {
    private final UUID flashSaleItemId;
    private final Integer requestedQuantity;
    private final Integer availableStock;
}
