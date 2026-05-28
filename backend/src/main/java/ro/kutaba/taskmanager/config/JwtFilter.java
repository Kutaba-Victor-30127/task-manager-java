package ro.kutaba.taskmanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ro.kutaba.taskmanager.service.JwtService;

import java.io.IOException;
import java.util.List;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter{
    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService){
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException{
        
        String path = request.getRequestURI();
        //permitem login/register/swagger
       if (path.contains("/auth") ||
            path.contains("/swagger") ||
            path.contains("/swagger-ui") ||
            path.contains("/v3/api-docs")) {

            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");    

        // daca nu exista header mergi mai departe
        if (authHeader == null || !authHeader.startsWith("Bearer")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try{
            //validare
            if (jwtService.validateToken(token) && SecurityContextHolder.getContext().getAuthentication() == null){
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);

                // cream authority
                List<SimpleGrantedAuthority> authorities = 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

                // cream authentication
                UsernamePasswordAuthenticationToken auth = 
                        new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                        );
                
                //punem userul in context
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        }catch (Exception e){
            //token invalid ignoram si continuam
        }
            
        //continua request
        filterChain.doFilter(request,response);
        
    }
}
