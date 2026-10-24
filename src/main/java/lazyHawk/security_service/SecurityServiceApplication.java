package lazyHawk.security_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
/**
 * Clase principal de la aplicación Spring Boot.
 * Esta clase se encarga de iniciar la aplicación y habilitar la auditoría JPA.
 * @author leoM2022
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
public class SecurityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityServiceApplication.class, args);
	}

}
