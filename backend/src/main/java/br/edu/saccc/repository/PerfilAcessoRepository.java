package br.edu.saccc.repository;

import br.edu.saccc.model.PerfilAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilAcessoRepository extends JpaRepository<PerfilAcesso, Long> {

    Optional<PerfilAcesso> findByNome(String nome);
}
