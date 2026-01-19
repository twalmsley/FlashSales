package uk.co.aosd.flash.exc;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class InvalidSaleTimesException extends Exception {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
}
