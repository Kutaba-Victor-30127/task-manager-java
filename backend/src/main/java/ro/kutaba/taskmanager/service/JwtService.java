package ro.kutaba.taskmanager.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import ro.kutaba.taskmanager.model.User;

import java.security.Key;
import java.util.Date;
import java.util.Base64;

@Service
public class JwtService{
     
    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init(){
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        key = Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(User user){
        return Jwts.builder()
                   .setSubject(user.getUsername())
                   .claim("role", user.getRole().name())
                   .setIssuedAt(new Date())
                   .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) //1h
                   .signWith(key, SignatureAlgorithm.HS256)
                   .compact();        
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject(); 
    }

    public String extractRole(String token){
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token){
        try{
            parseClaims(token);
            return true;
        }catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e){
            return false;
        }
            
    }
    
    //refractor clean
    private Claims parseClaims(String token){
        return Jwts.parserBuilder()
                   .setSigningKey(key)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

}