package com.soe.training.common;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Excel 导出工具(动态表头, 支持多 Sheet)
 */
public class ExcelExportUtil {

    @Data
    @AllArgsConstructor
    public static class SheetSpec {
        private String name;
        private List<String> headers;
        private List<List<Object>> rows;
    }

    private static void prepare(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    private static List<List<String>> toHead(List<String> headers) {
        return headers.stream().map(Collections::singletonList).toList();
    }

    public static void write(HttpServletResponse response, String filename, String sheetName,
                             List<String> headers, List<List<Object>> rows) throws IOException {
        prepare(response, filename);
        EasyExcel.write(response.getOutputStream())
                .head(toHead(headers))
                .sheet(sheetName)
                .doWrite(rows == null ? Collections.emptyList() : rows);
    }

    public static void writeMulti(HttpServletResponse response, String filename,
                                  List<SheetSpec> sheets) throws IOException {
        prepare(response, filename);
        ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build();
        try {
            int i = 0;
            for (SheetSpec spec : sheets) {
                WriteSheet ws = EasyExcel.writerSheet(i, spec.getName())
                        .head(toHead(spec.getHeaders()))
                        .build();
                writer.write(spec.getRows() == null ? Collections.emptyList() : spec.getRows(), ws);
                i++;
            }
        } finally {
            writer.finish();
        }
    }
}
