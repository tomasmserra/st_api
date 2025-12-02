package ar.com.st.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utilidades para manejo de JWT
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Component
@Slf4j
public class JwtUtils {
    
    @Value("${app.jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}") // 24 horas por defecto
    private int jwtExpirationMs;
    
    /**
     * Genera un token JWT a partir de la autenticación
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername());
    }
    
    /**
     * Genera un token JWT a partir del nombre de usuario
     */
    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, getSigningKey())
                .compact();
    }
    
    /**
     * Obtiene el nombre de usuario del token JWT
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    /**
     * Valida un token JWT
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Token JWT inválido: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Claims JWT vacío: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Obtiene la clave de firma
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        
        // Asegurar que la clave tenga al menos 512 bits (64 bytes) para HS512
        if (keyBytes.length < 64) {
            // Si la clave es muy corta, generar una clave segura
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Obtiene la expiración del token en milisegundos
     */
    public int getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}
