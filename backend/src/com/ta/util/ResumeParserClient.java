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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@SuppressWarnings("deprecation")
public class ResumeParserClient {
    /**
     * Local resume parsing client.
     *
     * <p>Uses Apache PDFBox to extract text from PDF files and builds a
     * structured JSON representation. The `parse(Path, String, String)` API
     * returns a {@code ParseResult} used by servlets to obtain parsed text and
     * page information for downstream AI analysis or display.</p>
     *
     * <p>Parsing is performed locally (no external service). Debug logs are
     * appended to `data/resume_parser_debug.log` on error to aid troubleshooting.</p>
     */
    public boolean isConfigured() {
        return true;
    }

    public ParseResult parse(Path file, String originalName, String contentType) {
        try {
            if (file == null || !Files.exists(file)) {
                return new ParseResult(false, 404, "", "resume-file-not-found", "", "", false);
            }

            JSONObject parsed = extractPdfAsJson(file, originalName, contentType);
            appendDebug("LOCAL PARSE OK: file=" + file + " originalName=" + originalName + " pages=" + parsed.optInt("pageCount", 0) + "\n");
            return new ParseResult(true, 200, parsed.toString(), "", "", "", false);
        } catch (Throwable e) {
            appendDebug("LOCAL PARSE ERROR: " + e.getMessage() + "\n");
            return new ParseResult(false, -1, "", e.getMessage(), "", "", false);
        }
    }

    private static void appendDebug(String text) {
        try {
            Path p = Path.of("data", "resume_parser_debug.log");
            String entry = Instant.now().toString() + " " + text;
            Files.createDirectories(p.getParent());
            Files.write(p, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignore) {
        }
    }

    private static JSONObject extractPdfAsJson(Path file, String originalName, String contentType) throws IOException {
        JSONObject result = new JSONObject();
        String safeOriginal = originalName == null ? "" : originalName.trim();
        String safeContentType = contentType == null ? "" : contentType.trim();

        result.put("sourceFileName", safeOriginal.isEmpty() ? file.getFileName().toString() : safeOriginal);
        result.put("sourceContentType", safeContentType.isEmpty() ? "application/pdf" : safeContentType);
        result.put("extractedAt", Instant.now().toString());

        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int pageCount = document.getNumberOfPages();
            result.put("pageCount", pageCount);

            JSONArray pages = new JSONArray();
            List<String> fullTextParts = new ArrayList<>();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = normalizeText(stripper.getText(document));
                JSONObject pageJson = new JSONObject()
                        .put("pageNumber", page)
                        .put("text", pageText);
                pages.put(pageJson);
                if (!pageText.isEmpty()) {
                    fullTextParts.add(pageText);
                }
            }

            String fullText = normalizeText(String.join("\n\n", fullTextParts));
            result.put("text", fullText);
            result.put("pages", pages);
            result.put("charCount", fullText.length());
            result.put("wordCount", countWords(fullText));
        }

        return result;
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        return normalized.replaceAll("[ \t]+", " ");
    }

    private static int countWords(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        return normalized.split("\\s+").length;
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
