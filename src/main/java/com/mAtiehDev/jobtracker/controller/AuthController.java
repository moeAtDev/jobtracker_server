package com.mAtiehDev.jobtracker.controller;

import com.mAtiehDev.jobtracker.dto.LoginRequest;
import com.mAtiehDev.jobtracker.model.User;
import com.mAtiehDev.jobtracker.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

@RestController
//@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")

public class AuthController {

    @Autowired
    private UserService userService;

    // ⚠ Replace this with a real key from config or environment variable
    private static final String SECRET = "your-256-bit-secret-your-256-bit-secret"; // Must be 32+ characters
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.login(loginRequest.getEmailOrUsername(), loginRequest.getPassword());

        if (user == null) {
            return ResponseEntity.status(401).body("Invalid email/username or password");
        }

        // ✅ Generate JWT with SecretKey
        //this token will used in frontend and saved in local storage to used to check if user is still 
        //logged in and based some actions happen hide/display some comonenet .....
        String token = Jwts.builder()
        	    .setSubject(user.getUserId())   // ✅ Now subject = userId
        	    .claim("name", user.getFirstName())  // optional
        	    .setIssuedAt(new Date())
        	    .setExpiration(new Date(System.currentTimeMillis() + 86400000))
        	    .signWith(KEY, SignatureAlgorithm.HS256)
        	    .compact();

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user); // Optional: You can send user info to frontend

        return ResponseEntity.ok(response);
    }
    //this api aims to check if token is still valid on front end 
    @PostMapping("/api/auth/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Invalid token format");
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Optionally return some user info from the token
            Map<String, Object> result = new HashMap<>();
            result.put("userId", claims.get("id"));
            result.put("firstName", claims.getSubject());
            result.put("expiresAt", claims.getExpiration());

            return ResponseEntity.ok(result);
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(401).body("Token expired");
        } catch (JwtException e) {
            return ResponseEntity.status(401).body("Invalid token");
        }
    }
}
