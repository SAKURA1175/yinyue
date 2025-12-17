package com.yinyue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yinyue"})
public class YinyueApplication {

    public static void main(String[] args) {
        SpringApplication.run(YinyueApplication.class, args);
    }
}
