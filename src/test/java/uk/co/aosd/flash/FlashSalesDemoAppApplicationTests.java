package uk.co.aosd.flash;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles({"test", "admin-service", "api-service"})
class FlashSalesDemoAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
