package uk.co.aosd.flash;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.co.aosd.flash.config.TestSecurityConfig;

@Import({ TestcontainersConfiguration.class, TestSecurityConfig.class })
@SpringBootTest
@ActiveProfiles({ "test", "admin-service", "api-service" })
class FlashSalesDemoAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
