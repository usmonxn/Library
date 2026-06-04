package com.example.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {AuthServiceApplication.class, AuthServiceApplicationTests.MailTestConfig.class})
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class MailTestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
