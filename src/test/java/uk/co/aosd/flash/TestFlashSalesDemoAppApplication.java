package uk.co.aosd.flash;

import org.springframework.boot.SpringApplication;

public class TestFlashSalesDemoAppApplication {

	public static void main(String[] args) {
		SpringApplication.from(FlashSalesDemoAppApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
