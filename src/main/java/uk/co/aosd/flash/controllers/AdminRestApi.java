package uk.co.aosd.flash.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/admin")
public class AdminRestApi {

    public AdminRestApi() {
    }

    @GetMapping("admin_api_status")
    public ResponseEntity<String> adminApiStatus() {
        return ResponseEntity.ok("Ready");
    }
}
