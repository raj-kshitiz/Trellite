package com.example.trellite.repository;

import com.example.trellite.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepo extends JpaRepository<User, Integer> {
    User findByUsername(String username);
}
