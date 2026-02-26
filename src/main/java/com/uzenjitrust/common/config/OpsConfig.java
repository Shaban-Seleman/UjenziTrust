package com.uzenjitrust.common.config;

import com.uzenjitrust.ops.service.OpsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpsProperties.class)
public class OpsConfig {
}
