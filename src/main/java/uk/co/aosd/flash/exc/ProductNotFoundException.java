package uk.co.aosd.flash.exc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProductNotFoundException extends Exception {
    private final String id;
}

