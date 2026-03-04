package com.example.facesapp.service;

import com.example.facesapp.config.EnvConfig;
import com.example.facesapp.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SMTP-based implementation of {@link EmailService}.
 * All connection details are read from {@link EnvConfig} / .env.
 */
@ApplicationScoped
public class EmailServiceImpl implements EmailService {

    private static final Logger LOG = Logger.getLogger(EmailServiceImpl.class.getName());

    @Inject
    private EnvConfig env;

    // ── public API ────────────────────────────────────────────────────────────

    @Override
    public void sendVerificationEmail(User user, String token) {
        String link = env.getAppBaseUrl() + "/verify-email.xhtml?token=" + token;
        String subject = "Please verify your e-mail – Faces App";
        String body = buildVerificationBody(user.getName(), link);
        send(user.getEmail(), subject, body);
    }

    @Override
    public void sendPasswordResetEmail(User user, String token) {
        String link = env.getAppBaseUrl() + "/reset-password.xhtml?token=" + token;
        String subject = "Password reset request – Faces App";
        String body = buildPasswordResetBody(user.getName(), link);
        send(user.getEmail(), subject, body);
    }

    // ── internal ──────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        Session session = buildSession();
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(env.getMailFrom()));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(msg);
        } catch (MessagingException e) {
            LOG.log(Level.SEVERE, "Failed to send e-mail to " + to, e);
        }
    }

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(env.isMailStartTlsEnabled()));
        props.put("mail.smtp.host",            env.getMailHost());
        props.put("mail.smtp.port",            String.valueOf(env.getMailPort()));

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(env.getMailUsername(), env.getMailPassword());
            }
        });
    }

    // ── HTML templates ────────────────────────────────────────────────────────

    private String buildVerificationBody(String name, String link) {
        return """
               <html><body>
               <p>Hi %s,</p>
               <p>Thanks for signing up! Please verify your e-mail address by clicking the link below.
               The link is valid for <strong>24 hours</strong>.</p>
               <p><a href="%s">Verify my e-mail</a></p>
               <p>If you did not create an account, you can safely ignore this message.</p>
               </body></html>
               """.formatted(name, link);
    }

    private String buildPasswordResetBody(String name, String link) {
        return """
               <html><body>
               <p>Hi %s,</p>
               <p>We received a request to reset your password. Click the link below to choose a new one.
               The link is valid for <strong>1 hour</strong>.</p>
               <p><a href="%s">Reset my password</a></p>
               <p>If you did not request a password reset, you can safely ignore this message.</p>
               </body></html>
               """.formatted(name, link);
    }
}
