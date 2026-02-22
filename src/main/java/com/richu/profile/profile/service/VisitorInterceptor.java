package com.richu.profile.profile.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.*;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class VisitorInterceptor implements HandlerInterceptor {

    private final AtomicInteger visitorCount = new AtomicInteger(0);
    private static final String COUNT_FILE = "visitor_count.txt";
    private static final String LOG_FILE = "visitor_log.txt";
    @Value("${app.secret.key}")
    private String secretKey;


    @PostConstruct
    public void init() {
        try {
            String savedCount = Files.readString(Paths.get(COUNT_FILE)).trim();
            visitorCount.set(Integer.parseInt(savedCount));
            System.out.println("Resuming visitor count: " + savedCount);
        } catch (IOException e) {
            System.out.println("No existing count file, starting from 0");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String url = request.getRequestURI();

        if (url.equals("/")) {
            HttpSession session = request.getSession();

            if (session.getAttribute("counted") == null) {
                int count = visitorCount.incrementAndGet();
                session.setAttribute("counted", true);

                String referrer = request.getHeader("Referer");
                String source = parseReferrer(referrer);

                System.out.println("=== New Visitor #" + count + " ===");
                System.out.println("Time: " + LocalDateTime.now());
                System.out.println("Source: " + source);

                saveCount(count);
                saveLog(count, source);
            }
        }

        return true;
    }

    private String encrypt(String data) {
        try {
            Key key = new SecretKeySpec(secretKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
            return data;  // fallback to plain text if encryption fails
        }
    }

    private void saveLog(int count, String source) {
        String logEntry = String.format(
                "Visitor #%d | Time: %s | Source: %s",
                count, LocalDateTime.now(), source
        );
        String encryptedEntry = encrypt(logEntry);  // encrypt it
        try {
            Files.writeString(Paths.get(LOG_FILE), encryptedEntry + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write visitor log: " + e.getMessage());
        }
    }

    private String parseReferrer(String referrer) {
        if (referrer == null || referrer.isEmpty()) {
            return "Direct";  // typed URL directly or bookmarked
        } else if (referrer.contains("linkedin.com")) {
            return "LinkedIn";
        } else if (referrer.contains("google.com")) {
            return "Google";
        } else if (referrer.contains("github.com")) {
            return "GitHub";
        } else if (referrer.contains("twitter.com") || referrer.contains("x.com")) {
            return "Twitter/X";
        } else if (referrer.contains("instagram.com")) {
            return "Instagram";
        } else if (referrer.contains("whatsapp.com") || referrer.contains("wa.me")) {
            return "WhatsApp";
        } else {
            return referrer;  // return the full URL if unknown source
        }
    }

    private void saveCount(int count) {
        try {
            Files.writeString(Paths.get(COUNT_FILE), String.valueOf(count));
        } catch (IOException e) {
            System.err.println("Failed to save visitor count: " + e.getMessage());
        }
    }

    public int getCount() {
        return visitorCount.get();
    }
}