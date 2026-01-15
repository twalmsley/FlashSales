package uk.co.aosd.flash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class FlashSalesDemoAppApplication {

	public static void main(final String[] args) {
		SpringApplication.run(FlashSalesDemoAppApplication.class, args);
	}

}
