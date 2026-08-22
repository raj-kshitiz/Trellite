package com.example.trellite.service;

import com.example.trellite.enums.BoardRole;
import com.example.trellite.exception.ForbiddenException;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.Board;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.BoardRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * The single place that answers "may this caller touch this board?".
 *
 * <p>Access has two levels, and every board-scoped operation in the app goes through
 * one of them:
 * <ul>
 *   <li>{@link #requireVisibleBoard} — owner OR member. Reading a board and full CRUD
 *       on its lists and tasks.</li>
 *   <li>{@link #requireOwnedBoard} — owner only. Renaming or deleting the board itself,
 *       and changing who its members are.</li>
 * </ul>
 *
 * <p>Centralised on purpose: the checks used to be ad-hoc string comparisons repeated
 * inside a handful of service methods, and every method that forgot one was an open door.
 * Identity is compared by id, never by username string or entity equality.
 */
@Service
public class BoardAccessGuard {

    private final BoardRepo boardRepo;
    private final AuthRepo userRepo;

    public BoardAccessGuard(BoardRepo boardRepo, AuthRepo userRepo) {
        this.boardRepo = boardRepo;
        this.userRepo = userRepo;
    }

    /** The authenticated caller, as a persistent entity. */
    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("You must be signed in to do that");
        }
        return userRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("Your account no longer exists"));
    }

    /** Loads a board without any access check. Only for callers that check separately. */
    public Board findBoard(Integer boardId) {
        return boardRepo.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
    }

    /** Owner or member — the level required to read a board and to work on its cards. */
    public Board requireVisibleBoard(Integer boardId) {
        Board board = findBoard(boardId);
        if (!canAccess(board, currentUser())) {
            throw new ForbiddenException("You do not have access to this board");
        }
        return board;
    }

    /** Owner only — renaming or deleting a board, and managing its members. */
    public Board requireOwnedBoard(Integer boardId) {
        Board board = findBoard(boardId);
        requireOwner(board);
        return board;
    }

    public void requireOwner(Board board) {
        if (!isOwner(board, currentUser())) {
            throw new ForbiddenException("Only the board owner can do that");
        }
    }

    public boolean canAccess(Board board, User user) {
        return isOwner(board, user) || isMember(board, user.getId());
    }

    public boolean isOwner(Board board, User user) {
        return board.getOwner().getId().equals(user.getId());
    }

    public boolean isMember(Board board, Long userId) {
        return board.getMembers().stream()
                .anyMatch(member -> member.getId().equals(userId));
    }

    /** What the given user is to this board, for the {@code role} field on board responses. */
    public BoardRole roleOf(Board board, User user) {
        return isOwner(board, user) ? BoardRole.OWNER : BoardRole.MEMBER;
    }
}
