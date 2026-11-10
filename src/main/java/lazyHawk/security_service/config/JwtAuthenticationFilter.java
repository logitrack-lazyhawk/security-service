package lazyHawk.security_service.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lazyHawk.security_service.servicio.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String xAuthUser = request.getHeader("X-Auth-User");
        final String xAuthRoles = request.getHeader("X-Auth-Roles");

        // 1. Si la petición ya fue autenticada e inyectada por el API Gateway
        if (xAuthUser != null && !xAuthUser.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (xAuthRoles != null && !xAuthRoles.isBlank()) {
                for (String role : xAuthRoles.split(",")) {
                    String trimmed = role.trim();
                    if (!trimmed.isEmpty()) {
                        String authRole = trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
                        authorities.add(new SimpleGrantedAuthority(authRole));
                    }
                }
            }
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    xAuthUser, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Si viene con cabecera Authorization directa (Bearer token)
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7).trim();
        if (jwt.isEmpty()) {
            SecurityErrorResponse.escribir(request, response, "auth.token.missing");
            return;
        }
        try {
            String userEmail = jwtService.extractUsername(jwt);
            if (userEmail == null || userEmail.isBlank()) {
                SecurityErrorResponse.escribir(request, response, "auth.token.invalid");
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (!jwtService.isTokenValid(jwt, userDetails) || !userDetails.isEnabled()
                        || !userDetails.isAccountNonLocked() || !userDetails.isAccountNonExpired()
                        || !userDetails.isCredentialsNonExpired()) {
                    SecurityErrorResponse.escribir(request, response, "auth.token.invalid");
                    return;
                }
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | IllegalArgumentException | AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            SecurityErrorResponse.escribir(request, response, "auth.token.invalid");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
