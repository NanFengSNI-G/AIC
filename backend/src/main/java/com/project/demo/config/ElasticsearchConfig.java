package com.project.demo.config;

import com.project.demo.document.ForumPostDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {

    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void createIndexIfNeeded() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ForumPostDocument.class);

        if (indexOps.exists()) {
            log.info("ES index 'forum_post' already exists, skipping creation");
            return;
        }

        Document settings = Document.parse("""
            {
              "index": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "analysis": {
                  "analyzer": {
                    "ik_smart_pinyin": {
                      "type": "custom",
                      "tokenizer": "ik_smart",
                      "filter": ["pinyin_filter", "lowercase"]
                    },
                    "ik_max_word_pinyin": {
                      "type": "custom",
                      "tokenizer": "ik_max_word",
                      "filter": ["pinyin_filter", "lowercase"]
                    }
                  },
                  "filter": {
                    "pinyin_filter": {
                      "type": "pinyin",
                      "keep_first_letter": true,
                      "keep_separate_first_letter": true,
                      "keep_full_pinyin": true,
                      "keep_original": true,
                      "limit_first_letter_length": 16,
                      "lowercase": true,
                      "remove_duplicated_term": true
                    }
                  }
                }
              }
            }
            """);

        indexOps.create(settings);
        indexOps.putMapping(indexOps.createMapping(ForumPostDocument.class));
        log.info("ES index 'forum_post' created with IK + Pinyin analyzers");
    }
}
