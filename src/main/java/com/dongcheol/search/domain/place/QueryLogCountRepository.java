package com.dongcheol.search.domain.place;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryLogCountRepository extends JpaRepository<QueryLogCount, Long> {

}
