package com.mochao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@MapperScan("com.mochao.module.**.mapper")
public class MochaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MochaoApplication.class, args);
    }
}
