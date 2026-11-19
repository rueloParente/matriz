package com.jiraclone.common;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.validation.annotation.Validated;

@Configuration
@Profile("observability")
@EnableConfigurationProperties(ObservabilityConfig.MetricsCredentials.class)
class ObservabilityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain metricsSecurityFilterChain(HttpSecurity http, MetricsCredentials credentials,
			PasswordEncoder encoder) {
		var user = User.withUsername(credentials.username())
				.password(encoder.encode(credentials.password()))
				.roles("METRICS").build();
		var provider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(user));
		provider.setPasswordEncoder(encoder);

		http.securityMatcher("/actuator/prometheus")
				.authenticationManager(new ProviderManager(provider))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/actuator/prometheus").hasRole("METRICS")
						.anyRequest().denyAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.requestCache(cache -> cache.disable())
				.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	@Validated
	@ConfigurationProperties("matriz.observability")
	record MetricsCredentials(@NotBlank String username, @NotBlank String password) {
	}
}
