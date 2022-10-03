package com.dongcheol.search.domain.place;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QueryLogCountRepository extends JpaRepository<QueryLogCount, Long> {

    Optional<QueryLogCount> findByQuery(String query);

    @Transactional
    @Modifying
    @Query("update QueryLogCount q set q.count = q.count + :count where q.id = :id")
    int increaseCount(@Param("id") Long id, @Param("count") Long count);
}
