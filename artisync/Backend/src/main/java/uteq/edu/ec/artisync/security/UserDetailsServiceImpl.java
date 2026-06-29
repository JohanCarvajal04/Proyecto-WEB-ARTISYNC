package uteq.edu.ec.artisync.security;

import uteq.edu.ec.artisync.model.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuario no encontrado con correo: " + correo));

        if (!usuario.getEstadoCuenta()) {
            throw new UsernameNotFoundException("Cuenta deshabilitada para: " + correo);
        }

        return UsuarioPrincipal.build(usuario);
    }
}
