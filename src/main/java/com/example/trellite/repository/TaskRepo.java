package com.example.trellite.repository;

import com.example.trellite.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepo extends JpaRepository<Task, Integer>, JpaSpecificationExecutor<Task> {

    List<Task> findByTaskList_ListId(Integer listId);
}
