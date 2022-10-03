package com.dongcheol.search.domain.place;

import com.dongcheol.search.global.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(indexes = @Index(name = "idx_query", columnList = "query"))
public class QueryLogCount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String query;

    @Column(nullable = false)
    private Long count;

    public QueryLogCount(String query, Long count) {
        this.query = query;
        this.count = count;
    }
}
