package com.app.bank_acquiring.config;

import com.app.bank_acquiring.domain.account.Authority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService customUserDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //enable h2-console
        http.csrf().disable().formLogin().permitAll();
        http.headers().frameOptions().sameOrigin();

        http.authorizeRequests()
                .antMatchers("/", "/images/**", "/accounts/registration").permitAll()
                .antMatchers("/h2-console", "/h2-console/**").permitAll()
                //.antMatchers("/h2-console","/h2-console/**").hasAuthority("ADMIN")
                .antMatchers("/shops", "/shop/**", "/accounts", "/terminals", "/terminals/**").hasAuthority("ADMIN")
                .antMatchers("/main").hasAnyAuthority(Authority.CASHIER.toString(),
                        Authority.HEAD_CASHIER.toString(), Authority.ADMIN.toString())
                .antMatchers("/products", "/products/**").hasAnyAuthority(Authority.HEAD_CASHIER.toString(),
                        Authority.ADMIN.toString())
                .anyRequest().authenticated();

        http
                .formLogin()
                .loginPage("/login.html")
                .failureUrl("/login-error.html")
                .and()
                .logout()
                .logoutSuccessUrl("/login.html");
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
