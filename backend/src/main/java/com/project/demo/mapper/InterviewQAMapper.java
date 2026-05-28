package com.project.demo.mapper;

import com.project.demo.entity.InterviewQA;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InterviewQAMapper{

    void insertQA(InterviewQA qa);

    List<InterviewQA> selectByRecordId(@Param("recordId") Long recordId);

    void softDeleteByRecordId(@Param("recordId") Long recordId);
}