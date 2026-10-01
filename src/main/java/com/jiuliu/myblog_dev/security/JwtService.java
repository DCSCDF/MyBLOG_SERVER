package com.jiuliu.myblog_dev.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and parses stateless JWTs (HS256).
 *
 * <p>Expiry: "remember me" logins get
 * {@code app.jwt.expiration-remember-seconds} (default 30 days); other
 * logins get {@code app.jwt.expiration-seconds} (default 2 hours, a hard
 * expiry since JWTs have no sliding renewal).</p>
 */
@Service
public class JwtService {

    /** Parsed, signature-verified token contents. */
    public record ParsedToken(Long userId, String username, String jti, Date expiresAt) {
    }

    private final SecretKey key;
    private final String issuer;
    private final long expirationSeconds;
    private final long expirationRememberSeconds;
    private final ActiveTokenRegistry tokenRegistry;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer:Myblog_dev}") String issuer,
                      @Value("${app.jwt.expiration-seconds:7200}") long expirationSeconds,
                      @Value("${app.jwt.expiration-remember-seconds:2592000}") long expirationRememberSeconds,
                      ActiveTokenRegistry tokenRegistry) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (env JWT_SECRET) must be at least 32 characters for HS256");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
        this.expirationRememberSeconds = expirationRememberSeconds;
        this.tokenRegistry = tokenRegistry;
    }

    /** Issue a token and register its jti for logout / forced-logout support. */
    public String generateToken(Long userId, String username, boolean rememberMe) {
        long ttl = rememberMe ? expirationRememberSeconds : expirationSeconds;
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttl * 1000L);
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .id(jti)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
        tokenRegistry.register(jti, userId, ttl);
        return token;
    }

    /** Verify signature/expiry and extract claims. Throws JwtException when invalid. */
    public ParsedToken parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new ParsedToken(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.getId(),
                claims.getExpiration());
    }
}
