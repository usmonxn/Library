package com.example.excelbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "telegram.bot.enabled=false")
class ExcelbotApplicationTests {

    @Test
    void contextLoads() {
    }

}
