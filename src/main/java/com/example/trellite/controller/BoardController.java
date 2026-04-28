package com.example.trellite.controller;

import com.example.trellite.model.Board;
import com.example.trellite.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<List<Board>> getAllBoards() {
        return new ResponseEntity<>(boardService.getAllBoards(), HttpStatus.OK);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Board>> getBoards(
            @RequestParam(required = false) String boardName,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) List<String> members,
            @RequestParam(required = false) LocalDateTime createdAt
    ) {
        List<Board> boards = boardService.getBoards(boardName, owner, members, createdAt);
        if(boards.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(boards, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Board> createBoard(@RequestBody Board board) {
        boardService.createBoard(board);
        return new ResponseEntity<>(board, HttpStatus.CREATED);
    }



}
