package com.project.demo.controller;

import com.project.demo.dto.ApiResponse;
import com.project.demo.dto.UpdateRecordNameDTO;
import com.project.demo.entity.InterviewQA;
import com.project.demo.entity.InterviewRecord;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 面试控制器
 * 路径: /api/interview/**
 */
@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * POST /api/interview/upload
     * 上传简历和 JD
     */
    @PostMapping("/upload")
    public ApiResponse<Void> uploadResumeAndJd(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam("file") MultipartFile file,
            @RequestParam("jdText") String jdText) throws Exception {
            interviewService.uploadResumeAndJd(file, jdText, user.getUserId());
            return ApiResponse.<Void>success("简历上传成功，请点击开始面试", null);
    }

    /**
     * 更新面试知识库/api/interview/update
     */
    @PostMapping("/update")
    public ApiResponse<String> uploadResumeFile(){
        interviewService.updateKnowledge();
        return ApiResponse.success("面试知识库更新成功");
    }

    /**
     * 查询用户所有面试记录
     */
    @GetMapping("/records")
    public ApiResponse<List<InterviewRecord>> getInterviewRecords(@AuthenticationPrincipal
                                                                     UserDetailsImpl user){
        return ApiResponse.success(interviewService.getInterviewRecords(user.getUserId()));
    }

    /**
     * 查询某条记录对应的所有问答
     */
    @GetMapping("/records/{recordId}")
    public ApiResponse<List<InterviewQA>> getQARecords(@PathVariable Long recordId){
        return ApiResponse.success(interviewService.getQARecords(recordId));
    }

    /**
     *  修改记录名称
     */
    @PutMapping("/records/name")
    public ApiResponse<Void> updateRecordName(@RequestBody UpdateRecordNameDTO updateRecordNameDTO){
        interviewService.updateRecordNameById(updateRecordNameDTO.getRecordId(), updateRecordNameDTO.getName());
        return ApiResponse.success();
    }


    /**
     * 删除面试记录
     */
    @DeleteMapping("/records/{recordId}")
    public ApiResponse<Void> deleteInterviewRecord(@PathVariable Long recordId){
        interviewService.deleteInterviewRecord(recordId);
        return ApiResponse.success();
    }
}