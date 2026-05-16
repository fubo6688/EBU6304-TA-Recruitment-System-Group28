package com.ta.servlet;

import org.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.ta.util.DataManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class LoginServletProjectTest {

    public static void main(String[] args) throws Exception {
        Path tempDataDir = Files.createTempDirectory("ta-login-test-");
        String originalDataDirProperty = System.getProperty("ta.data.dir");

        try {
            System.setProperty("ta.data.dir", tempDataDir.toString());
            Files.writeString(tempDataDir.resolve("users.txt"), String.join(System.lineSeparator(),
                    "ta002|李四|lisi@example.com|Qmta2026A|TA|20210002|active",
                    "mo001|王老师|wangteacher@example.com|Qmta2026A|MO|M001|active",
                    "admin001|管理员|admin@example.com|Qmta2026A|Admin|ADM001|active",
                    "ta_bob|Bob Li|ta_bob@example.com|Qmta2026A|TA|ta_bob|active"
            ) + System.lineSeparator(), StandardCharsets.UTF_8);

            DataManager dataManager = new DataManager();
            assertTrue(dataManager.getUserById("ta002") != null, "precondition failed: ta002 should exist in the temp project data");

            LoginServlet servlet = new LoginServlet();

            JSONObject roleHint = invokeGet(servlet, request(Map.of("action", "role-hint", "userId", "ta002")));
            assertTrue(roleHint.getBoolean("success"), "role-hint should succeed for an existing account, response=" + roleHint);
            assertEquals("TA", roleHint.getString("role"), "role-hint should return the real role from project data");

            JSONObject loginSuccess = invokePost(servlet, request(Map.of(
                    "userId", "ta002",
                    "password", "Qmta2026A",
                    "role", "TA"
            )));
            assertTrue(loginSuccess.getBoolean("success"), "login should succeed with correct project credentials");
            assertEquals("ta002", loginSuccess.getJSONObject("user").getString("userId"), "login should return the matched user");
            assertEquals("TA", loginSuccess.getJSONObject("user").getString("role"), "login response should keep the role");

            JSONObject roleMismatch = invokePost(servlet, request(Map.of(
                    "userId", "ta002",
                    "password", "Qmta2026A",
                    "role", "MO"
            )));
            assertFalse(roleMismatch.getBoolean("success"), "login should reject role mismatch");
            assertEquals("Role mismatch", roleMismatch.getString("message"), "role mismatch message should match servlet behavior");

            JSONObject invalidAttempt1 = invokePost(servlet, request(Map.of(
                    "userId", "ta_bob",
                    "password", "wrong-1",
                    "role", "TA"
            )));
            assertFalse(invalidAttempt1.getBoolean("success"), "invalid password should fail");
            assertEquals(2, invalidAttempt1.getInt("attemptsRemaining"), "first failure should leave two attempts");

            invokePost(servlet, request(Map.of(
                    "userId", "ta_bob",
                    "password", "wrong-2",
                    "role", "TA"
            )));

            JSONObject locked = invokePost(servlet, request(Map.of(
                    "userId", "ta_bob",
                    "password", "wrong-3",
                    "role", "TA"
            )));
            assertFalse(locked.getBoolean("success"), "third wrong password should lock the account");
            assertTrue(locked.getBoolean("locked"), "response should mark the account as locked");

            System.out.println("LoginServletProjectTest passed.");
        } finally {
            restoreSystemProperty("ta.data.dir", originalDataDirProperty);
            deleteRecursively(tempDataDir);
        }
    }

    public static JSONObject invokeGet(LoginServlet servlet, HttpServletRequest request) throws Exception {
        TestResponse response = new TestResponse();
        servlet.doGet(request, response.proxy());
        return new JSONObject(response.body());
    }

    public static JSONObject invokePost(LoginServlet servlet, HttpServletRequest request) throws Exception {
        TestResponse response = new TestResponse();
        servlet.doPost(request, response.proxy());
        return new JSONObject(response.body());
    }

    public static HttpServletRequest request(Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                LoginServletProjectTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new RequestHandler(parameters)
        );
    }

    public static HttpServletRequest request(String pathInfo, String userId, Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                LoginServletProjectTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new RequestHandler(pathInfo, userId, parameters)
        );
    }

    public static TestResponse response() {
        return new TestResponse();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    public static void restoreSystemProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    public static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }

        try {
            Files.walk(root)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    public static final class TestResponse {
        private final StringWriter bodyWriter = new StringWriter();
        private final PrintWriter writer = new PrintWriter(bodyWriter, true);

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    LoginServletProjectTest.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("setContentType".equals(name)) {
                            return null;
                        }
                        if ("getWriter".equals(name)) {
                            return writer;
                        }
                        if ("sendError".equals(name)) {
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        String body() {
            writer.flush();
            return bodyWriter.toString();
        }
    }

    public static final class RequestHandler implements InvocationHandler {
        private final Map<String, String> parameters;
        private final String pathInfo;
        private TestSession session;

        RequestHandler(Map<String, String> parameters) {
            this(null, null, parameters);
        }

        RequestHandler(String pathInfo, String userId, Map<String, String> parameters) {
            this.pathInfo = pathInfo;
            this.parameters = new HashMap<>(parameters);
            if (userId != null) {
                this.session = new TestSession();
                this.session.attributes.put("userId", userId);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getParameter".equals(name)) {
                return parameters.get(args[0]);
            }
            if ("getPathInfo".equals(name)) {
                return pathInfo;
            }
            if ("getContextPath".equals(name)) {
                return "/ta-system";
            }
            if ("setCharacterEncoding".equals(name)) {
                return null;
            }
            if ("getSession".equals(name)) {
                boolean create = args == null || args.length == 0 || !(args[0] instanceof Boolean) || (Boolean) args[0];
                if (session == null && create) {
                    session = new TestSession();
                }
                return session == null ? null : session.proxy();
            }
            return defaultValue(method.getReturnType());
        }
    }

    public static final class TestSession implements InvocationHandler {
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean invalidated;

        HttpSession proxy() {
            return (HttpSession) Proxy.newProxyInstance(
                    LoginServletProjectTest.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("setAttribute".equals(name)) {
                attributes.put((String) args[0], args[1]);
                return null;
            }
            if ("getAttribute".equals(name)) {
                return attributes.get(args[0]);
            }
            if ("invalidate".equals(name)) {
                invalidated = true;
                attributes.clear();
                return null;
            }
            if ("setMaxInactiveInterval".equals(name)) {
                return null;
            }
            if ("isNew".equals(name)) {
                return !invalidated;
            }
            return defaultValue(method.getReturnType());
        }
    }

    public static Object defaultValue(Class<?> returnType) {
        if (returnType == null || returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE || returnType == Short.TYPE || returnType == Byte.TYPE || returnType == Long.TYPE) {
            return 0;
        }
        if (returnType == Float.TYPE || returnType == Double.TYPE) {
            return 0;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}