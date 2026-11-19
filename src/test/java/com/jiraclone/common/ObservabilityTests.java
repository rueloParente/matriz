package com.jiraclone.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jiraclone.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("observability")
@AutoConfigureMetrics(export = true)
@TestPropertySource(properties = {
		"METRICS_USERNAME=prometheus",
		"METRICS_PASSWORD=local-test-metrics-password"
})
class ObservabilityTests extends AbstractIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void scrapeRequiresDedicatedBasicCredentialsForOrdinaryAndHtmxRequests() throws Exception {
		for (boolean htmx : new boolean[] {false, true}) {
			mvc.perform(get("/actuator/prometheus").header("HX-Request", htmx))
					.andExpect(status().isUnauthorized())
					.andExpect(header().doesNotExist("Location"))
					.andExpect(header().doesNotExist("HX-Redirect"));
			mvc.perform(get("/actuator/prometheus").header("HX-Request", htmx)
							.with(httpBasic("prometheus", "wrong")))
					.andExpect(status().isUnauthorized());
			mvc.perform(get("/actuator/prometheus").header("HX-Request", htmx)
							.with(httpBasic("alice", "password")))
					.andExpect(status().isUnauthorized());
		}
	}

	@Test
	void successfulScrapeContainsApplicationAndJvmMetricsWithoutCreatingSession() throws Exception {
		var result = mvc.perform(get("/actuator/prometheus")
						.with(httpBasic("prometheus", "local-test-metrics-password")))
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString())
				.contains("jvm_memory_used_bytes", "application=\"matriz\"", "process_cpu_usage",
						"hikaricp_connections_active");
		assertThat(result.getRequest().getSession(false)).isNull();
	}

	@Test
	void browserSessionDoesNotGrantMetricsAccess() throws Exception {
		var result = mvc.perform(post("/login").with(csrf())
						.param("username", "alice").param("password", "password"))
				.andExpect(status().isFound()).andReturn();
		var session = (MockHttpSession) result.getRequest().getSession(false);
		mvc.perform(get("/projects").session(session)).andExpect(status().isOk());
		mvc.perform(get("/actuator/prometheus").session(session)).andExpect(status().isUnauthorized());
		mvc.perform(get("/actuator/env").session(session)).andExpect(status().isForbidden());
	}

	@Test
	void metricsCredentialsCannotLogInOrAccessBrowserRoutes() throws Exception {
		mvc.perform(post("/login").with(csrf()).param("username", "prometheus")
						.param("password", "local-test-metrics-password"))
				.andExpect(redirectedUrl("/login?error"));
		mvc.perform(get("/projects").with(httpBasic("prometheus", "local-test-metrics-password")))
				.andExpect(redirectedUrl("/login"));
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(post("/actuator/prometheus")
						.with(httpBasic("prometheus", "local-test-metrics-password")))
				.andExpect(status().isForbidden());
	}
}
