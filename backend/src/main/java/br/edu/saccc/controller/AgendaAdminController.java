package br.edu.saccc.controller;

import br.edu.saccc.dto.AgendamentoResponse;
import br.edu.saccc.model.Agendamento;
import br.edu.saccc.model.ModalidadeAtendimento;
import br.edu.saccc.model.StatusAgendamento;
import br.edu.saccc.repository.AgendamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * UC07 — agenda administrativa: filtros combinados + paginação (RNF12).
 * Filtros via Specification: só entram na consulta os predicados presentes,
 * o que evita o bug do driver PG de inferir bytea/desconhecido para
 * parâmetro nulo em JPQL ("(:x is null or ...)" — visto no E2E 2026-11-05).
 */
@RestController
@RequestMapping("/api/admin/agenda")
public class AgendaAdminController {

    private final AgendamentoRepository agendamentos;

    public AgendaAdminController(AgendamentoRepository agendamentos) {
        this.agendamentos = agendamentos;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public Page<AgendamentoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) ModalidadeAtendimento modalidade,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = {"data", "horaInicio"}, direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        String termo = busca == null || busca.isBlank() ? null : busca.trim().toLowerCase();

        Specification<Agendamento> spec = Specification.where(null);
        if (de != null) spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("data"), de));
        if (ate != null) spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("data"), ate));
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (modalidade != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("modalidade"), modalidade));
        if (categoriaId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("categoriaId"), categoriaId));
        if (termo != null) {
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("assunto")), "%" + termo + "%"));
        }
        return agendamentos.findAll(spec, pageable).map(AgendamentoResponse::de);
    }
}
