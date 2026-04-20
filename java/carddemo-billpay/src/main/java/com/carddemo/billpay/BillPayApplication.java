package com.carddemo.billpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 21 / Spring Boot 3 port of COBIL00C (CardDemo Bill Payment).
 *
 * <p>The business rules preserved here are catalogued in
 * {@code docs/COBIL00C_SPEC.md}. Rule identifiers referenced in the source
 * (e.g. {@code R11}, {@code R22}) map directly to that specification.
 */
@SpringBootApplication
public class BillPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillPayApplication.class, args);
    }
}
