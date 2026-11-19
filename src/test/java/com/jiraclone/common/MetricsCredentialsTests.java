package com.jiraclone.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MetricsCredentialsTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(CredentialsBinding.class);

	@Test
	void missingCredentialsFailStartup() {
		runner.run(context -> assertThat(context).hasFailed()
				.getFailure().hasRootCauseInstanceOf(BindValidationException.class));
	}

	@Test
	void blankUsernameOrPasswordFailsStartup() {
		for (String property : new String[] {"username", "password"}) {
			runner.withPropertyValues("matriz.observability.username=prometheus",
							"matriz.observability.password=local-test-password",
							"matriz.observability." + property + "= ")
					.run(context -> assertThat(context).hasFailed()
							.getFailure().hasRootCauseInstanceOf(BindValidationException.class));
		}
	}

	@Test
	void validCredentialsBind() {
		runner.withPropertyValues("matriz.observability.username=prometheus",
						"matriz.observability.password=local-test-password")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(ObservabilityConfig.MetricsCredentials.class).username())
							.isEqualTo("prometheus");
				});
	}

	@TestConfiguration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ObservabilityConfig.MetricsCredentials.class)
	static class CredentialsBinding {
	}
}
