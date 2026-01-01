package com.fitfamily.app.security;

import com.fitfamily.app.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

	// Annotate with @Value to prevent secret from being logged
	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration}")
	private long jwtExpiration;
	
	// Override toString to prevent accidental secret exposure in logs
	@Override
	public String toString() {
		return "JwtUtil{secretKey=***REDACTED***, jwtExpiration=" + jwtExpiration + "}";
	}

	/**
	 * Generate JWT token for a user
	 */
	public String generateToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("userId", user.getId().toString());
		claims.put("role", user.getRole().toString());
		
		return Jwts.builder()
				.claims(claims)
				.subject(user.getEmail())
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + jwtExpiration))
				.signWith(getSignInKey())
				.compact();
	}

	/**
	 * Extract email from JWT token
	 */
	public String extractEmail(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	/**
	 * Extract user ID from JWT token
	 */
	public String extractUserId(String token) {
		return extractClaim(token, claims -> claims.get("userId", String.class));
	}

	/**
	 * Extract role from JWT token
	 */
	public String extractRole(String token) {
		return extractClaim(token, claims -> claims.get("role", String.class));
	}

	/**
	 * Validate JWT token
	 */
	public boolean validateToken(String token) {
		try {
			extractAllClaims(token);
			return !isTokenExpired(token);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Validate token against user email
	 */
	public boolean validateToken(String token, String email) {
		final String tokenEmail = extractEmail(token);
		return (tokenEmail.equals(email) && !isTokenExpired(token));
	}

	/**
	 * Extract a specific claim from token
	 */
	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	/**
	 * Extract all claims from token
	 */
	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * Check if token is expired
	 */
	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	/**
	 * Extract expiration date from token
	 */
	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	/**
	 * Get signing key from secret
	 */
	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}

}

