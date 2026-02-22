package com.richu.profile.profile.controller;

import com.richu.profile.profile.service.VisitorInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;

@Controller
public class HomeController {


    @Autowired
    private VisitorInterceptor visitorInterceptor;

    @Value("${app.secret.key}")
    private String secretKey;

    @GetMapping("/")
    public String homePage()
    {
        return "index.html";
    }

    @GetMapping("/stats")
    @ResponseBody
    public int stats() {
        return visitorInterceptor.getCount();
    }

    @GetMapping("/download-log")
    public ResponseEntity<String> downloadLog(@RequestParam String key) {

        // check key against env var first
        if (!key.equals(secretKey)) {
            return ResponseEntity.status(403).body("Forbidden: Invalid key");
        }

        try {
            Path filePath = Paths.get("visitor_log.txt");

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            StringBuilder decrypted = new StringBuilder();
            Files.lines(filePath)
                    .filter(line -> !line.isBlank())
                    .forEach(line -> {
                        String decryptedLine = decrypt(line.trim(), key);
                        decrypted.append(decryptedLine).append("\n");
                    });

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"visitor_log_decrypted.txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(decrypted.toString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    private String decrypt(String encryptedData, String key) {
        try {
            Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            return "Decryption failed for line: " + e.getMessage();
        }
    }

}
