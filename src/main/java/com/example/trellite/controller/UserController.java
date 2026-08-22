package com.example.trellite.controller;

import com.example.trellite.dto.UserSummaryDTO;
import com.example.trellite.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Turning a person into a user id, so a board owner can add someone by name. Exact match
 * only, and authenticated like everything else — a substring search would make the whole
 * user table enumerable by any account.
 */
@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    // 200 with an empty array rather than the 204 the collection endpoints use: "nobody
    // by that name" is a normal answer to a search, and a client should not have to treat
    // it as a special no-body case.
    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryDTO>> search(
            @RequestParam @NotBlank(message = "Search query cannot be empty") String q
    ) {
        return new ResponseEntity<>(authService.findUser(q), HttpStatus.OK);
    }
}
