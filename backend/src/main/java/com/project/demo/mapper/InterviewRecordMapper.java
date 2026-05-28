package com.project.demo.mapper;

import com.project.demo.dto.InterviewRecordSummary;
import com.project.demo.entity.InterviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InterviewRecordMapper{
    void insertRecord(InterviewRecord record);

    void updateRecordNameById(@Param("id") Long id, @Param("recordName") String recordName);

    List<InterviewRecord> selectByUserId(@Param("userId") Long userId);

    void softDeleteById(@Param("id") Long id);

    void updateEvaluation(@Param("id") Long id, @Param("evaluation") String evaluation);

    List<InterviewRecordSummary> selectEvaluationHistoryByUserId(@Param("userId") Long userId);

}
