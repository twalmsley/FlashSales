package uk.co.aosd.flash.exc;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SaleNotActiveException extends RuntimeException {
    private final UUID saleId;
    private final OffsetDateTime endTime;
    private final OffsetDateTime currentTime;
}
