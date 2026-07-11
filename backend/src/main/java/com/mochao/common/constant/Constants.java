package com.mochao.common.constant;

public class Constants {

    private Constants() {
    }

    /** Token前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Token Header名称 */
    public static final String TOKEN_HEADER = "Authorization";

    /** Redis key前缀 */
    public static final String REDIS_TOKEN_PREFIX = "mochao:token:";
    public static final String REDIS_USER_PREFIX = "mochao:user:";
    public static final String REDIS_CHECKIN_PREFIX = "mochao:checkin:";

    /** 用户角色 */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 用户状态 */
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISABLED = 0;

    /** 素材来源类型 */
    public static final int SOURCE_TYPE_BUILTIN = 0;
    public static final int SOURCE_TYPE_CUSTOM = 1;

    /** 练习状态 */
    public static final String PRACTICE_STATUS_ACTIVE = "active";
    public static final String PRACTICE_STATUS_PAUSED = "paused";
    public static final String PRACTICE_STATUS_COMPLETED = "completed";
    public static final String PRACTICE_STATUS_ABANDONED = "abandoned";

    /** 小说状态 */
    public static final String NOVEL_STATUS_DRAFT = "draft";
    public static final String NOVEL_STATUS_ONGOING = "ongoing";
    public static final String NOVEL_STATUS_COMPLETED = "completed";
    public static final String NOVEL_STATUS_ARCHIVED = "archived";

    /** 章节状态 */
    public static final String CHAPTER_STATUS_DRAFT = "draft";
    public static final String CHAPTER_STATUS_PUBLISHED = "published";
}
