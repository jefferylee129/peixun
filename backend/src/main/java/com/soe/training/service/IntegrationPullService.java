package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soe.training.common.BusinessException;
import com.soe.training.domain.CourseSource;
import com.soe.training.domain.ExternalCourse;
import com.soe.training.mapper.ExternalCourseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部课程拉取适配器
 * 行业模式: 按来源类型路由到适配器, 统一输出"标准课程条目", 幂等写入暂存表
 *  - yunxuetang: OAuth2 风格取令牌 + 分页拉取课程列表(端点/字段可配置, 具体以贵司租约的开放平台文档为准)
 *  - api: 通用 REST JSON 拉取(dataPath 定位列表 + fieldMapping 字段映射)
 *  - mock: 演示数据(离线演示/联调)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationPullService {

    private final ExternalCourseMapper externalCourseMapper;
    private final ObjectMapper objectMapper;

    public record PullOutcome(int total, int success, int fail, int skip, List<String> errors) {}

    /** 拉取并幂等写入暂存表 */
    public PullOutcome pull(CourseSource src) {
        List<JsonNode> items = switch (src.getSourceType()) {
            case "mock" -> mockItems();
            case "yunxuetang" -> fetchRemote(src, true);
            case "api" -> fetchRemote(src, false);
            default -> throw new BusinessException("不支持的数据源类型: " + src.getSourceType());
        };
        return upsertStaging(src, items);
    }

    /** 测试连接: 尝试真实拉取(最多1页), 成功返回条数 */
    public Map<String, Object> test(CourseSource src) {
        Map<String, Object> result = new HashMap<>();
        if ("mock".equals(src.getSourceType())) {
            result.put("ok", true);
            result.put("message", "演示数据源无需外部连接, 可直接同步");
            return result;
        }
        try {
            List<JsonNode> items = fetchRemote(src, true);
            result.put("ok", true);
            result.put("message", "连接成功, 首次拉取到 " + items.size() + " 门课程");
            result.put("sample", items.size());
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", "连接失败: " + rootMessage(e));
        }
        return result;
    }

    // ---------- 远端拉取 ----------

    private List<JsonNode> fetchRemote(CourseSource src, boolean firstPageOnly) {
        try {
            RestClient client = RestClient.create();
            String token = null;
            boolean needToken = "yunxuetang".equals(src.getSourceType())
                    || (src.getTokenPath() != null && !src.getTokenPath().isBlank());
            if (needToken) {
                if (isBlank(src.getBaseUrl()) || isBlank(src.getTokenPath())) {
                    throw new BusinessException("云学堂/带令牌数据源必须配置开放平台地址与令牌接口路径");
                }
                token = fetchToken(client, src);
            }
            if (isBlank(src.getListPath())) {
                throw new BusinessException("未配置课程列表接口路径");
            }
            List<JsonNode> items = new ArrayList<>();
            int maxPages = intCfg(src, "maxPages", firstPageOnly ? 1 : 10);
            int pageSize = intCfg(src, "pageSize", 50);
            String dataPath = isBlank(src.getDataPath()) ? "data.list" : src.getDataPath().trim();
            if ("api".equals(src.getSourceType()) && isBlank(src.getDataPath())) {
                dataPath = "data";
            }
            int page = 1;
            while (page <= maxPages) {
                String sep = src.getListPath().contains("?") ? "&" : "?";
                JsonNode resp = client.get()
                        .uri(src.getBaseUrl() + src.getListPath() + sep + "page=" + page + "&pageSize=" + pageSize)
                        .header("Authorization", token == null ? "" : "Bearer " + token)
                        .retrieve()
                        .body(JsonNode.class);
                JsonNode list = at(resp, dataPath);
                if (list == null || !list.isArray() || list.isEmpty()) {
                    break;
                }
                list.forEach(items::add);
                if (list.size() < pageSize) {
                    break;
                }
                page++;
            }
            return items;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException("连接外部系统失败: " + rootMessage(e));
        } catch (Exception e) {
            throw new BusinessException("拉取课程数据失败: " + rootMessage(e));
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchToken(RestClient client, CourseSource src) {
        String tokenField = cfg(src, "tokenField", "access_token");
        String url = src.getBaseUrl() + src.getTokenPath();
        String tokenBody = cfg(src, "tokenBody", "form");
        JsonNode resp;
        if ("json".equals(tokenBody)) {
            resp = client.post().uri(url)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of("app_key", nvl(src.getAppKey()), "app_secret", nvl(src.getAppSecret())))
                    .retrieve().body(JsonNode.class);
        } else {
            org.springframework.util.MultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
            form.add("app_key", nvl(src.getAppKey()));
            form.add("app_secret", nvl(src.getAppSecret()));
            resp = client.post().uri(url)
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve().body(JsonNode.class);
        }
        JsonNode tokenNode = at(resp, tokenField);
        if (tokenNode == null || tokenNode.asText().isBlank()) {
            throw new BusinessException("令牌接口未返回 " + tokenField + ", 请核对开放平台文档与字段配置");
        }
        return tokenNode.asText();
    }

    // ---------- 暂存写入(幂等) ----------

    private PullOutcome upsertStaging(CourseSource src, List<JsonNode> items) {
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();
        Map<String, String> fmap = fieldMap(src);
        Map<String, String> cmap = categoryMap(src);
        int no = 1;
        for (JsonNode item : items) {
            try {
                String extId = txt(item, fmap.get("externalId"));
                String name = txt(item, fmap.get("name"));
                if (isBlank(extId)) {
                    fail++;
                    errors.add("第" + no + "条: 缺少外部课程ID, 已跳过");
                    continue;
                }
                if (isBlank(name)) {
                    fail++;
                    errors.add("第" + no + "条: 缺少课程名称, 已跳过");
                    continue;
                }
                String rawCat = txt(item, fmap.get("category"));
                String mapped = rawCat == null ? null : cmap.get(rawCat.trim());
                ExternalCourse e = externalCourseMapper.selectBySourceAndExternalId(src.getId(), extId);
                boolean isNew = e == null;
                if (isNew) {
                    e = new ExternalCourse();
                    e.setSourceId(src.getId());
                    e.setExternalId(extId);
                    e.setStatus("0");
                }
                e.setCourseName(name);
                e.setCategoryRaw(rawCat);
                if (mapped != null) {
                    e.setCategory(mapped);
                }
                e.setCourseType("online");
                String hours = txt(item, fmap.get("hours"));
                if (!isBlank(hours)) {
                    try {
                        e.setHours(new BigDecimal(hours.trim()));
                    } catch (Exception ignored) {
                    }
                }
                e.setLevel(txt(item, fmap.get("level")));
                e.setTargetUsers(txt(item, fmap.get("targetUsers")));
                e.setDescription(txt(item, fmap.get("description")));
                e.setUrl(txt(item, fmap.get("url")));
                String raw = item.toString();
                e.setRawJson(raw.length() > 4000 ? raw.substring(0, 4000) : raw);
                e.setSyncTime(LocalDateTime.now());
                if (isNew) {
                    externalCourseMapper.insert(e);
                } else {
                    externalCourseMapper.updateById(e);
                }
                success++;
            } catch (Exception ex) {
                fail++;
                errors.add("第" + no + "条: " + rootMessage(ex));
            }
            no++;
        }
        return new PullOutcome(items.size(), success, fail, 0, errors);
    }

    // ---------- 演示数据 ----------

    private List<JsonNode> mockItems() {
        String[][] data = {
                {"EX-MOCK-001", "数字化转型:管理者的第一课", "通用课程", "2", "面向中基层管理者的数字化转型启蒙课"},
                {"EX-MOCK-002", "新员工入职通关指南", "新员工培训", "1.5", "企业文化/制度/安全一站式导入"},
                {"EX-MOCK-003", "安全生产责任制解读与落地", "安全课程", "3", "结合事故案例讲解全员安全责任制"},
                {"EX-MOCK-004", "高效沟通与跨部门协作", "管理课程", "2", "结构化表达与协同工作方法"},
                {"EX-MOCK-005", "财务报表十日通", "专业课程", "4", "非财务人员的报表分析实务"},
                {"EX-MOCK-006", "党性教育:精神谱系专题", "党建课程", "2.5", "以案例学习伟大建党精神"},
                {"EX-MOCK-007", "办公效率工具进阶", "通用课程", "1", "Excel/PPT 高阶实操"},
                {"EX-MOCK-008", "项目管理实战(PMP精要)", "管理课程", "3.5", "从立项到收尾的全流程演练"}
        };
        List<JsonNode> list = new ArrayList<>();
        for (String[] d : data) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d[0]);
            m.put("name", d[1]);
            m.put("categoryName", d[2]);
            m.put("credit", d[3]);
            m.put("description", d[4]);
            m.put("url", "https://www.yunxuetang.cn/course/" + d[0].replace("EX-MOCK-", "").toLowerCase());
            m.put("level", "全员");
            list.add(objectMapper.valueToTree(m));
        }
        return list;
    }

    // ---------- 配置辅助 ----------

    private Map<String, String> fieldMap(CourseSource src) {
        Map<String, String> defaults = new HashMap<>(Map.of(
                "externalId", "id", "name", "name", "category", "categoryName",
                "hours", "credit", "url", "url", "level", "level",
                "description", "description", "targetUsers", "targetUsers"));
        if (!isBlank(src.getFieldMapping())) {
            try {
                Map<String, Object> custom = objectMapper.readValue(src.getFieldMapping(), Map.class);
                custom.forEach((k, v) -> {
                    if (v != null && !v.toString().isBlank()) {
                        defaults.put(k, v.toString());
                    }
                });
            } catch (Exception e) {
                throw new BusinessException("字段映射配置不是合法 JSON");
            }
        }
        return defaults;
    }

    private Map<String, String> categoryMap(CourseSource src) {
        Map<String, String> map = new HashMap<>();
        if (!isBlank(src.getCategoryMapping())) {
            try {
                Map<String, Object> m = objectMapper.readValue(src.getCategoryMapping(), Map.class);
                m.forEach((k, v) -> map.put(k, v == null ? null : v.toString()));
            } catch (Exception e) {
                throw new BusinessException("分类映射配置不是合法 JSON");
            }
        }
        return map;
    }

    public String cfg(CourseSource src, String key, String def) {
        if (isBlank(src.getExtraConfig())) {
            return def;
        }
        try {
            JsonNode n = at(objectMapper.readTree(src.getExtraConfig()), key);
            return n == null || n.asText().isBlank() ? def : n.asText();
        } catch (Exception e) {
            return def;
        }
    }

    private int intCfg(CourseSource src, String key, int def) {
        try {
            return Integer.parseInt(cfg(src, key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    /** 按 a.b.c 点路径取 JSON 值 */
    private JsonNode at(JsonNode node, String path) {
        if (node == null || path == null || path.isBlank()) {
            return node;
        }
        JsonNode cur = node;
        for (String p : path.split("\\.")) {
            if (cur == null) {
                return null;
            }
            cur = cur.get(p.trim());
        }
        return cur;
    }

    private String txt(JsonNode node, String path) {
        JsonNode n = at(node, path);
        return n == null || n.isNull() ? null : n.asText().trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg;
    }
}
