package com.jiraclone.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jiraclone.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ObservabilityDisabledTests extends AbstractIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void browserUsersCannotAccessMetricsWithoutProfile() throws Exception {
		mvc.perform(get("/actuator/prometheus").with(user("alice")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
