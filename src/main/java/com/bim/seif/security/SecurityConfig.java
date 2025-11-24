package com.bim.seif.security;

import com.bim.seif.config.AuthEntryPoint;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.ldap.LdapPasswordComparisonAuthenticationManagerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthEntryPoint authEntryPoint;
    private final Environment env;

    private final RevokedJwtFilter revokedJwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS y CSRF
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)

                // Manejo de excepciones (tu entry point)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))

                // Autorizaciones (fusiona tus paths permitidos)
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/int/**").permitAll()
                        .antMatchers("/auth/**").permitAll()
                        .antMatchers("/password-policy/public").permitAll()
                        .antMatchers("/forgotPass/**").permitAll()
                        .antMatchers("/seif/cambio-contrasena/**").permitAll()
                        .antMatchers("/password-reset/verify").permitAll()
                        .antMatchers("/password-reset/confirm").permitAll()
                        .antMatchers("/archivos/**").permitAll()
                        .antMatchers("/docs/**").permitAll()
                        .antMatchers("/actuator/**").permitAll()
                        .antMatchers("/metrics/**").permitAll()
                        .antMatchers("/swagger-ui/**").permitAll()
                        .antMatchers("/error", "/int/index.html", "/favicon.ico", "/assets/**",
                                "/static/**", "/**/*.js", "/**/*.css", "/**/*.png",
                                "/**/*.jpg", "/**/*.svg")
                        .permitAll()
                        .anyRequest().authenticated())

                // HTTP Basic (si lo usas para algo puntual)
                .httpBasic(Customizer.withDefaults())

                // Headers (sameOrigin para iframes como en tu config)
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))

                // Stateless (usamos tokens)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Resource server con JWT (para validar los access tokens)
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        // Filtro que invalida tokens en blacklist (después de la auth básica)
        http.addFilterAfter(revokedJwtFilter,
                org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object p = jwt.getClaim("p");

            List<String> roleNames = new ArrayList<>();

            if (p instanceof String s) {
                roleNames.add(s);
            } else if (p instanceof Collection<?> col) {
                for (Object o : col) {
                    if (o instanceof String s2) {
                        roleNames.add(s2);
                    } else if (o instanceof Map<?, ?> m) { // por si vienen objetos {cve, descripcion}
                        Object cve = m.get("cve");
                        if (cve != null)
                            roleNames.add(cve.toString());
                    }
                }
            } else if (p instanceof Map<?, ?> m) { // tu caso: objeto con cve
                Object cve = m.get("cve");
                if (cve != null)
                    roleNames.add(cve.toString());
            }

            return roleNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.startsWith("ROLE_") ? s : "ROLE_" + s) // normaliza
                    .map(SimpleGrantedAuthority::new) // -> SimpleGrantedAuthority
                    .map(a -> (GrantedAuthority) a) // asegura tipo base
                    .collect(Collectors.toList()); // List<GrantedAuthority>
        });
        return conv;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // LDAP
    @Bean
    LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(env.getRequiredProperty("spring.ldap.url"));
        contextSource.setBase(env.getRequiredProperty("spring.ldap.base"));
        contextSource.setPooled(true);
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
        return new LdapTemplate(contextSource());
    }

    @Bean
    LdapAuthoritiesPopulator authoritiesPopulator() {
        String groupSearchBase = "ou=roles";
        DefaultLdapAuthoritiesPopulator authorities = new DefaultLdapAuthoritiesPopulator(contextSource(),
                groupSearchBase);
        authorities.setGroupSearchFilter("member={0}");
        return authorities;
    }

    @Bean
    public FilterBasedLdapUserSearch userSearch() {
        return new FilterBasedLdapUserSearch("ou=empleados", "(uid={0})", contextSource());
    }

    @Bean
    public UserDetailsService ldapUserDetailsService() {
        LdapUserDetailsService service = new LdapUserDetailsService(userSearch(), authoritiesPopulator());
        // Si luego mapeas atributos personalizados:
        // service.setUserDetailsContextMapper(customMapper);
        return service;
    }

    @Bean
    AuthenticationManager authenticationManager(BaseLdapPathContextSource contextSource,
            LdapAuthoritiesPopulator authorities) {

        LdapPasswordComparisonAuthenticationManagerFactory factory = new LdapPasswordComparisonAuthenticationManagerFactory(
                contextSource, new LdapShaPasswordEncoder());

        factory.setUserSearchBase("ou=empleados");
        factory.setUserSearchFilter("(uid={0})");
        factory.setPasswordAttribute("userPassword");
        factory.setLdapAuthoritiesPopulator(authorities);
        return factory.createAuthenticationManager();
    }
}