package com.carepath.vault;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
@Configuration @EnableScheduling @EnableConfigurationProperties({VaultProperties.class,com.carepath.intelligence.ProcessingProperties.class})
public class VaultConfiguration {}
