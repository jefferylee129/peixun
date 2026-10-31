package com.soe.training.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果
 */
@Data
public class ImportResult {

    private int total;
    private int success;
    private int fail;
    private int skip;
    private List<String> errors = new ArrayList<>();

    public void error(int rowNo, String msg) {
        fail++;
        errors.add("第" + rowNo + "行: " + msg);
    }

    public void skip(int rowNo, String msg) {
        skip++;
        errors.add("第" + rowNo + "行(跳过): " + msg);
    }
}
