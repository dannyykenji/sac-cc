package br.edu.saccc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Força a emissão do cookie XSRF-TOKEN para a SPA antes do POST de login:
 * ler o valor do token (getToken()) materializa o cookie na resposta.
 */
@RestController
@RequestMapping("/api/auth")
public class CsrfController {

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        token.getToken();
        return ResponseEntity.ok().build();
    }
}
