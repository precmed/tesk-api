package uk.ac.ebi.tsc.tesk.config.security;


import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

/**
 * @author Ania Niewielska <aniewielska@ebi.ac.uk>
 * <p>
 * This class (specifically the @EnableResourceServer annotation)
 * + 2 dependencies: spring-boot-starter-security + spring-security-oauth2-autoconfigure
 * + security.oauth2.resource.user-info-uri property (which points to user info endpoint of the authorisation server)
 * switch on protection of (all minus Swagger) the endpoints via OAuth2 token
 */
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("auth")
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {


    private final ErrorAttributes errorAttributes;
    private final HttpMessageConverters messageConverters;
    private final AuthorisationProperties authorisationProperties;

    public ResourceServerConfiguration(ErrorAttributes errorAttributes, HttpMessageConverters messageConverters,
                                       AuthorisationProperties authorisationProperties) {
        this.errorAttributes = errorAttributes;
        this.messageConverters = messageConverters;
        this.authorisationProperties = authorisationProperties;
    }


    /**
     * We leave the sensible defaults only letting swagger UI requests in
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/", "/v2/api-docs", "/configuration/ui", "/swagger-resources/**", "/configuration/**", "/swagger-ui.html", "/webjars/**").permitAll()
                .anyRequest().authenticated();
    }

    /**
     * Sets a custom entryPoint that returns json in our usual error format with the use of standard Boot's errorAttributes and
     * autoconfigured messageConverters (specifically with autoconfigured objectMapper for JacksonConverter)
     */
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.authenticationEntryPoint(new CustomAuthenticationEntryPoint(errorAttributes, messageConverters));
    }

    /**
     * Magic, that changes default role prefix from ROLE_ to elixir:
     */
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("elixir:");
    }

    /**
     * Custom authorities extractor (we use Elixir roles as authorities)
     */
    @Bean
    public AuthoritiesExtractor authoritiesExtractor() {
        return new GroupNamesSecurityExtractor();
    }

    /**
     * Custom principal extractor (we use the whole userinfo response object as Principal
     */
    @Bean
    public PrincipalExtractor principalExtractor() {
        return new ElixirPrincipalExtractor(authoritiesExtractor(), authorisationProperties);
    }

    /**
     * Switches off default in memory user store containing one user with auto-generated password
     *
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> null;
    }

}
