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
/**
 * AI resume analysis client.
 *
 * <p>Encapsulates calls to an external large-model service configured via
 * environment variables (TA_AI_*). It builds prompts, issues HTTP requests,
 * parses responses and provides a small fallback mechanism to try alternative
 * models when the preferred model is unavailable.</p>
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>TA_AI_BASE_URL - base URL of the model service</li>
 *   <li>TA_AI_API_KEY - API key or token</li>
 *   <li>TA_AI_MODEL - preferred model name</li>
 * </ul>
 * </p>
 */
public class AiResumeAnalysisClient {
    // 固定的环境变量 Key 名
    private static final String ENV_BASE_URL = "TA_AI_BASE_URL";
    private static final String ENV_API_KEY = "TA_AI_API_KEY";
    private static final String ENV_MODEL = "TA_AI_MODEL";
    private static final String ENV_TIMEOUT_MS = "TA_AI_TIMEOUT_MS";
    private static final String ENV_MAX_OUTPUT_TOKENS = "TA_AI_MAX_OUTPUT_TOKENS";
    private static final String ENV_AUTH_HEADER = "TA_AI_AUTH_HEADER";
    private static final String ENV_AUTH_SCHEME = "TA_AI_AUTH_SCHEME";

    public boolean isConfigured() {
        Config config = config();
        return !config.apiKey.isEmpty() && !config.baseUrl.isEmpty() && !config.model.isEmpty();
    }

    public AnalysisResult analyzeResume(String resumeJsonBody, String analysisDemand) {
        Config config = config();
        // 核心熔断机制：只要三大核心变量有一个没配，直接报错拒绝请求
        if (config.apiKey.isEmpty() || config.baseUrl.isEmpty() || config.model.isEmpty()) {
            return new AnalysisResult(false, -1, "", "未配置完整的大模型环境变量（请检查TA_AI_BASE_URL, TA_AI_API_KEY, TA_AI_MODEL）");
        }
        // 尝试调用配置的模型；若返回 model_not_found，则尝试备选模型列表以提高可用性。
        String[] fallbackModels = parseFallbackModels(firstNonEmpty(env("TA_AI_FALLBACK_MODELS"), "gpt-4o-mini,gpt-4,gpt-3.5-turbo"));
        String prompt = buildUserPrompt(resumeJsonBody, analysisDemand);

        // 首先尝试配置中的 model，然后按需尝试备选模型（去重）
        java.util.LinkedHashSet<String> tryModels = new java.util.LinkedHashSet<>();
        tryModels.add(config.model);
        for (String m : fallbackModels) tryModels.add(m);

        for (String modelTry : tryModels) {
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
                conn.setRequestProperty(config.authHeader, config.authScheme.isEmpty() ? config.apiKey : config.authScheme + " " + config.apiKey);

                JSONObject payload = new JSONObject()
                        .put("model", modelTry)
                        .put("temperature", 0.2)
                    .put("max_tokens", config.maxOutputTokens)
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
                    // 如果是 model_not_found 风格的返回，继续尝试下一个模型
                    if (body != null && body.toLowerCase().contains("model_not_found")) {
                        // 记录并继续
                        continue;
                    }
                    return new AnalysisResult(false, status, "", body.isEmpty() ? "http-error" : body);
                }

                String content = extractContent(body);
                if (content.isEmpty()) {
                    return new AnalysisResult(false, status, body, "empty-content");
                }

                String normalized = normalizeJsonText(content);
                Object parsedJson = null;
                try {
                    parsedJson = new org.json.JSONTokener(normalized).nextValue();
                } catch (Exception pe) {
                    // fall through, parsedJson stays null
                }

                if (parsedJson instanceof JSONObject) {
                    JSONObject analysis = (JSONObject) parsedJson;
                    return new AnalysisResult(true, status, analysis.toString(), "");
                } else if (parsedJson instanceof JSONArray) {
                    // 如果模型返回了 JSON 数组，则包装为一个 object，放入 items 字段
                    JSONObject wrapper = new JSONObject().put("items", (JSONArray) parsedJson);
                    return new AnalysisResult(true, status, wrapper.toString(), "");
                } else {
                    // 解析失败或返回非 JSON，返回原始内容以便上层记录/显示
                    return new AnalysisResult(true, status, normalized.isEmpty() ? content : normalized, "");
                }
            } catch (Exception e) {
                // 如果异常中提示模型不可用，则继续尝试下一模型
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (msg.contains("model_not_found") || msg.contains("model not found")) {
                    continue;
                }
                return new AnalysisResult(false, -1, "", e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        return new AnalysisResult(false, -1, "", "All candidate models failed or unavailable (model_not_found)");
    }

    private static String[] parseFallbackModels(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new String[0];
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }

    private static String buildSystemPrompt() {
        return "你是一位高校专业的助教招聘专家。请分析以下求职者的 PDF 简历内容，并严格按照 JSON 格式输出以下字段：\n"
                + "name (姓名)\n"
                + "education (最高学历及专业)\n"
                + "core_skills (核心技能数组)\n"
                + "ta_experience (是否有助教或教学相关经验，true/false)\n"
                + "matching_score (岗位匹配度打分，0-100)\n"
            + "evaluation (简短的优缺点评价，尽量控制在 80 字以内)\n\n"
            + "只返回 JSON，不要输出 Markdown、解释、重复内容或额外文本。";
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
        return root.optString("content", "");
    }

    private static String normalizeJsonText(String content) {
        String text = content == null ? "" : content.trim();
        int firstObj = text.indexOf('{');
        int firstArr = text.indexOf('[');
        int firstIndex = -1;
        char open = '{';
        char close = '}';
        if (firstObj >= 0 && firstArr >= 0) {
            if (firstObj < firstArr) {
                firstIndex = firstObj;
                open = '{'; close = '}';
            } else {
                firstIndex = firstArr;
                open = '['; close = ']';
            }
        } else if (firstObj >= 0) {
            firstIndex = firstObj;
            open = '{'; close = '}';
        } else if (firstArr >= 0) {
            firstIndex = firstArr;
            open = '['; close = ']';
        }

        if (firstIndex >= 0) {
            int lastIndex = text.lastIndexOf(close);
            if (lastIndex > firstIndex) {
                return text.substring(firstIndex, lastIndex + 1);
            }
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

    // 🌟 核心重构：彻底只从 System.getenv 纯净读取
    private static Config config() {
        String baseUrl = env(ENV_BASE_URL);
        String apiKey = env(ENV_API_KEY);
        String model = env(ENV_MODEL);
        
        // 非核心非敏感参数允许保留兜底逻辑
        String authHeader = firstNonEmpty(env(ENV_AUTH_HEADER), "Authorization");
        String authScheme = firstNonEmpty(env(ENV_AUTH_SCHEME), "Bearer");
        int timeoutMs = parseInt(firstNonEmpty(env(ENV_TIMEOUT_MS), "60000"), 60000);
        int maxOutputTokens = parseInt(firstNonEmpty(env(ENV_MAX_OUTPUT_TOKENS), "384"), 384);

        return new Config(normalizeBaseUrl(baseUrl), apiKey, model, timeoutMs, maxOutputTokens, authHeader, authScheme);
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
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
    }

    private static final class Config {
        private final String baseUrl;
        private final String apiKey;
        private final String model;
        private final int timeoutMs;
        private final int maxOutputTokens;
        private final String authHeader;
        private final String authScheme;

        private Config(String baseUrl, String apiKey, String model, int timeoutMs, int maxOutputTokens, String authHeader, String authScheme) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.timeoutMs = timeoutMs;
            this.maxOutputTokens = maxOutputTokens;
            this.authHeader = authHeader;
            this.authScheme = authScheme;
        }
    }
}
