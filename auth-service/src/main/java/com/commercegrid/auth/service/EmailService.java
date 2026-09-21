package com.commercegrid.auth.service;

import com.commercegrid.auth.enums.AdminStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from-address}")
    private String fromAddress;

    public void sendAdminWelcomeEmail(String toEmail, String adminName, String rawPassword) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your CommerceGrid Admin Account");
        message.setText(
                "Hi " + adminName + ",\n\n"
                        + "Your CommerceGrid admin account has been created.\n\n"
                        + "Email: " + toEmail + "\n"
                        + "Temporary Password: " + rawPassword + "\n\n"
                        + "Please log in and change your password immediately.\n\n"
                        + "— CommerceGrid Team"
        );

        mailSender.send(message);
    }

    public void sendStatusChangeEmail(String toEmail, String adminName, AdminStatus newStatus, String reason) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("CommerceGrid Admin Account Status Update");
        message.setText(
                "Hi " + adminName + ",\n\n"
                        + "Your CommerceGrid admin account status has changed to: " + newStatus + "\n"
                        + "Reason: " + reason + "\n\n"
                        + "If you believe this is a mistake, please contact your system administrator.\n\n"
                        + "— CommerceGrid Team"
        );

        mailSender.send(message);
    }
}