package in.co.bel.acempp.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import in.co.bel.acempp.entity.RefreshToken;
import in.co.bel.acempp.repository.RefreshTokenRepository;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.refresh_token_expiration}")
    private long refreshTokenExpiry;

    @Value("${jwt.access_token_expiration}")
    private long accessTokenExpiry;


    @Value("${jwt.inactivity_threshold}")
    private long inactivityThreshold;
    @Value("${concurrent.login.enabled:false}")
    private boolean concurrentLoginEnabled;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }


    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserName(String token) {
        return extractAllClaims(token).getSubject();
    }


    public void storeRefreshToken(String username, String token, Date expiryDate) {
        if (!concurrentLoginEnabled) {
            List<RefreshToken> existingTokens = refreshTokenRepository.findByUsername(username);
            if (!existingTokens.isEmpty()) {
                log.info("Removing {} existing refresh token(s) for user: {}", existingTokens.size(), username);
                refreshTokenRepository.deleteAll(existingTokens);
            }
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUsername(username);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setLastUsed(new Date());

        refreshTokenRepository.save(refreshToken);
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            Optional<RefreshToken> storedTokenOpt = refreshTokenRepository.findByToken(refreshToken);
            if (storedTokenOpt.isEmpty()) {
                log.warn("Refresh token not found in DB");
                return false;
            }

            RefreshToken storedToken = storedTokenOpt.get();

            if (storedToken.getExpiryDate().before(new Date())) {
                log.warn("Refresh token expired for user: {}", storedToken.getUsername());
                refreshTokenRepository.delete(storedToken);
                return false;
            }

            long inactivityDuration = System.currentTimeMillis() - storedToken.getLastUsed().getTime();
            if (inactivityDuration > inactivityThreshold) {
                log.warn("Refresh token inactive for {}ms — user: {}", inactivityDuration, storedToken.getUsername());
                refreshTokenRepository.delete(storedToken);
                return false;
            }

            extractAllClaims(refreshToken); // validates JWT signature

            storedToken.setLastUsed(new Date());
            refreshTokenRepository.save(storedToken);

            log.info("Refresh token validated for user: {}", storedToken.getUsername());
            return true;

        } catch (JwtException e) {
            log.error("Refresh token JWT validation failed: {}", e.getMessage());
            return false;
        }
    }


    public void invalidateRefreshToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            log.info("Invalidating refresh token for user: {}", token.getUsername());
            refreshTokenRepository.delete(token);
        });
    }

}