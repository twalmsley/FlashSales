package uk.co.aosd.flash.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("api-service")
@RequestMapping("/api/v1")
public class ClientRestApi {

    public ClientRestApi() {
    }

    @GetMapping("client_api_status")
    public String clientApiStatus() {
        return "Ready";
    }
}
