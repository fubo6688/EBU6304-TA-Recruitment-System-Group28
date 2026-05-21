package com.ta.servlet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class CORSFilterProjectTest {

    public static void main(String[] args) throws Exception {
        CORSFilter filter = new CORSFilter();
        filter.init(null);

        TestResponse optionsResponse = new TestResponse();
        TestChain optionsChain = new TestChain();
        filter.doFilter(request("OPTIONS", "https://frontend.example", Map.of()), optionsResponse.proxy(), optionsChain.proxy());
        assertEquals("https://frontend.example", optionsResponse.headers.get("Access-Control-Allow-Origin"), "origin should be echoed back");
        assertEquals("Origin", optionsResponse.headers.get("Vary"), "vary header should be set when origin exists");
        assertEquals("true", optionsResponse.headers.get("Access-Control-Allow-Credentials"), "credentials should be enabled");
        assertEquals(Integer.valueOf(HttpServletResponse.SC_OK), optionsResponse.status, "options should short-circuit with 200");
        assertEquals(0, optionsChain.invocations, "preflight should not reach the chain");

        TestResponse getResponse = new TestResponse();
        TestChain getChain = new TestChain();
        filter.doFilter(request("GET", null, Map.of()), getResponse.proxy(), getChain.proxy());
        assertEquals("*", getResponse.headers.get("Access-Control-Allow-Origin"), "missing origin should allow any origin");
        assertEquals(1, getChain.invocations, "non-options requests should continue down the chain");

        TestResponse blankOriginResponse = new TestResponse();
        TestChain blankOriginChain = new TestChain();
        filter.doFilter(request("GET", "   ", Map.of()), blankOriginResponse.proxy(), blankOriginChain.proxy());
        assertEquals("*", blankOriginResponse.headers.get("Access-Control-Allow-Origin"), "blank origin should fall back to wildcard");
        assertEquals(1, blankOriginChain.invocations, "blank origin request should continue down the chain");

        System.out.println("CORSFilterProjectTest passed.");
    }

    private static HttpServletRequest request(String method, String origin, Map<String, String> headers) {
        Map<String, String> values = new HashMap<>(headers);
        if (origin != null) {
            values.put("Origin", origin);
        }
        return (HttpServletRequest) Proxy.newProxyInstance(
                CORSFilterProjectTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new RequestHandler(method, values)
        );
    }

    private static final class RequestHandler implements InvocationHandler {
        private final String method;
        private final Map<String, String> headers;

        RequestHandler(String method, Map<String, String> headers) {
            this.method = method;
            this.headers = headers;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getMethod".equals(name)) {
                return this.method;
            }
            if ("getHeader".equals(name)) {
                return headers.get(String.valueOf(args[0]));
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class TestResponse implements InvocationHandler {
        private final Map<String, String> headers = new HashMap<>();
        private Integer status;

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    CORSFilterProjectTest.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("setHeader".equals(name)) {
                headers.put(String.valueOf(args[0]), String.valueOf(args[1]));
                return null;
            }
            if ("setStatus".equals(name)) {
                status = (Integer) args[0];
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class TestChain implements InvocationHandler {
        private int invocations;

        FilterChain proxy() {
            return (FilterChain) Proxy.newProxyInstance(
                    CORSFilterProjectTest.class.getClassLoader(),
                    new Class<?>[]{FilterChain.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("doFilter".equals(method.getName())) {
                invocations++;
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static Object defaultValue(Class<?> returnType) {
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

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }
}