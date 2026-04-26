package com.example.trellite.service;

import com.example.trellite.repository.AuthRepo;
import com.example.trellite.model.User;
import com.example.trellite.model.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    final
    AuthRepo authRepo;

    public MyUserDetailsService(AuthRepo authRepo) {
        this.authRepo = authRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = authRepo.findByUsername(username);
        if(user == null){
            System.out.println("User not found - 404");
            throw new UsernameNotFoundException("User not found");
        }
        return new UserPrincipal(user);
    }
}
