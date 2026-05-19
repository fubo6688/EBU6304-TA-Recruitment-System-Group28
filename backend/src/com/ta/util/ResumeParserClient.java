package com.ta.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

@SuppressWarnings("deprecation")
public class ResumeParserClient {
    private static final String ENV_URL = "RESUME_PARSER_URL";
    private static final String ENV_TOKEN = "RESUME_PARSER_TOKEN";
    private static final String ENV_AUTH_HEADER = "RESUME_PARSER_AUTH_HEADER";
    private static final String ENV_AUTH_SCHEME = "RESUME_PARSER_AUTH_SCHEME";
    private static final String ENV_FILE_FIELD = "RESUME_PARSER_FILE_FIELD";
    private static final String ENV_TIMEOUT_MS = "RESUME_PARSER_TIMEOUT_MS";
    private static final String ENV_FORM_FIELDS = "RESUME_PARSER_FORM_FIELDS";
    private static final String ENV_LANGUAGE = "RESUME_PARSER_LANGUAGE";

    public boolean isConfigured() {
        return !getEnv(ENV_URL).isEmpty();
    }

    public ParseResult parse(Path file, String originalName, String contentType) {
        String url = getEnv(ENV_URL);
        if (url.isEmpty()) {
            return ParseResult.notConfigured();
        }

        String token = getEnv(ENV_TOKEN);
        String authHeader = getEnvOrDefault(ENV_AUTH_HEADER, "apy-token");
        String authScheme = getEnvOrDefault(ENV_AUTH_SCHEME, "");
        String fileField = getEnvOrDefault(ENV_FILE_FIELD, "file");
        int timeoutMs = parseInt(getEnv(ENV_TIMEOUT_MS), 20000);
        String language = getEnvOrDefault(ENV_LANGUAGE, "English");

        String boundary = "----ResumeParserBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            if (!token.isEmpty()) {
                String headerValue = authScheme.isEmpty() ? token : authScheme + " " + token;
                conn.setRequestProperty(authHeader, headerValue);
            }

            appendDebug("PARSE REQUEST: url=" + url + " file=" + file.toString() + " originalName=" + originalName + " contentType=" + contentType + "\n");

            try (OutputStream out = conn.getOutputStream()) {
                Map<String, String> formFields = parseFormFields(getEnv(ENV_FORM_FIELDS));
                if (!language.isEmpty() && !formFields.containsKey("language")) {
                    formFields.put("language", language);
                }
                for (Map.Entry<String, String> entry : formFields.entrySet()) {
                    writeTextPart(out, boundary, entry.getKey(), entry.getValue());
                }

                String safeName = (originalName == null || originalName.trim().isEmpty())
                        ? file.getFileName().toString()
                        : originalName;
                String safeType = (contentType == null || contentType.trim().isEmpty())
                        ? "application/octet-stream"
                        : contentType;

                writeFilePart(out, boundary, fileField, safeName, safeType, file);
                writeBoundaryEnd(out, boundary);
            }

            int status = conn.getResponseCode();
            String body = readResponseBody(conn, status);
            appendDebug("PARSE RESPONSE: status=" + status + " body=" + body + "\n");
            boolean ok = status >= 200 && status < 300;

            String statusUrl = "";
            String jobId = "";
            if (!body.isEmpty()) {
                try {
                    JSONObject json = new JSONObject(body);
                    statusUrl = json.optString("status_url", "");
                    jobId = json.optString("job_id", "");
                    appendDebug("PARSE JSON EXTRACT: statusUrl=" + statusUrl + " jobId=" + jobId + "\n");
                    appendDebug("PARSE JSON ALL KEYS: " + String.join(",", json.keySet()) + "\n");
                } catch (Exception e) {
                    appendDebug("PARSE JSON ERROR: " + e.getMessage() + "\n");
                }
            }

            if (!statusUrl.isEmpty()) {
                StatusResult statusResult = fetchStatus(statusUrl, authHeader, authScheme, token, timeoutMs);
                if (statusResult.ok) {
                    return new ParseResult(true, statusResult.statusCode, statusResult.body, "", statusUrl, jobId, false);
                }
                return new ParseResult(false, statusResult.statusCode, "", statusResult.errorMessage, statusUrl, jobId, true);
            }

            return new ParseResult(ok, status, body, ok ? "" : "HTTP " + status, "", "", false);
        } catch (IOException e) {
            return new ParseResult(false, -1, "", e.getMessage(), "", "", false);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public StatusResult fetchStatus(String statusUrl) {
        if (statusUrl == null || statusUrl.trim().isEmpty()) {
            return new StatusResult(false, -1, "", "missing-status-url");
        }

        String token = getEnv(ENV_TOKEN);
        String authHeader = getEnvOrDefault(ENV_AUTH_HEADER, "apy-token");
        String authScheme = getEnvOrDefault(ENV_AUTH_SCHEME, "");
        int timeoutMs = parseInt(getEnv(ENV_TIMEOUT_MS), 20000);
        return fetchStatus(statusUrl.trim(), authHeader, authScheme, token, timeoutMs);
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    private static String getEnvOrDefault(String key, String fallback) {
        String value = getEnv(key);
        return value.isEmpty() ? fallback : value;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignore) {
            return fallback;
        }
    }

    private static Map<String, String> parseFormFields(String raw) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            return fields;
        }
        String[] pairs = raw.split(";");
        for (String pair : pairs) {
            if (pair == null || pair.trim().isEmpty()) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            String key = kv[0].trim();
            String value = kv.length > 1 ? kv[1].trim() : "";
            if (!key.isEmpty()) {
                fields.put(key, value);
            }
        }
        return fields;
    }

    private static void writeTextPart(OutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFilePart(OutputStream out,
                                      String boundary,
                                      String fieldName,
                                      String fileName,
                                      String contentType,
                                      Path file) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));

        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBoundaryEnd(OutputStream out, String boundary) throws IOException {
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
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

    private static StatusResult fetchStatus(String statusUrl,
                                            String authHeader,
                                            String authScheme,
                                            String token,
                                            int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(statusUrl).openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "TaSystem-ResumeParser/1.0");
            if (!token.isEmpty()) {
                String headerValue = authScheme.isEmpty() ? token : authScheme + " " + token;
                conn.setRequestProperty(authHeader, headerValue);
            }

            appendDebug("FETCH STATUS: url=" + statusUrl + " headers=" + authHeader + "\n");
            int status = conn.getResponseCode();
            String body = readResponseBody(conn, status);
            appendDebug("FETCH RESPONSE: status=" + status + " body=" + body + "\n");

            // If the response looks like an HTML 404 page from the ApyHub site,
            // try a best-effort retry against the api subdomain which some providers use.
            if ((status >= 400 || body == null || body.trim().isEmpty() || body.trim().toLowerCase().startsWith("<!doctype") || body.contains("<html"))
                    && statusUrl != null && statusUrl.toLowerCase().contains("apyhub.com")
                    && !statusUrl.toLowerCase().contains("api.apyhub.com")) {
                try {
                    String alt = statusUrl.replace("apyhub.com", "api.apyhub.com");
                    appendDebug("FETCH RETRY ALT: url=" + alt + "\n");
                    if (conn != null) {
                        conn.disconnect();
                    }
                    conn = (HttpURLConnection) new URL(alt).openConnection();
                    conn.setConnectTimeout(timeoutMs);
                    conn.setReadTimeout(timeoutMs);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("User-Agent", "TaSystem-ResumeParser/1.0");
                    if (!token.isEmpty()) {
                        String headerValue = authScheme.isEmpty() ? token : authScheme + " " + token;
                        conn.setRequestProperty(authHeader, headerValue);
                    }
                    int s2 = conn.getResponseCode();
                    String b2 = readResponseBody(conn, s2);
                    appendDebug("FETCH RESPONSE ALT: status=" + s2 + " body=" + b2 + "\n");
                    boolean ok2 = s2 >= 200 && s2 < 300 && b2 != null && !b2.trim().isEmpty();

                    // If we successfully obtained parsed JSON from the API subdomain,
                    // attempt to persist it into the existing resume_parsed storage so
                    // the rest of the system (UserServlet) can pick it up immediately.
                    if (ok2) {
                        try {
                            // try to extract jobId from the status URL (last path segment)
                            String candidateJobId = null;
                            try {
                                String[] parts = alt.split("/");
                                if (parts.length > 0) {
                                    candidateJobId = parts[parts.length - 1];
                                }
                            } catch (Exception ignore) {
                            }

                            // fallback: try x-apy-job-id response header
                            if ((candidateJobId == null || candidateJobId.isEmpty()) && conn.getHeaderField("x-apy-job-id") != null) {
                                candidateJobId = conn.getHeaderField("x-apy-job-id");
                            }

                            if (candidateJobId != null && !candidateJobId.trim().isEmpty()) {
                                // locate the job file under data/resume_parsed_jobs that matches this jobId
                                DataManager dm = new DataManager();
                                Path jobsDir = dm.getDataDirPath().resolve("resume_parsed_jobs");
                                if (Files.exists(jobsDir) && Files.isDirectory(jobsDir)) {
                                    try (DirectoryStream<Path> ds = Files.newDirectoryStream(jobsDir, "*.json")) {
                                        for (Path f : ds) {
                                            try {
                                                String content = Files.readString(f, StandardCharsets.UTF_8);
                                                JSONObject jo = new JSONObject(content);
                                                String jid = jo.optString("jobId", "");
                                                if (candidateJobId.equals(jid)) {
                                                    // userId is the filename without extension
                                                    String fileName = f.getFileName().toString();
                                                    String userId = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
                                                    Path parsedDir = dm.getDataDirPath().resolve("resume_parsed");
                                                    Files.createDirectories(parsedDir);
                                                    Path parsedFile = parsedDir.resolve(userId + ".json");
                                                    Files.write(parsedFile, b2.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                                                    appendDebug("AUTO-SAVED PARSED RESULT: user=" + userId + " jobId=" + candidateJobId + " file=" + parsedFile.toString() + "\n");
                                                    // clear the job file so servlet won't re-poll
                                                    try {
                                                        Files.deleteIfExists(f);
                                                    } catch (Exception ignore) {
                                                    }
                                                    break;
                                                }
                                            } catch (Exception ignore) {
                                            }
                                        }
                                    } catch (Exception ignore) {
                                    }
                                }
                            }
                        } catch (Exception ignore) {
                            appendDebug("AUTO-SAVE ERROR: " + ignore.getMessage() + "\n");
                        }
                    }

                    return new StatusResult(ok2, s2, b2, ok2 ? "" : "pending");
                } catch (IOException ignore) {
                    appendDebug("FETCH RETRY ERROR: " + ignore.getMessage() + "\n");
                }
            }

            boolean ok = status >= 200 && status < 300 && body != null && !body.trim().isEmpty();
            return new StatusResult(ok, status, body, ok ? "" : "pending");
        } catch (IOException e) {
            appendDebug("FETCH ERROR: " + e.getMessage() + "\n");
            return new StatusResult(false, -1, "", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void appendDebug(String text) {
        try {
            Path p = Path.of("data", "resume_parser_debug.log");
            String entry = java.time.Instant.now().toString() + " " + text;
            Files.createDirectories(p.getParent());
            Files.write(p, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignore) {
        }
    }

    public static class ParseResult {
        public final boolean ok;
        public final int statusCode;
        public final String body;
        public final String errorMessage;
        public final String statusUrl;
        public final String jobId;
        public final boolean pending;

        public ParseResult(boolean ok,
                           int statusCode,
                           String body,
                           String errorMessage,
                           String statusUrl,
                           String jobId,
                           boolean pending) {
            this.ok = ok;
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
            this.statusUrl = statusUrl == null ? "" : statusUrl;
            this.jobId = jobId == null ? "" : jobId;
            this.pending = pending;
        }

        public static ParseResult notConfigured() {
            return new ParseResult(false, -1, "", "not-configured", "", "", false);
        }
    }

    public static class StatusResult {
        public final boolean ok;
        public final int statusCode;
        public final String body;
        public final String errorMessage;

        public StatusResult(boolean ok, int statusCode, String body, String errorMessage) {
            this.ok = ok;
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
        }
    }
}
