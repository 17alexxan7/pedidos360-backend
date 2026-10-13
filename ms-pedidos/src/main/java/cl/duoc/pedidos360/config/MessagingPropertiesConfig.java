package cl.duoc.pedidos360.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra MessagingProperties para poder inyectarla donde se necesite. */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingPropertiesConfig {
}