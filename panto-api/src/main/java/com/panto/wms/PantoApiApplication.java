package com.panto.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PantoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PantoApiApplication.class, args);
    }

}
