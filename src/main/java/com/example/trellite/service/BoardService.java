package com.example.trellite.service;

import com.example.trellite.dto.BoardCreateDTO;
import com.example.trellite.dto.BoardResponseDTO;
import com.example.trellite.dto.BoardUpdateDTO;
import com.example.trellite.dto.MemberUpdateDTO;
import com.example.trellite.dto.UserSummaryDTO;
import com.example.trellite.exception.ConflictException;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.Board;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.BoardRepo;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final BoardRepo boardRepo;
    private final AuthRepo userRepo;
    private final BoardAccessGuard access;

    public BoardService(BoardRepo boardRepo, AuthRepo userRepo, BoardAccessGuard access) {
        this.boardRepo = boardRepo;
        this.userRepo = userRepo;
        this.access = access;
    }

    // Reusable private helper that maps a Board entity to a BoardResponseDTO. The viewer
    // is passed in so the response can carry what that particular caller is to the board.
    private BoardResponseDTO mapToBoardResponseDTO(Board board, User viewer) {
        UserSummaryDTO ownerDTO = new UserSummaryDTO(
                board.getOwner().getId(),
                board.getOwner().getUsername(),
                board.getOwner().getEmail()
        );

        Set<UserSummaryDTO> memberDTOs = board.getMembers().stream()
                .map(member -> new UserSummaryDTO(
                        member.getId(),
                        member.getUsername(),
                        member.getEmail()
                ))
                .collect(Collectors.toSet());

        return new BoardResponseDTO(
                board.getBoardId(),
                board.getBoardName(),
                board.getBoardDescription(),
                board.getCreatedAt(),
                ownerDTO,
                memberDTOs,
                access.roleOf(board, viewer)
        );
    }

    public List<BoardResponseDTO> getBoards(String boardName, String ownerUsername,
                                            List<String> memberUsernames, LocalDateTime createdAt) {
        User viewer = access.currentUser();

        Specification<Board> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Prevent duplicate results from joins
            query.distinct(true);

            // Never let a filter widen visibility: whatever else is asked for, the results
            // are restricted to boards the caller owns or belongs to. The membership half
            // is a subquery rather than another join so it cannot interact with the
            // members filter below.
            Subquery<Integer> memberOf = query.subquery(Integer.class);
            Root<Board> memberBoard = memberOf.from(Board.class);
            Join<Board, User> memberOfJoin = memberBoard.join("members", JoinType.INNER);
            memberOf.select(memberBoard.get("boardId"))
                    .where(cb.equal(memberOfJoin.get("id"), viewer.getId()));
            predicates.add(cb.or(
                    cb.equal(root.get("owner").get("id"), viewer.getId()),
                    root.get("boardId").in(memberOf)
            ));

            // Filter by board name
            if (boardName != null) {
                predicates.add(cb.equal(
                        cb.lower(root.get("boardName")),
                        boardName.toLowerCase()
                ));
            }

            // Filter by owner — needs a JOIN since owner is a @ManyToOne User
            if (ownerUsername != null) {
                Join<Board, User> ownerJoin = root.join("owner", JoinType.INNER);
                predicates.add(cb.equal(
                        cb.lower(ownerJoin.get("username")),
                        ownerUsername.toLowerCase()
                ));
            }

            // Filter by members — needs a JOIN since members is a @ManyToMany Set<User>
            if (memberUsernames != null && !memberUsernames.isEmpty()) {
                Join<Board, User> memberJoin = root.join("members", JoinType.INNER);
                predicates.add(
                        memberJoin.get("username").in(memberUsernames)
                );
            }

            // Filter by createdAt
            if (createdAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        createdAt
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return boardRepo.findAll(spec).stream()
                .map(board -> mapToBoardResponseDTO(board, viewer))
                .collect(Collectors.toList());
    }

    public BoardResponseDTO createBoard(BoardCreateDTO dto) {
        User owner = access.currentUser();

        Board board = Board.builder()
                .boardName(dto.boardName())
                .boardDescription(dto.boardDescription())
                .owner(owner)
                .build();
        boardRepo.save(board);
        return mapToBoardResponseDTO(board, owner);
    }

    /** Only the caller's own boards — owned or joined. */
    public List<BoardResponseDTO> getAllBoards() {
        User viewer = access.currentUser();
        return boardRepo.findAllVisibleTo(viewer.getId())
                .stream()
                .map(board -> mapToBoardResponseDTO(board, viewer))
                .collect(Collectors.toList());
    }

    public BoardResponseDTO getBoard(Integer boardId) {
        Board board = access.requireVisibleBoard(boardId);
        return mapToBoardResponseDTO(board, access.currentUser());
    }

    public BoardResponseDTO updateBoard(Integer boardId, BoardUpdateDTO boardUpdateDTO) {
        Board board = access.requireOwnedBoard(boardId);

        if (boardUpdateDTO.boardName() != null)
            board.setBoardName(boardUpdateDTO.boardName());
        if (boardUpdateDTO.boardDescription() != null)
            board.setBoardDescription(boardUpdateDTO.boardDescription());
        return mapToBoardResponseDTO(boardRepo.save(board), access.currentUser());
    }

    public void deleteBoard(Integer boardId) {
        Board board = access.requireOwnedBoard(boardId);
        boardRepo.delete(board);
    }

    /**
     * Resolves the person a membership change is aimed at. The client may send either a
     * numeric userId or a username — the frontend has no way to know an id before the
     * person is already on the board, so username is the usable half of this in practice.
     */
    private User resolveTarget(MemberUpdateDTO memberUpdateDTO) {
        if (memberUpdateDTO.userId() != null) {
            return userRepo.findById(memberUpdateDTO.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        return userRepo.findByUsername(memberUpdateDTO.username())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user called " + memberUpdateDTO.username()));
    }

    public BoardResponseDTO addMember(Integer boardId, MemberUpdateDTO memberUpdateDTO) {
        Board board = access.requireOwnedBoard(boardId);
        User user = resolveTarget(memberUpdateDTO);

        if (access.isOwner(board, user)) {
            throw new ConflictException("The board owner is already on this board");
        }
        if (access.isMember(board, user.getId())) {
            throw new ConflictException(user.getUsername() + " is already a member of this board");
        }
        board.getMembers().add(user);
        return mapToBoardResponseDTO(boardRepo.save(board), access.currentUser());
    }

    public BoardResponseDTO removeMember(Integer boardId, MemberUpdateDTO memberUpdateDTO) {
        Board board = access.requireOwnedBoard(boardId);
        User user = resolveTarget(memberUpdateDTO);

        if (!access.isMember(board, user.getId())) {
            throw new ConflictException(user.getUsername() + " is not a member of this board");
        }
        board.getMembers().remove(user);
        return mapToBoardResponseDTO(boardRepo.save(board), access.currentUser());
    }
}
