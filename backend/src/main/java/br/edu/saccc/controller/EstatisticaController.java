package br.edu.saccc.controller;

import br.edu.saccc.dto.DistribuicaoItem;
import br.edu.saccc.dto.EvolucaoItem;
import br.edu.saccc.dto.ResumoEstatisticas;
import br.edu.saccc.service.EstatisticaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** UC10 — painel de estatísticas (RF44–RF55). */
@RestController
@RequestMapping("/api/estatisticas")
@PreAuthorize("hasAuthority('CONSULTAR_ESTATISTICAS')")
public class EstatisticaController {

    private final EstatisticaService service;

    public EstatisticaController(EstatisticaService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public ResumoEstatisticas resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long responsavelId) {
        return service.resumo(de, ate, modalidade, categoriaId, responsavelId);
    }

    @GetMapping("/evolucao")
    public List<EvolucaoItem> evolucao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long responsavelId) {
        return service.evolucao(de, ate, modalidade, categoriaId, responsavelId);
    }

    @GetMapping("/distribuicao")
    public List<DistribuicaoItem> distribuicao(
            @RequestParam String dimensao,   // MODALIDADE | CATEGORIA
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long responsavelId) {
        return service.distribuicao(dimensao, de, ate, modalidade, categoriaId, responsavelId);
    }
}
