package br.edu.saccc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Segurança da Fase 1 (ADR-004): sessão de servidor com cookie httpOnly + CSRF para SPA.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// Padrão SPA (documentação Spring Security): token CSRF em cookie legível pelo JS,
		// sem proteção BREACH (o JS lê o valor bruto do cookie e devolve no header).
		CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
		csrfHandler.setCsrfRequestAttributeName(null);

		http
		    .authorizeHttpRequests(auth -> auth
		        .requestMatchers("/api/auth/login", "/api/auth/csrf", "/api/cadastro-aluno").permitAll()
		        .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
		        .requestMatchers("/ws/**").permitAll()   // handshake SockJS (ADR-008); autorização por sessão no broker
		        // shell público da SPA embutida (#T4): HTML/JS/CSS não carregam dados;
		        // /api/** continua exigindo sessão. Em dev o Vite (5173) serve o front.
		        .requestMatchers("/", "/login", "/cadastro", "/cadastro/recebido", "/app/**", "/assets/**", "/favicon.ico", "/index.html").permitAll()
		        .anyRequest().authenticated())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.csrf(csrf -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(csrfHandler))
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable()); // logout manual no AuthController (JSON)
		return http.build();
	}
}
