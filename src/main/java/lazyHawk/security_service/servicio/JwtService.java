package lazyHawk.security_service.servicio;

import lazyHawk.security_service.dominio.entidad.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de la generación y firma de los tokens JWT.
 *
 * @author leoM2022
 * @version 1.0.0
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${security.jwt.issuer}")
    private String issuer;

    /**
     * Genera un token JWT firmado para el usuario especificado.
     * Incluye los claims obligatorios definidos en el contrato del equipo.
     */
    public String generateToken(Usuario usuario) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", List.of("ROLE_" + usuario.getRol().name()));
        extraClaims.put("customerId", usuario.getEmail());
        return Jwts.builder()
                .claims(extraClaims)
                .subject(usuario.getEmail())
                .issuer(issuer)
                .audience().add("logitrack").and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Decodifica la clave secreta de Base64 y genera la llave HMAC SHA.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}