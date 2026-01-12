package uk.co.aosd.flash.exc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DuplicateProductException extends Exception {
    private final String id;
    private final String name;
}
