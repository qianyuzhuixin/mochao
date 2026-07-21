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

    /** 摘要写作模式：摘要内容 */
    private String summaryContent;

    /** 摘要写作模式：自写内容 */
    private String selfWrittenContent;
}
