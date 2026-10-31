package com.soe.training.common;

import lombok.Data;

/**
 * 统一返回结构
 */
@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok() {
        return build(200, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return build(200, "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return build(200, msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return build(500, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    public static <T> Result<T> build(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        return r;
    }
}
