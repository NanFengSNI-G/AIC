package com.project.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.demo.dto.InterviewQAMessage;
import com.project.demo.dto.InterviewRecordSummary;
import com.project.demo.entity.InterviewQA;
import com.project.demo.entity.InterviewRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 面试主流程服务
 */
public interface InterviewService {

    /** 上传简历和 JD */
    void uploadResumeAndJd(MultipartFile file, String jdText, Long userId) throws Exception;

    /** 处理开始面试 */
    void startInterview(Long userId) throws Exception;

    /** 处理用户文字回复，返回本轮生成的面试题文本 */
    String processUserText(Long userId, String userMessage, String question) throws Exception;

    /** 处理用户语音回复（Opus），返回本轮生成的面试题文本 */
    String processUserAudio(Long userId, byte[] opusAudio) throws Exception;

    /** 清空用户的历史会话记忆 */
    void clearInterviewMemory(Long userId);

    /** 更新知识库 */
    void updateKnowledge();

    /** 创建面试记录，返回记录id */
    Long createInterviewRecord(Long userId) throws Exception;

    /** 根据记录id更新记录名 */
    void updateRecordNameById(Long recordId, String recordName);

    /** 插入问答记录 */
    void insertQARecord(InterviewQAMessage interviewQAMessage);

    /** 查询用户的所有面试记录 */
    List<InterviewRecord> getInterviewRecords(Long userId);

    /** 查询某条记录对应的所有问答 */
    List<InterviewQA> getQARecords(Long recordId);

    /** 删除面试记录及对应的问答记录 */
    void deleteInterviewRecord(Long recordId);

    /** 生成并保存总评 */
    void generateFinalEvaluation(Long recordId);

    /** 查询用户所有面试记录的总评和记录时间（含已删除） */
    List<InterviewRecordSummary> getEvaluationHistory(Long userId);

}
