package com.example.trellite.config;


import com.example.trellite.service.JwtService;
import com.example.trellite.service.MyUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;

    public JwtFilter(JwtService jwtService, MyUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // "Authorization: Bearer " with nothing after it is a common shape when a
            // frontend interpolates a null/empty token. jjwt asserts hasText() before
            // parsing and throws a plain IllegalArgumentException for it, so this is
            // screened out here rather than relying on the catch below.
            if (token.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }
            try {
                String username = jwtService.extractUserName(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                // Expired, malformed, or badly signed token, or the subject no longer exists.
                // Exceptions thrown here would escape the filter chain as a 500, since
                // @ControllerAdvice does not apply to servlet filters. Instead leave the
                // context unauthenticated and let the entry point in SecurityConfig answer 401,
                // which is the signal the client needs to go and call /auth/refresh.
                //
                // Logged because this catch is deliberately broad: DecodingException (thrown
                // by getKey() when jwt.secret is absent or not valid Base64) is itself a
                // JwtException, so a misconfigured secret would otherwise make every request
                // 401 with nothing explaining why.
                this.logger.debug("Rejecting JWT: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
