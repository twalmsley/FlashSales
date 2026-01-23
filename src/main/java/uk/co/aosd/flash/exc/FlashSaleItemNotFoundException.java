package uk.co.aosd.flash.exc;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FlashSaleItemNotFoundException extends RuntimeException {
    private final UUID itemId;
}
