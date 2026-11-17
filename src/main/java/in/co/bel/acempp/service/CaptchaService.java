package in.co.bel.acempp.service;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.imageio.ImageIO;

import in.co.bel.acempp.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import in.co.bel.acempp.dto.auth.CaptchaResponse;
import in.co.bel.acempp.entity.Captcha;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.repository.CaptchaRepository;
import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaService {

    private final CaptchaRepository captchaRepository;

    /** Local/dev only — when true, any non-blank captcha id+value passes. */
    @Value("${captcha.bypass:false}")
    private boolean captchaBypass;

    private static final int EXPIRES_IN_SECONDS = 300;
    private static final int IMAGE_WIDTH  = 200;
    private static final int IMAGE_HEIGHT = 60;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";


    public CaptchaResponse generateCaptcha() {
        String captchaId   = "cpt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String captchaText = generateRandomText(6);
        String base64Image = buildCaptchaImage(captchaText);

        // Calculate expiry as a java.util.Date
        Date expiresAt = Date.from(LocalDateTime.now().plusSeconds(EXPIRES_IN_SECONDS)
                .atZone(ZoneId.systemDefault()).toInstant());

        Captcha entity = Captcha.builder()
                .captchaId(captchaId)
                .captchaText(captchaText)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();

        captchaRepository.save(entity);

        return CaptchaResponse.builder()
                .captchaId(captchaId)
                .captchaImage(base64Image)
                .expiresIn(EXPIRES_IN_SECONDS)
                .build();
    }




    private String generateRandomText(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String buildCaptchaImage(String text) {
        try {
            BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Transparent background
            g.setComposite(java.awt.AlphaComposite.Clear);
            g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            g.setComposite(java.awt.AlphaComposite.SrcOver);

            SecureRandom rand = new SecureRandom();

            // ---------------------------
            // 1. DENSE DOT NOISE (like reference)
            // ---------------------------
            for (int i = 0; i < 300; i++) {
                int x = rand.nextInt(IMAGE_WIDTH);
                int y = rand.nextInt(IMAGE_HEIGHT);

                int alpha = 40 + rand.nextInt(80);
                g.setColor(new Color(0, 90, 255, alpha));

                g.fillRect(x, y, 1, 1);
            }

            // ---------------------------
            // 2. RANDOM SHORT STROKES (scribble noise)
            // ---------------------------
            g.setStroke(new java.awt.BasicStroke(1.0f));

            for (int i = 0; i < 20; i++) {
                int x1 = rand.nextInt(IMAGE_WIDTH);
                int y1 = rand.nextInt(IMAGE_HEIGHT);
                int x2 = x1 + rand.nextInt(10) - 5;
                int y2 = y1 + rand.nextInt(10) - 5;

                g.setColor(new Color(0, 100, 255, 60));
                g.drawLine(x1, y1, x2, y2);
            }

            // ---------------------------
            // 3. TEXT (scribbly + uneven)
            // ---------------------------
            String[] fontNames = {"Dialog", "SansSerif"};
            int x = 10;

            for (char c : text.toCharArray()) {

                int blue = 180 + rand.nextInt(50);
                g.setColor(new Color(80, 120, blue, 180)); // lighter + semi-transparent

                int style = Font.PLAIN; // IMPORTANT
                int size = 24 + rand.nextInt(4);

                String fontName = fontNames[rand.nextInt(fontNames.length)];
                g.setFont(new Font(fontName, style, size));

                int y = 35 + rand.nextInt(15);

                double angle = Math.toRadians(-20 + rand.nextInt(41));

                java.awt.geom.AffineTransform old = g.getTransform();
                g.rotate(angle, x + 10, y);

                g.drawString(String.valueOf(c), x, y);

                g.setTransform(old);

                x += 22 + rand.nextInt(6);
            }

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (IOException e) {
            log.error("Captcha image generation failed", e);
            throw new AuthException(ErrorCode.CAPTCHA_GENERATION_FAILED,
                    "Unable to generate captcha");
        }
    }

    public boolean validate(String captchaId, String captchaValue) {
        if (captchaId == null || captchaId.isBlank() || captchaValue == null || captchaValue.isBlank()) {
            return false;
        }

        String id = captchaId.trim();
        String value = captchaValue.trim();

        // Dev/testing bypass — accept any non-blank id+value without matching the image.
        if (captchaBypass) {
            captchaRepository.findById(id).ifPresent(captcha -> {
                if (!captcha.isUsed()) {
                    captcha.setUsed(true);
                    captchaRepository.save(captcha);
                }
            });
            return true;
        }

        // captchaId is @Id — look up by id, then check used/expiry in code
        // (avoids fragile boolean isUsed query derivation).
        Captcha captcha = captchaRepository.findById(id).orElse(null);

        if (captcha == null || captcha.isUsed()) {
            return false;
        }

        if (captcha.getExpiresAt() == null || captcha.getExpiresAt().before(new Date())) {
            return false;
        }

        String stored = captcha.getCaptchaText() != null ? captcha.getCaptchaText() : "";
        boolean valid = stored.equalsIgnoreCase(value);

        // Only consume on success so a mistype can retry the same captcha.
        if (valid) {
            captcha.setUsed(true);
            captchaRepository.save(captcha);
        }

        return valid;
    }
}