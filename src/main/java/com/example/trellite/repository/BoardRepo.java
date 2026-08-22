package com.example.trellite.repository;

import com.example.trellite.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepo extends JpaRepository<Board, Integer>, JpaSpecificationExecutor<Board> {

    /**
     * Boards the given user owns or is a member of. GET /boards used to call findAll(),
     * which handed every board in the system to any authenticated caller.
     */
    @Query("select distinct b from Board b left join b.members m "
            + "where b.owner.id = :userId or m.id = :userId")
    List<Board> findAllVisibleTo(@Param("userId") Long userId);
}
