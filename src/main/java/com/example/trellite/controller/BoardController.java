package com.example.trellite.controller;

import com.example.trellite.dto.BoardCreateDTO;
import com.example.trellite.dto.BoardResponseDTO;
import com.example.trellite.dto.BoardUpdateDTO;
import com.example.trellite.dto.MemberUpdateDTO;
import com.example.trellite.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** Only the caller's own boards — owned or joined. */
    @GetMapping
    public ResponseEntity<List<BoardResponseDTO>> getAllBoards() {
        return new ResponseEntity<>(boardService.getAllBoards(), HttpStatus.OK);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<BoardResponseDTO>> getBoards(
            @RequestParam(required = false) String boardName,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) List<String> members,
            @RequestParam(required = false) LocalDateTime createdAt
    ) {
        List<BoardResponseDTO> boards = boardService.getBoards(boardName, owner, members, createdAt);
        if (boards.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(boards, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<BoardResponseDTO> createBoard(
            @Valid @RequestBody BoardCreateDTO boardCreateDTO
    ) {
        BoardResponseDTO createdBoard = boardService.createBoard(boardCreateDTO);
        return new ResponseEntity<>(createdBoard, HttpStatus.CREATED);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> getBoard(@PathVariable Integer boardId) {
        BoardResponseDTO board = boardService.getBoard(boardId);
        return new ResponseEntity<>(board, HttpStatus.OK);
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> updateBoard(
            @PathVariable Integer boardId,
            @Valid @RequestBody BoardUpdateDTO boardUpdateDTO
    ) {
       BoardResponseDTO updatedBoard = boardService.updateBoard(boardId, boardUpdateDTO);
       return new ResponseEntity<>(updatedBoard, HttpStatus.OK);
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Integer boardId) {
        boardService.deleteBoard(boardId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{boardId}/members/add")
    public ResponseEntity<BoardResponseDTO> addMember(
            @PathVariable Integer boardId,
            @Valid @RequestBody MemberUpdateDTO memberUpdateDTO
    ) {
        BoardResponseDTO boardResponseDTO = boardService.addMember(boardId, memberUpdateDTO);
        return new ResponseEntity<>(boardResponseDTO, HttpStatus.OK);
    }

    @PatchMapping("/{boardId}/members/remove")
    public ResponseEntity<BoardResponseDTO> removeMember(
            @PathVariable Integer boardId,
            @Valid @RequestBody MemberUpdateDTO memberUpdateDTO
    ) {
         BoardResponseDTO boardResponseDTO = boardService.removeMember(boardId, memberUpdateDTO);
         return new ResponseEntity<>(boardResponseDTO, HttpStatus.OK);
    }
}
