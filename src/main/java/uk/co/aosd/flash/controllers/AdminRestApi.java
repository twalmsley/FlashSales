package uk.co.aosd.flash.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("api-service")
public class AdminRestApi {

    public AdminRestApi() {
    }
}
