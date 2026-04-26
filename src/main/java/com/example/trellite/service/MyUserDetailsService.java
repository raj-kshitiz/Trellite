package org.noobie.springsecdemo.service;

import org.noobie.springsecdemo.dao.UserRepo;
import org.noobie.springsecdemo.model.User;
import org.noobie.springsecdemo.model.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    final
    UserRepo userRepo;

    public MyUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username);
        if(user == null){
            System.out.println("User not found - 404");
            throw new UsernameNotFoundException("User not found");
        }
        return new UserPrincipal(user);
    }
}
