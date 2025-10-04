package com.example.english_exam.security;

import com.example.english_exam.repositories.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh.expiration}")
    private long jwtRefreshExpiration;

    private SecretKey cachedSigningKey;
    private UserRepository userRepository;

    // Khởi tạo key từ secretKey
    @PostConstruct
    private void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // Tạo Access Token
    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // ở đây = email
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(cachedSigningKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, new HashMap<>());
    }

    // Tạo Refresh Token
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))
                .signWith(cachedSigningKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Trích xuất username từ token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Trích xuất claim cụ thể
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    // Trích xuất tất cả claims
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(cachedSigningKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            return null; // Token không hợp lệ
        }
    }

    // Kiểm tra token hợp lệ
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // Thêm hàm trong JwtService

    // Kiểm tra token có phải Refresh Token không
    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null) return false;

        String type = (String) claims.get("type");
        return "refresh".equals(type);
    }


    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration != null && expiration.before(new Date());
    }

    public String resolveTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if ("accessToken".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    // ✅ Trích xuất userId trực tiếp từ token trong cookie
    public Long extractUserId(HttpServletRequest request) {
        try {
            String token = resolveTokenFromCookie(request);
            if (token == null) {
                throw new RuntimeException("Token not found in cookies");
            }

            // Lấy username (thường là email)
            String username = extractUsername(token);
            if (username == null) {
                throw new RuntimeException("Invalid token: no username");
            }

            // Tìm userId tương ứng trong DB
            return userRepository.findByUserNameOrEmail(username, username)
                    .map(u -> u.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to extract userId from JWT: " + e.getMessage());
        }
    }


}


