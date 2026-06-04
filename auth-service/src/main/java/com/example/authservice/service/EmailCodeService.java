package com.example.authservice.service;

import com.example.authservice.client.UserClient;
import com.example.authservice.entity.EmailCodeEntity;
import com.example.authservice.exception.CustomException;
import com.example.authservice.repository.EmailCodeRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_EXPIRE_MINUTES = 10;

    private final JavaMailSender mailSender;
    private final EmailCodeRepository emailCodeRepository;
    private final UserClient userClient;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendCode(String gmail) {
        if (gmail == null || gmail.isBlank()) {
            throw new CustomException(400, "Gmail kiritilishi kerak");
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new CustomException(500, "Gmail username sozlanmagan");
        }

        String code = String.valueOf(100000 + RANDOM.nextInt(900000));
        EmailCodeEntity emailCode = new EmailCodeEntity();
        emailCode.setGmail(gmail);
        emailCode.setCode(code);
        emailCode.setUsed(false);
        emailCode.setCreatedAt(LocalDateTime.now());
        emailCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        emailCodeRepository.save(emailCode);

        try {
            userClient.updateGmailCode(gmail, code);
        } catch (FeignException e) {
            throw new CustomException(400, "Gmail kodini users jadvaliga saqlab bo'lmadi");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(gmail);
            helper.setSubject("Yavax Library tasdiqlash kodi");
            helper.setText(buildVerificationEmailHtml(code), true);
            mailSender.send(message);
        } catch (MailAuthenticationException e) {
            throw new CustomException(500, "Gmail username yoki app password notogri");
        } catch (MessagingException e) {
            throw new CustomException(500, "Gmail xabari html formatini tayyorlab bo'lmadi");
        } catch (MailException e) {
            throw new CustomException(500, "Gmailga kod jonatishda xato");
        }
    }

    private String buildVerificationEmailHtml(String code) {
        String spacedCode = code.replaceAll("(.)", "$1 ").trim();
        return """
                <!doctype html>
                <html lang="uz">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Yavax Library tasdiqlash kodi</title>
                </head>
                <body style="margin:0;background:#faf5ee;color:#3a302a;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#faf5ee;padding:28px 16px 70px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%;">
                          <tr>
                            <td style="border:1px solid #ded6cc;border-radius:12px;background:#faf5ee;padding:50px 56px 0;text-align:center;box-shadow:0 18px 44px rgba(58,48,42,0.04);overflow:hidden;">
                              <div style="font-family:Georgia,'Times New Roman',serif;font-size:24px;font-weight:700;color:#3a302a;margin-bottom:34px;">
                                <span style="display:inline-block;color:#c9672a;font-size:25px;vertical-align:-2px;margin-right:8px;">▰</span>Yavax Library
                              </div>
                              <h1 style="margin:0 0 26px;font-family:Georgia,'Times New Roman',serif;font-size:48px;line-height:1.08;font-weight:400;color:#3a302a;">Hisobingizni tasdiqlang</h1>
                              <p style="margin:0 auto 28px;max-width:440px;font-size:18px;line-height:1.55;color:#746b63;">
                                Xush kelibsiz! Tizimga kirish yoki ro'yxatdan o'tishni yakunlash uchun quyidagi tasdiqlash kodidan foydalaning:
                              </p>
                              <div style="margin:0 auto 28px;background:#eee8de;border:1px solid #e2dacf;border-radius:10px;padding:34px 24px;font-size:50px;line-height:1;letter-spacing:14px;font-weight:700;color:#c9672a;">{{CODE}}</div>
                              <div style="margin:0 0 20px;font-size:16px;letter-spacing:1.5px;text-transform:uppercase;color:#817870;">Xavfsizlik eslatmasi</div>
                              <p style="margin:0 auto 24px;max-width:430px;font-size:14px;line-height:1.45;color:#817870;font-style:italic;">
                                Ushbu kod {{MINUTES}} daqiqa davomida faol bo'ladi. Xavfsizlik yuzasidan uni hech kimga bermang.
                              </p>
                              <div style="height:1px;background:#e8e0d6;margin:0 20px 38px;"></div>
                              <p style="margin:0 0 50px;font-size:14px;color:#746b63;">Muammo yuzaga kelsa, <a href="mailto:{{SUPPORT_EMAIL}}" style="color:#c9672a;text-decoration:none;font-weight:700;">biz bilan bog'laning</a>.</p>
                              <div style="height:7px;background:linear-gradient(90deg,#f2cfbd,#c9672a);margin:0 -56px;border-radius:0 0 12px 12px;"></div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:48px 32px 0;">
                              <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td width="33%" style="font-family:Georgia,'Times New Roman',serif;font-size:16px;font-weight:700;color:#3a302a;vertical-align:top;">Yavax<br>Library</td>
                                  <td width="34%" style="font-size:16px;line-height:2;color:#746b63;text-align:center;vertical-align:top;">
                                    Terms of Service<br>Privacy Policy<br>Admin Support
                                  </td>
                                  <td width="33%" style="font-size:16px;line-height:1.45;color:#aaa198;text-align:left;vertical-align:top;">© 2024 Yavax Library.<br>All rights reserved.</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .replace("{{CODE}}", spacedCode)
                .replace("{{MINUTES}}", String.valueOf(CODE_EXPIRE_MINUTES))
                .replace("{{SUPPORT_EMAIL}}", fromEmail);
    }

    public void verifyCode(String gmail, String code) {
        EmailCodeEntity emailCode = emailCodeRepository.findTopByGmailAndUsedFalseOrderByCreatedAtDesc(gmail)
                .orElseThrow(() -> new CustomException(400, "Kod topilmadi"));

        if (emailCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            emailCode.setUsed(true);
            emailCodeRepository.save(emailCode);
            throw new CustomException(400, "Kod eskirgan");
        }
        if (!emailCode.getCode().equals(code)) {
            throw new CustomException(400, "Kod notogri");
        }

        emailCode.setUsed(true);
        emailCodeRepository.save(emailCode);

        try {
            userClient.clearGmailCode(gmail);
        } catch (FeignException ignored) {
            // Kod tasdiqlangan — users.gmail_code tozalash ixtiyoriy
        }
    }
}
