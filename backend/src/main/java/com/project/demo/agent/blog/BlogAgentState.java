package com.project.demo.agent.blog;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AgentStateFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlogAgentState extends AgentState {

    // ── 基本信息 ──
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_SESSION_ID = "sessionId";

    // ── 意图与参数 ──
    public static final String KEY_USER_MESSAGE = "userMessage";
    public static final String KEY_INTENT = "intent";           // write_blog / search_forum / chat
    public static final String KEY_TOPIC = "topic";
    public static final String KEY_KEYWORDS = "keywords";

    // ── 博客生成 ──
    public static final String KEY_OUTLINE = "outline";
    public static final String KEY_CONTENT_SECTIONS = "contentSections";
    public static final String KEY_CODE_BLOCKS = "codeBlocks";
    public static final String KEY_IMAGES = "images";
    public static final String KEY_FULL_BLOG = "fullBlog";
    public static final String KEY_BLOG_TITLE = "blogTitle";

    // ── 中断决策 ──
    public static final String KEY_USER_DECISION = "userDecision";  // PUBLISH / DRAFT
    public static final String KEY_SECTION_ID = "sectionId";

    // ── 用户附加要求 ──
    public static final String KEY_REQUIREMENTS = "requirements";  // "不要代码, 500字" 等

    // ── 搜索与聊天 ──
    public static final String KEY_SEARCH_RESULTS = "searchResults";
    public static final String KEY_CHAT_RESPONSE = "chatResponse";

    public BlogAgentState(Map<String, Object> initData) {
        super(initData);
    }

    // ── getters / setters ──

    public Long getUserId() {
        return this.<Number>value(KEY_USER_ID).map(Number::longValue).orElse(null);
    }

    public String getSessionId() {
        return this.<String>value(KEY_SESSION_ID).orElse("");
    }

    public String getUserMessage() {
        return this.<String>value(KEY_USER_MESSAGE).orElse("");
    }

    public String getIntent() {
        return this.<String>value(KEY_INTENT).orElse("");
    }

    public String getTopic() {
        return this.<String>value(KEY_TOPIC).orElse("");
    }

    public String getKeywords() {
        return this.<String>value(KEY_KEYWORDS).orElse("");
    }

    public String getRequirements() {
        return this.<String>value(KEY_REQUIREMENTS).orElse("");
    }

    public String getOutline() {
        return this.<String>value(KEY_OUTLINE).orElse("");
    }

    public String getContentSections() {
        return this.<String>value(KEY_CONTENT_SECTIONS).orElse("");
    }

    public String getCodeBlocks() {
        return this.<String>value(KEY_CODE_BLOCKS).orElse("");
    }

    public String getImages() {
        return this.<String>value(KEY_IMAGES).orElse("");
    }

    public String getFullBlog() {
        return this.<String>value(KEY_FULL_BLOG).orElse("");
    }

    public String getBlogTitle() {
        return this.<String>value(KEY_BLOG_TITLE).orElse("");
    }

    public String getUserDecision() {
        return this.<String>value(KEY_USER_DECISION).orElse(null);
    }

    public Long getSectionId() {
        return this.<Number>value(KEY_SECTION_ID).map(Number::longValue).orElse(null);
    }

    public String getSearchResults() {
        return this.<String>value(KEY_SEARCH_RESULTS).orElse("");
    }

    public String getChatResponse() {
        return this.<String>value(KEY_CHAT_RESPONSE).orElse("");
    }

    public static AgentStateFactory<BlogAgentState> factory() {
        return BlogAgentState::new;
    }
}
