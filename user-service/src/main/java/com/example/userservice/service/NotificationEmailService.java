package com.example.userservice.service;

import com.example.userservice.entity.BorrowsEntity;
import com.example.userservice.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class NotificationEmailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void borrowCreated(UserEntity user, BorrowsEntity borrow) {
        send(user, "Yavax Library: ijara tasdiqlandi",
                "Kitob #" + borrow.getBookId() + " ijaraga olindi. Qaytarish muddati: " + borrow.getDueDate());
    }

    public void dueTomorrow(UserEntity user, BorrowsEntity borrow) {
        send(user, "Yavax Library: qaytarishga 1 kun qoldi",
                "Kitob #" + borrow.getBookId() + " qaytarish muddati ertaga tugaydi.");
    }

    public void fineStarted(UserEntity user, BorrowsEntity borrow) {
        send(user, "Yavax Library: jarima boshlandi",
                "Kitob #" + borrow.getBookId() + " bo'yicha jarima boshlandi. Hozirgi jarima: " + borrow.getFineSum() + " so'm.");
    }

    public void returned(UserEntity user, BorrowsEntity borrow) {
        send(user, "Yavax Library: kitob qaytarildi",
                "Kitob #" + borrow.getBookId() + " qaytarildi. Jami to'lov: " + borrow.getPaidSum() + " so'm.");
    }

    private void send(UserEntity user, String subject, String text) {
        if (user == null || user.getGmail() == null || user.getGmail().isBlank()
                || fromEmail == null || fromEmail.isBlank()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getGmail());
            helper.setSubject(subject);
            helper.setText(buildHtml(text), true);
            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException ignored) {
            // Email notification asosiy ijara oqimini to'xtatmasligi kerak.
        }
    }

    private String buildHtml(String text) {
        return """
                <!doctype html>
                <html lang="uz">
                <body style="margin:0;background:#faf5ee;color:#3a302a;font-family:Arial,Helvetica,sans-serif;">
                  <div style="max-width:560px;margin:0 auto;padding:36px 18px;">
                    <div style="border:1px solid #ded6cc;border-radius:12px;background:#fffaf4;padding:32px;">
                      <div style="font-family:Georgia,serif;font-size:24px;font-weight:700;margin-bottom:18px;color:#3a302a;">Yavax Library</div>
                      <p style="font-size:17px;line-height:1.55;color:#5d5148;margin:0;">{{TEXT}}</p>
                      <div style="height:6px;background:#c9672a;border-radius:999px;margin-top:28px;"></div>
                    </div>
                  </div>
                </body>
                </html>
                """.replace("{{TEXT}}", text);
    }
}
