package uk.co.aosd.flash.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import uk.co.aosd.flash.dto.CreateSaleDto;

@RestController
@Profile("admin-service")
@RequestMapping("/api/v1/admin")
public class FlashSaleRestApi {

    private static Logger log = LoggerFactory.getLogger(FlashSaleRestApi.class.getName());

    public FlashSaleRestApi() {
    }

    @PostMapping("/flash_sale")
    public ResponseEntity<String> createSale(@Valid @RequestBody final CreateSaleDto sale) {
        log.info("Creating Flash Sale: " + sale);
        return ResponseEntity.ok(sale.title());
    }
}
