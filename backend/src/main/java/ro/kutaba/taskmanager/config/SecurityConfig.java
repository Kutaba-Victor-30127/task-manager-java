package ro.kutaba.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.kutaba.taskmanager.config.JwtFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig{
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter){
        this.jwtFilter = jwtFilter;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
            .cors()
            .and()
            .csrf(csrf -> csrf.disable()) //pt API
            .authorizeHttpRequests(auth -> auth
            // publice
            .requestMatchers("api/auth/**",
                            "api-docs/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger"
                            ).permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            //restul securizate
                            .anyRequest().authenticated()
                            )
                        .addFilterBefore(jwtFilter,
                            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                            );    
                            
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


}
