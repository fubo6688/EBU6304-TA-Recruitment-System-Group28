package com.ta.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("deprecation")
public class AiResumeAnalysisClient {
    private static final String ENV_CONFIG = "TA_AI_CONFIG";
    private static final String ENV_BASE_URL = "TA_AI_BASE_URL";
    private static final String ENV_API_KEY = "TA_AI_API_KEY";
    private static final String ENV_MODEL = "TA_AI_MODEL";
    private static final String ENV_TIMEOUT_MS = "TA_AI_TIMEOUT_MS";
    private static final String ENV_AUTH_HEADER = "TA_AI_AUTH_HEADER";
    private static final String ENV_AUTH_SCHEME = "TA_AI_AUTH_SCHEME";

    private static final String DEFAULT_BASE_URL = "https://kuaipao.pro";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    public boolean isConfigured() {
        return !config().apiKey.isEmpty();
    }

    public AnalysisResult analyzeResume(String resumeJsonBody, String analysisDemand) {
        Config config = config();
        if (config.apiKey.isEmpty()) {
            return AnalysisResult.notConfigured();
        }

        String prompt = buildUserPrompt(resumeJsonBody, analysisDemand);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(config.baseUrl + "/v1/chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(config.timeoutMs);
            conn.setReadTimeout(config.timeoutMs);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "TaSystem-ResumeAnalyzer/1.0");

            String authHeader = config.authHeader.isEmpty() ? "Authorization" : config.authHeader;
            String authScheme = config.authScheme.isEmpty() ? "Bearer" : config.authScheme;
            conn.setRequestProperty(authHeader, authScheme.isEmpty() ? config.apiKey : authScheme + " " + config.apiKey);

            JSONObject payload = new JSONObject()
                    .put("model", config.model)
                    .put("temperature", 0.2)
                    .put("response_format", new JSONObject().put("type", "json_object"))
                    .put("messages", new JSONArray()
                            .put(new JSONObject().put("role", "system").put("content", buildSystemPrompt()))
                            .put(new JSONObject().put("role", "user").put("content", prompt)));

            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String body = readResponseBody(conn, status);
            if (status < 200 || status >= 300) {
                return new AnalysisResult(false, status, "", body.isEmpty() ? "http-error" : body);
            }

            String content = extractContent(body);
            if (content.isEmpty()) {
                return new AnalysisResult(false, status, body, "empty-content");
            }

            String normalized = normalizeJsonText(content);
            JSONObject analysis = new JSONObject(normalized);
            return new AnalysisResult(true, status, analysis.toString(), "");
        } catch (Exception e) {
            return new AnalysisResult(false, -1, "", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String buildSystemPrompt() {
        return "你是一位高校专业的助教招聘专家。请分析以下求职者的 PDF 简历内容，并严格按照 JSON 格式输出以下字段：\n"
                + "name (姓名)\n"
                + "education (最高学历及专业)\n"
                + "core_skills (核心技能数组)\n"
                + "ta_experience (是否有助教或教学相关经验，true/false)\n"
                + "matching_score (岗位匹配度打分，0-100)\n"
                + "evaluation (简短的优缺点评价)\n\n"
                + "只返回 JSON，不要输出 Markdown、解释或额外文本。";
    }

    private static String buildUserPrompt(String resumeJsonBody, String analysisDemand) {
        String resumeText = buildResumeText(resumeJsonBody);
        String demand = analysisDemand == null || analysisDemand.trim().isEmpty()
                ? "请基于简历内容进行助教招聘分析，并输出结构化 JSON。"
                : analysisDemand.trim();
        return demand + "\n\n简历文本：\n" + resumeText;
    }

    private static String buildResumeText(String resumeJsonBody) {
        if (resumeJsonBody == null || resumeJsonBody.trim().isEmpty()) {
            return "未解析到简历文本。";
        }

        try {
            JSONObject root = new JSONObject(resumeJsonBody);
            Object payload = extractResumePayload(root);
            StringBuilder builder = new StringBuilder();
            appendText(builder, payload, 0);
            String text = builder.toString().trim();
            if (text.isEmpty()) {
                text = resumeJsonBody;
            }
            return truncate(text, 12000);
        } catch (Exception e) {
            return truncate(resumeJsonBody, 12000);
        }
    }

    private static Object extractResumePayload(JSONObject root) {
        JSONObject data = root.optJSONObject("data");
        if (data != null) {
            JSONObject attributes = data.optJSONObject("attributes");
            if (attributes != null) {
                JSONObject result = attributes.optJSONObject("result");
                if (result != null) {
                    return result;
                }
            }
            JSONObject result = data.optJSONObject("result");
            if (result != null) {
                return result;
            }
        }

        JSONObject result = root.optJSONObject("result");
        if (result != null) {
            return result;
        }
        return root;
    }

    private static void appendText(StringBuilder builder, Object value, int depth) {
        if (value == null || depth > 3) {
            return;
        }

        if (value instanceof JSONObject) {
            JSONObject json = (JSONObject) value;
            for (String key : json.keySet()) {
                Object child = json.opt(key);
                if (child == null || child == JSONObject.NULL) {
                    continue;
                }
                if (child instanceof JSONObject || child instanceof JSONArray) {
                    appendText(builder, child, depth + 1);
                    continue;
                }
                String text = String.valueOf(child).trim();
                if (!text.isEmpty()) {
                    builder.append(key).append(": ").append(text).append('\n');
                }
            }
            return;
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                appendText(builder, array.opt(i), depth + 1);
            }
            return;
        }

        String text = String.valueOf(value).trim();
        if (!text.isEmpty()) {
            builder.append(text).append('\n');
        }
    }

    private static String extractContent(String body) {
        JSONObject root = new JSONObject(body);
        JSONArray choices = root.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject first = choices.optJSONObject(0);
            if (first != null) {
                JSONObject message = first.optJSONObject("message");
                if (message != null) {
                    String content = message.optString("content", "");
                    if (!content.isEmpty()) {
                        return content;
                    }
                }
                String text = first.optString("text", "");
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        String outputText = root.optString("output_text", "");
        if (!outputText.isEmpty()) {
            return outputText;
        }

        return root.optString("content", "");
    }

    private static String normalizeJsonText(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int firstBrace = text.indexOf('{');
            int lastBrace = text.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                text = text.substring(firstBrace, lastBrace + 1);
            }
        }

        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }
        return text;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private static String readResponseBody(HttpURLConnection conn, int status) throws IOException {
        InputStream stream = status >= 200 && status < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int len;
            while ((len = in.read(data)) != -1) {
                buffer.write(data, 0, len);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static Config config() {
        JSONObject json = readConfigJson();
        String baseUrl = firstNonEmpty(value(json, "base_url"), value(json, "baseUrl"), env(ENV_BASE_URL), DEFAULT_BASE_URL);
        String apiKey = firstNonEmpty(value(json, "api_key"), value(json, "apiKey"), env(ENV_API_KEY));
        String model = firstNonEmpty(value(json, "model"), env(ENV_MODEL), DEFAULT_MODEL);
        String authHeader = firstNonEmpty(value(json, "auth_header"), value(json, "authHeader"), env(ENV_AUTH_HEADER), "Authorization");
        String authScheme = firstNonEmpty(value(json, "auth_scheme"), value(json, "authScheme"), env(ENV_AUTH_SCHEME), "Bearer");
        int timeoutMs = parseInt(firstNonEmpty(value(json, "timeout_ms"), value(json, "timeoutMs"), env(ENV_TIMEOUT_MS), "20000"), 20000);

        return new Config(normalizeBaseUrl(baseUrl), apiKey, model, timeoutMs, authHeader, authScheme);
    }

    private static JSONObject readConfigJson() {
        String raw = firstNonEmpty(System.getProperty("ta.ai.config"), env(ENV_CONFIG));
        if (raw.isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(raw);
        } catch (Exception ignore) {
            return new JSONObject();
        }
    }

    private static String value(JSONObject json, String key) {
        if (json == null || !json.has(key)) {
            return "";
        }
        return json.optString(key, "").trim();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String env(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    private static String normalizeBaseUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (Exception ignore) {
            return fallback;
        }
    }

    public static final class AnalysisResult {
        public final boolean ok;
        public final int statusCode;
        public final String body;
        public final String errorMessage;

        public AnalysisResult(boolean ok, int statusCode, String body, String errorMessage) {
            this.ok = ok;
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
        }

        public static AnalysisResult notConfigured() {
            return new AnalysisResult(false, -1, "", "not-configured");
        }
    }

    private static final class Config {
        private final String baseUrl;
        private final String apiKey;
        private final String model;
        private final int timeoutMs;
        private final String authHeader;
        private final String authScheme;

        private Config(String baseUrl, String apiKey, String model, int timeoutMs, String authHeader, String authScheme) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey == null ? "" : apiKey.trim();
            this.model = model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
            this.timeoutMs = timeoutMs;
            this.authHeader = authHeader == null ? "Authorization" : authHeader.trim();
            this.authScheme = authScheme == null ? "Bearer" : authScheme.trim();
        }
    }
}