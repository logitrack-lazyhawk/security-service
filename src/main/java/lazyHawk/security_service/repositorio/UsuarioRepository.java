package lazyHawk.security_service.repositorio;

import lazyHawk.security_service.dominio.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
/**
 * Repositorio para la gestión de persistencia de la entidad Usuario.
 *
 * @author leoM2022
 * @version 1.0.0
 */

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
