package com.dandragu.saasetl.identity.infrastructure.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;

import com.dandragu.saasetl.identity.application.UserRepository;
import com.dandragu.saasetl.company.application.CompanyAccessService;
import com.dandragu.saasetl.company.infrastructure.context.ActiveCompanySession;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfiguration {

	@Bean
	AuthenticationManager authenticationManager(UserAuthenticationProvider provider) {
		return new ProviderManager(provider);
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository(
			@Value("${nexo.security.cookie-secure:false}") boolean cookieSecure) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie
				.path("/")
				.sameSite("Lax")
				.secure(cookieSecure));
		return repository;
	}

	@Bean
	DefaultCookieSerializer cookieSerializer(
			@Value("${nexo.security.cookie-secure:false}") boolean cookieSecure) {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("NEXO_SESSION");
		serializer.setCookiePath("/");
		serializer.setUseHttpOnlyCookie(true);
		serializer.setUseSecureCookie(cookieSecure);
		serializer.setSameSite("Lax");
		return serializer;
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(
				new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		return new CompositeSessionAuthenticationStrategy(List.of(
				new ChangeSessionIdAuthenticationStrategy(),
				new CsrfAuthenticationStrategy(csrfTokenRepository)));
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			AuthenticationManager authenticationManager,
			ObjectMapper objectMapper,
			SecurityContextRepository securityContextRepository,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			CsrfTokenRepository csrfTokenRepository,
			SecurityJsonResponseWriter responseWriter,
			JsonAuthenticationFailureHandler failureHandler,
			JsonAuthenticationEntryPoint authenticationEntryPoint,
			JsonAccessDeniedHandler accessDeniedHandler,
			UserRepository userRepository,
			CompanyAccessService companyAccessService,
			ActiveCompanySession activeCompanySession) throws Exception {
		JsonLoginAuthenticationFilter loginFilter = new JsonLoginAuthenticationFilter(
				authenticationManager, objectMapper);
		loginFilter.setSecurityContextRepository(securityContextRepository);
		loginFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
		loginFilter.setAuthenticationSuccessHandler(
				new JsonAuthenticationSuccessHandler(
						responseWriter,
						csrfTokenRepository,
						companyAccessService,
						activeCompanySession));
		loginFilter.setAuthenticationFailureHandler(failureHandler);

		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository)
						.requireExplicitSave(true))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation(fixation -> fixation.changeSessionId()))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("NEXO_SESSION")
						.logoutSuccessHandler(new JsonLogoutSuccessHandler(csrfTokenRepository)))
				.authorizeHttpRequests(authorize -> authorize
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/api/health", "/api/auth/csrf", "/api/auth/login").permitAll()
						.requestMatchers("/api/etl/executions/**").denyAll()
						.requestMatchers(
								"/api/auth/session",
								"/api/auth/session/company",
								"/api/auth/companies",
								"/api/auth/logout",
								"/api/csv/**").authenticated()
						.anyRequest().denyAll())
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				.addFilterAfter(
						new ActiveUserValidationFilter(userRepository, authenticationEntryPoint),
						SecurityContextHolderFilter.class)
				.addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
