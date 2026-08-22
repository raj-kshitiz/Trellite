package com.example.trellite.service;

import com.example.trellite.dto.UserRegisterDTO;
import com.example.trellite.dto.UserResponseDTO;
import com.example.trellite.dto.UserSummaryDTO;
import com.example.trellite.enums.Role;
import com.example.trellite.exception.ConflictException;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthRepo authRepo;
    private final BCryptPasswordEncoder encoder;

    public AuthService(AuthRepo authRepo) {
        this.authRepo = authRepo;
        encoder = new BCryptPasswordEncoder(12);
    }

    private UserResponseDTO mapToUserResponseDTO(User user) {
        return new UserResponseDTO(
                user.getUsername(),
                user.getEmail()
        );
    }

    private UserSummaryDTO mapToUserSummaryDTO(User user) {
        return new UserSummaryDTO(user.getId(), user.getUsername(), user.getEmail());
    }

    public User requireUser(String username) {
        return authRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserSummaryDTO whoAmI(String username) {
        return mapToUserSummaryDTO(requireUser(username));
    }

    /**
     * Exact-match lookup so a board owner can turn a username or email into the id the
     * membership endpoints need. Deliberately not a prefix/substring search: that would
     * turn this into a directory anyone with an account could enumerate.
     */
    public List<UserSummaryDTO> findUser(String query) {
        String trimmed = query.trim();
        Optional<User> match = authRepo.findByUsernameIgnoreCase(trimmed);
        if (match.isEmpty()) {
            match = authRepo.findByEmailIgnoreCase(trimmed);
        }
        return match.map(user -> List.of(mapToUserSummaryDTO(user))).orElseGet(List::of);
    }

    public UserResponseDTO register(UserRegisterDTO userRegisterDTO) {
        // Checked here rather than left to the DB: duplicate usernames used to be allowed,
        // and two rows with one name break login for BOTH accounts, permanently —
        // findByUsername returns a single Optional and Spring Data throws on two rows.
        if (authRepo.existsByUsernameIgnoreCase(userRegisterDTO.username())) {
            throw new ConflictException("That username is already taken");
        }
        if (authRepo.existsByEmailIgnoreCase(userRegisterDTO.email())) {
            throw new ConflictException("An account with that email already exists");
        }

        User user = User.builder()
                .username(userRegisterDTO.username())
                .email(userRegisterDTO.email())
                .password(encoder.encode(userRegisterDTO.password()))
                .role(Role.USER)
                .build();
        return mapToUserResponseDTO(authRepo.save(user));
    }
}
