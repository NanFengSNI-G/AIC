package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_qa")
public class InterviewQA {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private String question;

    private String answer;

    private String evaluation;

    private Integer status;
}
