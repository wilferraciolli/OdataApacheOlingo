package com.wiltech.odata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * The type Odata sample apache olingo application.
 */
@SpringBootApplication
@ServletComponentScan
public class OdataSampleApacheOlingoApplication {

    /**
     * The entry point of application.
     * @param args the input arguments
     */
    public static void main(String[] args) {
		SpringApplication.run(OdataSampleApacheOlingoApplication.class, args);
	}

}
