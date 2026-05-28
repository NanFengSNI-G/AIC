package com.project.demo.agent.interview.agent;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("decisionAgentTool")
public class DecisionAgentTool {

    @Tool("进入开场阶段，让候选人做自我介绍")
    public String toOpening() {
        DecisionContext.setLastRoute("opening");
        return "SUCCESS";
    }

    @Tool("进入技术面试阶段，考察技术栈基础")
    public String toTech() {
        DecisionContext.setLastRoute("tech");
        return "SUCCESS";
    }

    @Tool("进入项目经验阶段，深挖项目经历")
    public String toProject() {
        DecisionContext.setLastRoute("project");
        return "SUCCESS";
    }

    @Tool("进入追问阶段，对上一轮回答深入追问")
    public String toFollowup() {
        DecisionContext.setLastRoute("followup");
        return "SUCCESS";
    }

    @Tool("进入算法题阶段，考察算法和设计能力")
    public String toAlgorithm() {
        DecisionContext.setLastRoute("algorithm");
        return "SUCCESS";
    }

    @Tool("结束面试，生成结束语")
    public String toEnding() {
        DecisionContext.setLastRoute("ending");
        return "SUCCESS";
    }
}
