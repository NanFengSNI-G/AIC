package com.project.demo.agent.interview;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AgentStateFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InterviewState extends AgentState {

    // ── 基础信息 ──
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_RESUME = "resume";
    public static final String KEY_JD = "jd";

    // ── 路由栈（用 List 模拟，兼容 LangGraph4j 序列化） ──
    public static final String KEY_ROUTE_STACK = "routeStack";

    // ── 音频/文字数据 ──
    public static final String KEY_INPUT_AUDIO = "inputAudio";
    public static final String KEY_INPUT_TEXT = "inputText";
    public static final String KEY_OUTPUT_TEXT = "outputText";
    public static final String KEY_OUTPUT_AUDIO = "outputAudio";

    // ── 消息列表（决策Agent的LLM响应记录） ──
    public static final String KEY_MESSAGES = "messages";

    // ── 当前题目（用于持久化） ──
    public static final String KEY_CURRENT_QUESTION = "currentQuestion";

    public InterviewState(Map<String, Object> initData) {
        super(initData);
    }

    // ── 基础信息 getters ──

    public Long getUserId() {
        return this.<Long>value(KEY_USER_ID).orElse(null);
    }

    public String getResume() {
        return this.<String>value(KEY_RESUME).orElse("");
    }

    public String getJd() {
        return this.<String>value(KEY_JD).orElse("");
    }

    // ── 路由栈（List 模拟：末尾=栈顶） ──

    @SuppressWarnings("unchecked")
    public List<String> getRouteStack() {
        return this.<List<String>>value(KEY_ROUTE_STACK).orElseGet(ArrayList::new);
    }

    public void pushRoute(String nodeName) {
        getRouteStack().add(nodeName);
    }

    public String popRoute() {
        List<String> stack = getRouteStack();
        if (stack.isEmpty()) return null;
        return stack.remove(stack.size() - 1);
    }

    public String peekRoute() {
        List<String> stack = getRouteStack();
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    // ── 音频/文字 getters ──

    public byte[] getInputAudio() {
        return this.<byte[]>value(KEY_INPUT_AUDIO).orElse(null);
    }

    public String getInputText() {
        return this.<String>value(KEY_INPUT_TEXT).orElse("");
    }

    public String getOutputText() {
        return this.<String>value(KEY_OUTPUT_TEXT).orElse("");
    }

    public byte[] getOutputAudio() {
        return this.<byte[]>value(KEY_OUTPUT_AUDIO).orElse(null);
    }

    // ── 题目持久化 ──

    public String getCurrentQuestion() {
        return this.<String>value(KEY_CURRENT_QUESTION).orElse("");
    }

    // ── 消息列表 ──

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMessages() {
        return this.<List<Map<String, Object>>>value(KEY_MESSAGES).orElseGet(ArrayList::new);
    }

    // ── 工厂方法 ──

    public static AgentStateFactory<InterviewState> factory() {
        return InterviewState::new;
    }
}
