package uk.co.aosd.flash.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;
import uk.co.aosd.flash.services.ProductsService;

@WebMvcTest(ProductRestApi.class)
@Import({ErrorMapper.class, GlobalExceptionHandler.class, ProductsService.class})
public class ProductRestApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnReadyStatus() throws Exception {
    }

}
