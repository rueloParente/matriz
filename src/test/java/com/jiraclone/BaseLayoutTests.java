package com.jiraclone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class BaseLayoutTests extends AbstractIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void layoutLoadsLocalStylesHtmxAndSortable() throws Exception {
		mvc.perform(get("/login")).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("href=\"/css/app.css\""),
						org.hamcrest.Matchers.containsString("src=\"/js/htmx.min.js\""),
						org.hamcrest.Matchers.containsString("src=\"/js/sortable.min.js\""),
						org.hamcrest.Matchers.containsString("src=\"/js/theme.js\""),
						org.hamcrest.Matchers.containsString("id=\"theme-toggle\""),
						org.hamcrest.Matchers.containsString("aria-label=\"Switch to dark mode\""),
						org.hamcrest.Matchers.containsString("id=\"navbar-slot\""),
						org.hamcrest.Matchers.containsString("<title>Log in</title>"))));
	}

	@Test
	void loginCanLoadLocalAssetsWithoutAuthentication() throws Exception {
		mvc.perform(get("/css/app.css")).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/css"));
		mvc.perform(get("/js/htmx.min.js")).andExpect(status().isOk());
		mvc.perform(get("/js/sortable.min.js")).andExpect(status().isOk());
		mvc.perform(get("/js/theme.js")).andExpect(status().isOk());
	}

	@Test
	void themeControlIsSharedByGuestAndAuthenticatedPagesAndScriptLoadsBeforeStyles() throws Exception {
		for (String path : new String[] { "/", "/login", "/register" }) {
			var html = mvc.perform(get(path)).andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			assertThat(html).contains("id=\"theme-toggle\"", "aria-label=\"Switch to dark mode\"");
			assertThat(html.indexOf("id=\"theme-toggle\"")).isGreaterThan(html.indexOf("href=\"/register\""));
			assertThat(html.indexOf("src=\"/js/theme.js\"")).isLessThan(html.indexOf("href=\"/css/app.css\""));
		}
		var html = mvc.perform(get("/").with(user("alice"))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(html).contains("id=\"theme-toggle\"", "aria-label=\"Switch to dark mode\"");
		assertThat(html.indexOf("id=\"theme-toggle\"")).isGreaterThan(html.indexOf("action=\"/logout\""));
	}
}
