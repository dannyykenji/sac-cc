package br.edu.saccc.repository;

import br.edu.saccc.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>,
        JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByIdAndStatus(Long id, br.edu.saccc.model.StatusUsuario status);

    long countByPerfilNomeAndStatus(String perfilNome, br.edu.saccc.model.StatusUsuario status);

    /**
     * Lock pessimista NA LINHA DO ALUNO (ADR-005, complemento):
     * serializa agendamentos concorrentes DO MESMO aluno em slots diferentes —
     * sem isso, duas requisições paralelas do mesmo aluno passam nas checagens
     * de RN03 (sobreposição) e #N4 (limite) antes de qualquer INSERT.
     * NATIVO e não JPQL: Usuario é raiz de herança JOINED e o FOR UPDATE do JPQL
     * viraria join entre usuario/aluno/coordenador sem lock efetivo na linha.
     * Alunos distintos travam linhas distintas: zero contenção entre eles.
     */
    @org.springframework.data.jpa.repository.Query(
            value = "select id from usuario where id = :id for update",
            nativeQuery = true)
    Long travarLinha(@org.springframework.data.repository.query.Param("id") Long id);

    /** Listagem com filtros via Specification no UsuarioAdminService
     *  (JPQL com "(:x is null or ...)" quebrava no PG com parâmetro nulo — idem AgendaAdminController). */
}
