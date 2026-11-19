package br.edu.saccc.controller;

import br.edu.saccc.dto.AutoCadastroRequest;
import br.edu.saccc.service.AutoCadastroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cadastro-aluno")
public class AutoCadastroController {

    private final AutoCadastroService service;

    public AutoCadastroController(AutoCadastroService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> cadastrar(@Valid @RequestBody AutoCadastroRequest req,
                                                         HttpServletRequest request) {
        service.registrar(req, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensagem",
                        "Cadastro recebido. Após a aprovação da coordenação você poderá entrar."));
    }
}
