package uk.co.aosd.flash.controllers;

// Static imports for the fluent API (crucial for readability)
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.aosd.flash.errorhandling.ErrorMapper;
import uk.co.aosd.flash.errorhandling.GlobalExceptionHandler;

/**
 * Admin API Web Test.
 */
@WebMvcTest(AdminRestApi.class)
@Import({ErrorMapper.class, GlobalExceptionHandler.class})
public class AdminRestApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnReadyStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/admin_api_status")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Ready"));
    }

}
