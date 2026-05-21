package com.ta.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class DeadlineReminderSchedulerListenerProjectTest {

    public static void main(String[] args) throws Exception {
        String originalInterval = System.getProperty("ta.deadline.reminder.interval.minutes");
        String originalInitialDelay = System.getProperty("ta.deadline.reminder.initial.delay.seconds");
        System.setProperty("ta.deadline.reminder.interval.minutes", "0");
        System.setProperty("ta.deadline.reminder.initial.delay.seconds", "abc");

        try {
            DeadlineReminderSchedulerListener listener = new DeadlineReminderSchedulerListener();
            TestServletContext context = new TestServletContext();
            ServletContextEvent event = new ServletContextEvent(context.proxy());

            listener.contextInitialized(event);
            Object scheduler = readField(listener, "scheduler");
            assertTrue(scheduler != null, "scheduler should be created during initialization");

            listener.contextDestroyed(event);
            assertTrue(readField(listener, "scheduler") == null, "scheduler should be cleared on destroy");
            assertTrue(context.logs.stream().anyMatch(msg -> msg.contains("started")), "context should log startup");
            assertTrue(context.logs.stream().anyMatch(msg -> msg.contains("stopped")), "context should log shutdown");

            DeadlineReminderSchedulerListener untouchedListener = new DeadlineReminderSchedulerListener();
            TestServletContext untouchedContext = new TestServletContext();
            untouchedListener.contextDestroyed(new ServletContextEvent(untouchedContext.proxy()));
            assertTrue(untouchedContext.logs.stream().anyMatch(msg -> msg.contains("stopped")), "destroy without init should still log stop");

            System.out.println("DeadlineReminderSchedulerListenerProjectTest passed.");
        } finally {
            restoreSystemProperty("ta.deadline.reminder.interval.minutes", originalInterval);
            restoreSystemProperty("ta.deadline.reminder.initial.delay.seconds", originalInitialDelay);
        }
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void restoreSystemProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TestServletContext implements InvocationHandler {
        private final List<String> logs = new ArrayList<>();

        ServletContext proxy() {
            return (ServletContext) Proxy.newProxyInstance(
                    DeadlineReminderSchedulerListenerProjectTest.class.getClassLoader(),
                    new Class<?>[]{ServletContext.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("log".equals(method.getName())) {
                if (args != null && args.length > 0) {
                    logs.add(String.valueOf(args[0]));
                }
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
}