package cl.duoc.pedidos360.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Configuración TEMPORAL de Spring Security.
 *
 * Por ahora dejamos todo abierto para poder probar la
 * comunicación Angular <-> Spring Boot en local, sin
 * pedir autenticación todavía.
 *
 * En el siguiente paso vamos a reemplazar esto por un
 * filtro que valide el JWT emitido por Microsoft Entra ID
 * (oauth2ResourceServer con JWT), que es lo que pide la
 * pauta de la evaluación.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
