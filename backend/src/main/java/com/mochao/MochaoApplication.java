package com.mochao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mochao.module.**.mapper")
public class MochaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MochaoApplication.class, args);
    }
}
