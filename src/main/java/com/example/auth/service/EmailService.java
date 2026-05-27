package com.example.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;
    private final String username;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from-email}") String fromEmail,
                        @Value("${app.mail.from-name}") String fromName,
                        @Value("${spring.mail.username:}") String username) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.username = username;
    }

    public void sendActivationEmail(String to, String activationLink) {
        String subject = "Activate your account";
        String html = """
                <p>Welcome! Please click the link below to activate your account.</p>
                <p><a href="%s">Activate</a></p>
                <p>This link expires in 24 hours.</p>
                """.formatted(activationLink);
        send(to, subject, html);
    }

    public void sendOtpEmail(String to, String otp) {
        String subject = "Your login OTP";
        String html = """
                <p>Your one-time login code is:</p>
                <h2>%s</h2>
                <p>This code expires in 5 minutes.</p>
                """.formatted(otp);
        send(to, subject, html);
    }

    private void send(String to, String subject, String html) {
        if (username == null || username.isBlank()) {
            // No SMTP creds configured — log the email for local dev instead of sending.
            log.warn("[Mail disabled] to={} subject={} body={}", to, subject, html);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Sent email to {} (subject={})", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
