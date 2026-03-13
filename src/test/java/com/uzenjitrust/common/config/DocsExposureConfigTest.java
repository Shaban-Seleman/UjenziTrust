package com.uzenjitrust.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DocsExposureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DocsConfig.class, OpenApiConfig.class);

    @Test
    void openApiBeanIsDisabledByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(OpenAPI.class));
    }

    @Test
    void openApiBeanIsEnabledWhenDocsFlagIsTrue() {
        contextRunner
                .withPropertyValues("app.docs.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(OpenAPI.class));
    }
}
