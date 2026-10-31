package com.soe.training.common;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入读取工具: 把首行为表头的工作表读成行数据(列下标 -> 单元格文本)
 */
public class ExcelImportUtil {

    public static List<Map<Integer, String>> read(MultipartFile file) throws IOException {
        List<Map<Integer, String>> rows = new ArrayList<>();
        EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                rows.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet(0).headRowNumber(1).doRead();
        return rows;
    }

    /** 取单元格文本(去首尾空白) */
    public static String str(Map<Integer, String> row, int col) {
        String v = row.get(col);
        return v == null ? null : v.trim();
    }

    /** 宽容解析日期: 支持 2026-03-05 / 2026/3/5 / 2026.3.5 */
    public static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim().replace('/', '-').replace('.', '-');
        String[] parts = v.split("-");
        if (parts.length == 3) {
            try {
                return LocalDate.of(Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
            } catch (Exception ignored) {
            }
        }
        try {
            return LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }
}
