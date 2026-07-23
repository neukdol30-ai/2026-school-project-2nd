package com.siyan1234.itproject2nd;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ItProject2ndApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItProject2ndApplication.class, args);
    }

}
