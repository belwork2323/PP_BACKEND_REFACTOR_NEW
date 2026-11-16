package in.co.bel.acempp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;


@Component
public class JwtContextUtil {

    @Value("${jwt.expiration:3600000}")
    private long ACCESS_TOKEN_EXPIRY;

    @Value("${jwt.refresh.expiration:86400000}")
    private long REFRESH_TOKEN_EXPIRY;

    @Value("${jwt.secret}")
    private String SECRET;

    /**
     * FIX: Always build SecretKey from secret string — same approach as JwtUtil.
     * Previously used raw .getBytes() which was inconsistent with JwtUtil's Keys.hmacShaKeyFor().
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /* ==========================
       REQUEST HEADER EXTRACTION
       ========================== */
    public String getTokenFromHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new RuntimeException("No request context found");
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        return authHeader.substring(7);
    }

    /* ==========================
       CLAIM EXTRACTION METHODS
       ========================== */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    /**
     * FIX: JJWT deserializes JSON numbers as Integer when they fit in int range.
     * Safely handle both Integer and Long to avoid ClassCastException.
     */
    public Long extractUserId(String token) {
        Object val = extractAllClaims(token).get("userId");
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return Long.valueOf(val.toString());
    }

    /**
     * FIX: roleId also suffers the same Integer/Long issue from JJWT deserialization.
     */
    public Long extractRoleId(String token) {
        Object val = extractAllClaims(token).get("roleId");
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return Long.valueOf(val.toString());
    }

    public String extractUsernameFromRefreshToken(String token) {
        // refresh token has "userName" claim added now — this will work correctly
        return extractUsername(token);
    }

    /* ==========================
       CONTEXT ACCESS METHODS
       ========================== */
    public String getUsername() {
        return extractUsername(getTokenFromHeader());
    }

    public String getRole() {
        return extractRole(getTokenFromHeader());
    }

    public Long getUserId() {
        return extractUserId(getTokenFromHeader());
    }

    public Long getRoleId() {
        return extractRoleId(getTokenFromHeader());
    }

    /* ==========================
       VALIDATION
       ========================== */
    public boolean validateTokenForExpire() {
        return validateToken(getTokenFromHeader(), getUsername());
    }

    public boolean validateToken(String token, String userName) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername != null
                && extractedUsername.equals(userName)
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public Claims extractAllClaims(String token) {
        // FIX: Use Keys.hmacShaKeyFor() consistently — matches how JwtUtil signs tokens
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /* ==========================
       TOKEN GENERATION METHODS
       (kept here for any utility use — main generation is in JwtUtil)
       ========================== */
    public String generateAccessToken(Long userId, String username, String role, Long roleId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userName", username);
        claims.put("role", role);
        claims.put("roleId", roleId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userName", username);
        claims.put("role", role);
        claims.put("type", "REFRESH");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
                .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }
}

