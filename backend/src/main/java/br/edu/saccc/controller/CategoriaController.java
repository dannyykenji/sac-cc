package br.edu.saccc.controller;

import br.edu.saccc.repository.CategoriaAtendimentoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Categorias ativas para o formulário de agendamento (#N8, RF41). */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaAtendimentoRepository repository;

    public CategoriaController(CategoriaAtendimentoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> listar() {
        return repository.findByAtivoTrueOrderByNome().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "nome", c.getNome()))
                .toList();
    }
}
