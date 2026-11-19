package br.edu.saccc.security;

import br.edu.saccc.model.PerfilAcesso;
import br.edu.saccc.model.StatusUsuario;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Carrega o usuário pelo e-mail e materializa authorities:
 * ROLE_<PERFIL> + uma authority por permissão (RF06, RNF17).
 */
@Service
public class UsuarioUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioUserDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario u = repository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        PerfilAcesso perfil = u.getPerfil();
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + perfil.getNome()));
        perfil.getPermissoes().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getCodigo())));

        boolean ativo = u.getStatus() == StatusUsuario.ATIVO;
        return new User(u.getEmail(), u.getSenhaHash(), ativo, true, true, true, authorities);
    }
}
