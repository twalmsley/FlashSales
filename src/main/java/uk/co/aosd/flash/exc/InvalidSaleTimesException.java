package uk.co.aosd.flash.exc;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class InvalidSaleTimesException extends Exception {
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
}
