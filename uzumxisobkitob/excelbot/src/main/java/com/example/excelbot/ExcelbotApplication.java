package com.example.excelbot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.apache.http.client.config.RequestConfig;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class ExcelbotApplication implements CommandLineRunner {

    @Value("${telegram.bot.enabled:true}")
    private boolean botEnabled;

    private final ProductService productService;

    public ExcelbotApplication(ProductService productService) {
        this.productService = productService;
    }

    public static void main(String[] args) {
        SpringApplication.run(ExcelbotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (!botEnabled) {
            System.out.println("Telegram bot test/config orqali o'chirilgan.");
            return;
        }

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new ProductBot(createBotOptions(), productService));

        System.out.println("Telegram bot ishlayapti...");
    }

    private DefaultBotOptions createBotOptions() {
        DefaultBotOptions botOptions = new DefaultBotOptions();
        botOptions.setGetUpdatesTimeout(30);
        botOptions.setRequestConfig(RequestConfig.custom()
                .setConnectTimeout(15_000)
                .setConnectionRequestTimeout(15_000)
                .setSocketTimeout(90_000)
                .build());
        return botOptions;
    }
}
