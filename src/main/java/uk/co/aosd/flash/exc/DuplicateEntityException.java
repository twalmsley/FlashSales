package uk.co.aosd.flash.exc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DuplicateEntityException extends RuntimeException {
    private final String id;
    private final String name;
}
