package br.edu.saccc.dto;

import java.util.List;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String perfil,
        List<String> permissoes) {

    public static UsuarioResponse de(br.edu.saccc.model.Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getPerfil().getNome(),
                u.getPerfil().getPermissoes().stream()
                        .map(br.edu.saccc.model.Permissao::getCodigo)
                        .sorted()
                        .toList());
    }
}
