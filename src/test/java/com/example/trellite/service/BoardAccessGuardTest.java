package com.example.trellite.service;

import com.example.trellite.enums.BoardRole;
import com.example.trellite.exception.ForbiddenException;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.Board;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.BoardRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BoardAccessGuardTest {

    @Mock
    BoardRepo boardRepo;
    @Mock
    AuthRepo userRepo;

    BoardAccessGuard guard;

    User owner = User.builder().id(1L).username("owner").email("owner@x.com").build();
    User member = User.builder().id(2L).username("member").email("member@x.com").build();
    User stranger = User.builder().id(3L).username("stranger").email("stranger@x.com").build();

    Board board;

    @BeforeEach
    void setUp() {
        guard = new BoardAccessGuard(boardRepo, userRepo);
        board = Board.builder()
                .boardId(10)
                .boardName("Board")
                .owner(owner)
                .members(Set.of(member))
                .build();
        lenient().when(userRepo.findByUsername("owner")).thenReturn(Optional.of(owner));
        lenient().when(userRepo.findByUsername("member")).thenReturn(Optional.of(member));
        lenient().when(userRepo.findByUsername("stranger")).thenReturn(Optional.of(stranger));
        lenient().when(boardRepo.findById(10)).thenReturn(Optional.of(board));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void signedInAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, java.util.List.of()));
    }

    @Test
    void ownerAndMemberCanAccessBoard_strangerCannot() {
        assertThat(guard.canAccess(board, owner)).isTrue();
        assertThat(guard.canAccess(board, member)).isTrue();
        assertThat(guard.canAccess(board, stranger)).isFalse();
    }

    @Test
    void memberMayViewBoard() {
        signedInAs("member");
        assertThat(guard.requireVisibleBoard(10)).isSameAs(board);
    }

    @Test
    void strangerIsRefusedTheBoard() {
        signedInAs("stranger");
        assertThatThrownBy(() -> guard.requireVisibleBoard(10))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void memberIsNotTreatedAsOwner() {
        signedInAs("member");
        assertThatThrownBy(() -> guard.requireOwnedBoard(10))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void ownerPassesTheOwnerCheck() {
        signedInAs("owner");
        assertThat(guard.requireOwnedBoard(10)).isSameAs(board);
        assertThat(guard.roleOf(board, owner)).isEqualTo(BoardRole.OWNER);
        assertThat(guard.roleOf(board, member)).isEqualTo(BoardRole.MEMBER);
    }

    @Test
    void missingBoardIsA404NotA403() {
        signedInAs("owner");
        assertThatThrownBy(() -> guard.requireVisibleBoard(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
