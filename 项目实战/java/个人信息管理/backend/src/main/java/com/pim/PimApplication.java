package com.pim;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.pim.mapper")
public class PimApplication {

    public static void main(String[] args) {
        SpringApplication.run(PimApplication.class, args);
    }
}
