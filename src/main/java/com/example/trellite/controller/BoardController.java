package com.example.trellite.controller;

import com.example.trellite.dto.BoardCreateDTO;
import com.example.trellite.dto.BoardResponseDTO;
import com.example.trellite.dto.BoardUpdateDTO;
import com.example.trellite.service.BoardService;
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
    public ResponseEntity<BoardResponseDTO> createBoard(@RequestBody BoardCreateDTO boardCreateDTO) {
        BoardResponseDTO createdBoard = boardService.createBoard(boardCreateDTO);
        return new ResponseEntity<>(createdBoard, HttpStatus.CREATED);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> getBoard(@PathVariable Integer boardId) {
        BoardResponseDTO board = boardService.getBoard(boardId);
        return new ResponseEntity<>(board, HttpStatus.OK);
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> updateBoard(@PathVariable Integer boardId, @RequestBody BoardUpdateDTO boardUpdateDTO) {

        // not the correct way to handle this, but it works for now -> correct way is to handle exceptions globally using @ControllerAdvice
        try {
            BoardResponseDTO updatedBoard = boardService.updateBoard(boardId, boardUpdateDTO);
            return new ResponseEntity<>(updatedBoard, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Integer boardId) {
        try {
            boardService.deleteBoard(boardId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
