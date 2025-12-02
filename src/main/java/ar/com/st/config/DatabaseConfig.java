package ar.com.st.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuración de base de datos
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Configuration
@EnableJpaRepositories(basePackages = "ar.com.st.repository")
@EntityScan(basePackages = "ar.com.st.entity")
@EnableTransactionManagement
public class DatabaseConfig {
}
