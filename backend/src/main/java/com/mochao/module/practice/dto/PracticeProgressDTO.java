package com.mochao.module.practice.dto;

import lombok.Data;

@Data
public class PracticeProgressDTO {

    private String typedContent;
    private Integer currentPosition;
    private Integer typedChars;
    private Integer errorCount;
}
