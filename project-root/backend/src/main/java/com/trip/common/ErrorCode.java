package com.trip.common;

/**
 * 基础错误码定义。
 */
public enum ErrorCode {

    SUCCESS("SUCCESS", "success"),
    COMMON_001("COMMON_001", "请求参数缺失或格式错误"),
    COMMON_002("COMMON_002", "请求参数不合法"),
    COMMON_003("COMMON_003", "请求资源不存在"),
    COMMON_008("COMMON_008", "排序字段不合法"),
    COMMON_006("COMMON_006", "系统内部错误，请稍后重试"),
    ROUTE_001("ROUTE_001", "起点节点不存在"),
    ROUTE_002("ROUTE_002", "终点节点不存在"),
    ROUTE_003("ROUTE_003", "当前起点与目标点不可达"),
    ROUTE_009("ROUTE_009", "起点和终点不属于同一目的地"),
    ROUTE_010("ROUTE_010", "路径规划服务异常"),
    AUTH_001("AUTH_001", "用户名已存在"),
    AUTH_002("AUTH_002", "用户名或密码错误"),
    AUTH_003("AUTH_003", "未登录或登录状态已失效"),
    AUTH_004("AUTH_004", "登录状态无效，请重新登录"),
    AUTH_005("AUTH_005", "权限不足，无法访问当前资源"),
    AUTH_006("AUTH_006", "当前账号已被禁用"),
    AUTH_007("AUTH_007", "用户名不能为空"),
    AUTH_008("AUTH_008", "密码不能为空"),
    AUTH_009("AUTH_009", "当前用户不存在"),
    AUTH_010("AUTH_010", "账号信息校验失败"),
    FILE_001("FILE_001", "未检测到上传文件"),
    FILE_002("FILE_002", "文件类型不支持"),
    FILE_003("FILE_003", "文件大小超出限制"),
    FILE_005("FILE_005", "文件保存失败"),
    IMPORT_001("IMPORT_001", "导入文件为空"),
    IMPORT_002("IMPORT_002", "导入文件格式不正确"),
    IMPORT_003("IMPORT_003", "导入字段缺失或字段名不匹配"),
    IMPORT_004("IMPORT_004", "数据校验失败，无法导入"),
    IMPORT_008("IMPORT_008", "导入服务异常"),
    COMMENT_001("COMMENT_001", "评论目标不存在或当前不可评论"),
    COMMENT_002("COMMENT_002", "评论不存在"),
    COMMENT_003("COMMENT_003", "评论内容不能为空且不能超过 500 个字符"),
    COMMENT_004("COMMENT_004", "无权删除该评论"),
    COMMENT_005("COMMENT_005", "评论保存或状态更新失败"),
    COMMENT_006("COMMENT_006", "评论类型不合法"),
    DIARY_003("DIARY_003", "日记不存在"),
    DIARY_006("DIARY_006", "日记评分失败"),
    DIARY_008("DIARY_008", "评分值不合法"),
    DIARY_011("DIARY_011", "当前日记不可访问");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
