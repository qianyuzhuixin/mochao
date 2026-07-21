package com.mochao.module.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_practice_session")
public class PracticeSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    /** 章节索引（null=整本练习） */
    private Integer chapterIndex;

    /** 章节标题 */
    private String chapterTitle;

    /** 练习模式: copy-1:1抄写 summary-摘要写作 */
    private String mode;

    private String status;

    private String typedContent;

    private Integer currentPosition;

    private Integer totalChars;

    private Integer typedChars;

    private Integer errorCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private Double accuracy;

    private Double speed;

    private Double score;

    /** 摘要内容（摘要写作模式） */
    private String summaryContent;

    /** 自写内容（摘要写作模式） */
    private String selfWrittenContent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
