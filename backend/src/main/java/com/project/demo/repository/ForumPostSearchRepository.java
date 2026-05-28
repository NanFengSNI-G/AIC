package com.project.demo.repository;

import com.project.demo.document.ForumPostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostSearchRepository
        extends ElasticsearchRepository<ForumPostDocument, Long> {
}
