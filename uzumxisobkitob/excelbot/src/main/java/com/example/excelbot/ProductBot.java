package com.example.excelbot;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProductBot extends TelegramLongPollingBot {

    private final Map<Long, ProductData> userSessions = new ConcurrentHashMap<>();
    private final ProductService productService;

    public ProductBot(DefaultBotOptions botOptions, ProductService productService) {
        super(botOptions);
        this.productService = productService;
    }

    @Override
    public String getBotUsername() {
        return "uzumxisobkitob_bot";
    }

    @Override
    public String getBotToken() {
        return "8839498757:AAFCUC3QeXPSuCzXNSD3krmA8N7qAbrZd4Y";
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if (text.equals("/start")) {
            send(chatId,
                    "Salom! Mahsulot qo‘shish uchun /add bosing.\n\n" +
                            "Bot sizdan mahsulot ismi, sklad narxi, Uzum foizi, logistika, KGT va sotilish narxini so‘raydi."
            );
            return;
        }

        if (text.equals("/add")) {
            ProductData data = new ProductData();
            userSessions.put(chatId, data);

            send(chatId, "Mahsulot ismini kiriting:");
            return;
        }

        if (text.equals("/cancel")) {
            userSessions.remove(chatId);
            send(chatId, "Bekor qilindi.");
            return;
        }

        if (!userSessions.containsKey(chatId)) {
            send(chatId, "Mahsulot qo‘shish uchun /add bosing.");
            return;
        }

        ProductData data = userSessions.get(chatId);

        switch (data.getStep()) {

            case "product_name" -> {
                data.setProductName(text);
                data.setStep("sklad_price");
                send(chatId, "Sklad narxini kiriting:");
            }

            case "sklad_price" -> {
                try {
                    long skladPrice = parseLong(text);

                    if (skladPrice < 0) {
                        send(chatId, "Sklad narxi manfiy bo‘lmasligi kerak.");
                        return;
                    }

                    data.setSkladPrice(skladPrice);
                    data.setStep("uzum_percent");

                    send(chatId, "Uzum foizini kiriting. Masalan: 20");
                } catch (NumberFormatException e) {
                    send(chatId, "Faqat raqam kiriting. Masalan: 35000");
                }
            }

            case "uzum_percent" -> {
                try {
                    double uzumPercent = parseDouble(text);

                    if (uzumPercent < 0) {
                        send(chatId, "Uzum foizi manfiy bo‘lmasligi kerak.");
                        return;
                    }

                    data.setUzumPercent(uzumPercent);
                    data.setStep("logistika");

                    send(chatId,
                            "Logistika narxini kiriting.\n" +
                                    "Default: 5000\n\n" +
                                    "Default olish uchun 0 yoki skip yozing."
                    );
                } catch (NumberFormatException e) {
                    send(chatId, "Foizni raqamda kiriting. Masalan: 20 yoki 18.5");
                }
            }

            case "logistika" -> {
                try {
                    long logistika = parseDefaultMoney(text, 5000);

                    if (logistika < 0) {
                        send(chatId, "Logistika manfiy bo‘lmasligi kerak.");
                        return;
                    }

                    data.setLogistika(logistika);
                    data.setStep("kgt");

                    send(chatId,
                            "KGT narxini kiriting.\n" +
                                    "Default: 5000\n\n" +
                                    "Default olish uchun 0 yoki skip yozing."
                    );
                } catch (NumberFormatException e) {
                    send(chatId, "Faqat raqam kiriting. Masalan: 5000 yoki default uchun 0/skip");
                }
            }

            case "kgt" -> {
                try {
                    long kgt = parseDefaultMoney(text, 5000);

                    if (kgt < 0) {
                        send(chatId, "KGT manfiy bo‘lmasligi kerak.");
                        return;
                    }

                    data.setKgt(kgt);
                    data.setStep("sell_price");

                    send(chatId, "Sotilish narxini kiriting:");
                } catch (NumberFormatException e) {
                    send(chatId, "Faqat raqam kiriting. Masalan: 5000 yoki default uchun 0/skip");
                }
            }

            case "sell_price" -> {
                try {
                    long sellPrice = parseLong(text);

                    if (sellPrice < 0) {
                        send(chatId, "Sotilish narxi manfiy bo‘lmasligi kerak.");
                        return;
                    }

                    data.setSellPrice(sellPrice);

                    productService.saveProduct(data);

                    send(chatId,
                            "✅ Saqlandi!\n\n" +
                                    "Mahsulot: " + data.getProductName() + "\n" +
                                    "Sklad narxi: " + data.getSkladPrice() + "\n" +
                                    "Uzum foizi: " + data.getUzumPercent() + "%\n" +
                                    "Uzum komissiya: " + format(data.getUzumCommission()) + "\n" +
                                    "Logistika: " + data.getLogistika() + "\n" +
                                    "KGT: " + data.getKgt() + "\n" +
                                    "Sotilish narxi: " + data.getSellPrice() + "\n\n" +
                                    "Foyda: " + format(data.getProfit())
                    );

                    userSessions.remove(chatId);

                } catch (NumberFormatException e) {
                    send(chatId, "Sotilish narxini raqamda kiriting. Masalan: 70000");
                } catch (Exception e) {
                    send(chatId, "Xatolik bo‘ldi: " + e.getMessage());
                }
            }
        }
    }

    private long parseDefaultMoney(String text, long defaultValue) {
        if (text.equalsIgnoreCase("skip") || text.equals("0")) {
            return defaultValue;
        }

        return parseLong(text);
    }

    private long parseLong(String text) {
        text = text.replace(" ", "").replace(",", ".");
        return Math.round(Double.parseDouble(text));
    }

    private double parseDouble(String text) {
        text = text.replace(" ", "").replace(",", ".");
        return Double.parseDouble(text);
    }

    private String format(double value) {
        return String.format("%.0f", value);
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
