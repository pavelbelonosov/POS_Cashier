package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.service.CustomUserDetailService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class CustomUserDetailServiceTest {

    private CustomUserDetailService customUserDetailService;
    private AccountRepository accountRepository = Mockito.mock(AccountRepository.class);

    @Before
    public void setUp(){
        customUserDetailService = new CustomUserDetailService(accountRepository);
    }

    @Test
    public void givenExistingUser_whenLoadUserByUsername_thenReturnsUserDetails(){
        Account user = createUser();
        UserDetails userDetails = customUserDetailService.loadUserByUsername(user.getUsername());
        assertTrue(userDetails.getUsername().equals(user.getUsername()));
        assertTrue(userDetails.getPassword().equals(user.getPassword()));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority(user.getAuthority().toString())));
    }

    @Test(expected = UsernameNotFoundException.class)
    public void givenNonExistingUser_whenLoadUserByUsername_thenReturnsUserDetails(){
        createUser();
        customUserDetailService.loadUserByUsername("wrongUser");
    }

    private Account createUser(){
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        user.setAuthority(Authority.ADMIN);
        user.setId(Math.abs(new Random().nextLong()));
        user.setAccountInfo(new AccountInfo());
        Mockito.when(accountRepository.findByUsername(user.getUsername())).thenReturn(user);
        Mockito.when(accountRepository.findByUsername(AdditionalMatchers.not(eq(user.getUsername())))).thenReturn(null);
        return user;
    }

}
