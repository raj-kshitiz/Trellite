package com.example.trellite.service;

import com.example.trellite.dto.BoardCreateDTO;
import com.example.trellite.dto.BoardResponseDTO;
import com.example.trellite.dto.BoardUpdateDTO;
import com.example.trellite.dto.UserSummaryDTO;
import com.example.trellite.model.Board;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.BoardRepo;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardService {

    public final BoardRepo boardRepo;
    public final AuthRepo userRepo;

    public BoardService(BoardRepo boardRepo, AuthRepo userRepo) {
        this.boardRepo = boardRepo;
        this.userRepo = userRepo;
    }

    // Reusable private helper method in BoardService that is used to map a Board entity to a BoardResponseDTO
    private BoardResponseDTO mapToBoardResponseDTO(Board board) {
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
                memberDTOs
        );
    }

    public List<BoardResponseDTO> getBoards(String boardName, String ownerUsername,
                                 List<String> memberUsernames, LocalDateTime createdAt) {
        Specification<Board> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Prevent duplicate results from joins
            query.distinct(true);

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
                .map(this::mapToBoardResponseDTO)
                .collect(Collectors.toList());
    }

    public BoardResponseDTO createBoard(BoardCreateDTO dto) {

        // load the user and set as owner
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User owner = userRepo.findByUsername(username);

        Board board = Board.builder()
                .boardName(dto.boardName())
                .boardDescription(dto.boardDescription())
                .owner(owner)
                .build();

        boardRepo.save(board);
        return mapToBoardResponseDTO(board);
    }

    public List<BoardResponseDTO> getAllBoards() {
        return boardRepo.findAll()
                .stream()
                .map(this::mapToBoardResponseDTO)
                .collect(Collectors.toList());
    }

    public BoardResponseDTO getBoard(Integer boardId) {
        Board board = boardRepo.findById(boardId).orElseThrow(NullPointerException::new);
        return mapToBoardResponseDTO(board);
    }

    public BoardResponseDTO updateBoard(Integer boardId, BoardUpdateDTO boardUpdateDTO) {
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        if (boardUpdateDTO.boardName() != null)
            board.setBoardName(boardUpdateDTO.boardName());
        if (boardUpdateDTO.boardDescription() != null)
            board.setBoardDescription(boardUpdateDTO.boardDescription());
        return mapToBoardResponseDTO(boardRepo.save(board));
    }

    public void deleteBoard(Integer boardId) {
        boardRepo.deleteById(boardId);
    }
}

