package com.mochao.module.practice.dto;

import lombok.Data;

@Data
public class PracticeCompleteDTO {

    private String typedContent;
    private Integer typedChars;
    private Integer errorCount;
    private Integer duration;
    private Double accuracy;
    private Double speed;
}
