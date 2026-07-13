package com.mochao.module.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 网文榜单快照
 */
@Data
@TableName("t_ranking_snapshot")
public class RankingSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台: qidian / fanqie / jinjiang / zhihu */
    private String platform;

    /** 榜单类型: month_ticket / recommend / collect / hot_search / new_book */
    private String rankType;

    /** 排名 */
    private Integer rankNo;

    /** 书名 */
    private String bookName;

    /** 作者 */
    private String author;

    /** 分类/标签 */
    private String category;

    /** 字数 */
    private Long wordCount;

    /** 热度值（月票/推荐票/收藏数 等） */
    private Long hotValue;

    /** 简介摘要 */
    private String intro;

    /** 封面图 */
    private String coverUrl;

    /** 原书链接 */
    private String bookUrl;

    /** 快照日期 */
    private LocalDate snapDate;

    private LocalDateTime createdAt;
}
