package br.edu.saccc.controller;

import br.edu.saccc.dto.LoginRequest;
import br.edu.saccc.dto.UsuarioResponse;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.UsuarioRepository;
import br.edu.saccc.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/** UC01 — autenticação com sessão de servidor (cookie httpOnly, ADR-004). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final br.edu.saccc.security.LoginTentativaService tentativas;
    private final AuditoriaService auditoria;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioRepository usuarioRepository,
                          br.edu.saccc.security.LoginTentativaService tentativas,
                          AuditoriaService auditoria) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.tentativas = tentativas;
        this.auditoria = auditoria;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String chave = "login:" + req.email().toLowerCase() + ":" + request.getRemoteAddr();

        if (tentativas.bloqueado(chave)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(tentativas.segundosRestantes(chave)))
                    .body(Map.of("title", "Muitas tentativas",
                            "detail", "Aguarde alguns minutos antes de tentar novamente."));
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            req.email(), req.senha()));

            // Sessão nova a cada login (prevenção contra fixação de sessão)
            HttpSession anterior = request.getSession(false);
            if (anterior != null) anterior.invalidate();
            request.getSession(true);
            request.changeSessionId();

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, request, null);

            tentativas.limpar(chave);

            Usuario u = usuarioRepository.findByEmailIgnoreCase(req.email()).orElseThrow();
            u.setDataUltimoAcesso(LocalDateTime.now());
            usuarioRepository.save(u);

            auditoria.registrar("LOGIN", "usuario", u.getId(), u.getId(), request, null);
            return ResponseEntity.ok(UsuarioResponse.de(u));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "title", "Conta não habilitada",
                    "detail", "Sua conta está inativa ou aguardando aprovação. Procure a coordenação."));
        } catch (BadCredentialsException | LockedException e) {
            tentativas.registrarFalha(chave);
            auditoria.registrar("LOGIN_FALHA", "usuario", null, null, request, "credenciais inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "title", "Credenciais inválidas",
                    "detail", "E-mail ou senha incorretos."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Long uid = null;
            var principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                uid = usuarioRepository.findByEmailIgnoreCase(ud.getUsername()).map(Usuario::getId).orElse(null);
            }
            auditoria.registrar("LOGOUT", "usuario", uid, uid, request, null);
        }
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .map(UsuarioResponse::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
