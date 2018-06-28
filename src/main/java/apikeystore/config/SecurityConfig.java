package apikeystore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
// @EnableOAuth2Sso
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	/*
	 * // OAuth2 Configuration
	 * 
	 * @Override public void configure(HttpSecurity http) throws Exception {
	 * http.antMatcher("/**").authorizeRequests().antMatchers("/",
	 * "/login**").permitAll().anyRequest().authenticated() .and().csrf().disable();
	 * }
	 */

	// Basic Auth configuration
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.authorizeRequests()
				.anyRequest()
				.fullyAuthenticated()
				.and()
			.httpBasic()
				.and()
			.csrf()
				.disable();
		// @formatter:on
	}

	// Swagger documentation
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/v2/api-docs", "/configuration/ui/**", "/swagger-resources/**",
				"/configuration/security/**", "/swagger-ui.html", "/webjars/**");
	}
}
