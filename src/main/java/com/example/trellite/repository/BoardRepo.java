package com.example.trellite.repository;

import com.example.trellite.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepo extends JpaRepository<Board, Integer>, JpaSpecificationExecutor<Board> {

}
