package com.mochao.module.ranking.dto;

import lombok.Data;

/**
 * 榜单查询参数
 */
@Data
public class RankingQueryDTO {

    /** 平台 */
    private String platform;

    /** 榜单类型 */
    private String rankType;

    /** 快照日期（默认今天） */
    private String snapDate;

    /** 分页 */
    private Integer page = 1;
    private Integer size = 50;
}
