package com.example.trellite.repository;

import com.example.trellite.model.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface ListRepo extends JpaRepository<TaskList, Integer> {
    List<TaskList> findByBoard_BoardId(Integer boardId);
}
