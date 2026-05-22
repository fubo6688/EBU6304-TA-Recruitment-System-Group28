package com.ta.servlet;

import com.ta.util.DataManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deadline reminder scheduler listener.
 *
 * <p>Creates a background scheduled executor on application startup to
 * periodically run position deadline reminder scans. This decouples reminders
 * from front-end requests so notifications are sent even when no one visits
 * the positions pages.</p>
 */
public class DeadlineReminderSchedulerListener implements ServletContextListener {
    private static final long DEFAULT_INTERVAL_MINUTES = 60L;
    private static final long DEFAULT_INITIAL_DELAY_SECONDS = 10L;

    private ScheduledExecutorService scheduler;
    private final DataManager dataManager = new DataManager();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        long intervalMinutes = parsePositiveLong(
                System.getProperty("ta.deadline.reminder.interval.minutes"),
                DEFAULT_INTERVAL_MINUTES);
        long initialDelaySeconds = parsePositiveLong(
                System.getProperty("ta.deadline.reminder.initial.delay.seconds"),
                DEFAULT_INITIAL_DELAY_SECONDS);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ta-deadline-reminder-scheduler");
            t.setDaemon(true);
            return t;
        });

        Runnable job = () -> {
            try {
                dataManager.runDeadlineReminderSweep();
            } catch (Exception ex) {
                context.log("Deadline reminder scheduled task failed", ex);
            }
        };

        scheduler.scheduleAtFixedRate(
                job,
                initialDelaySeconds,
                TimeUnit.MINUTES.toSeconds(intervalMinutes),
                TimeUnit.SECONDS);

        context.log("Deadline reminder scheduler started (intervalMinutes=" + intervalMinutes + ")");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        sce.getServletContext().log("Deadline reminder scheduler stopped");
    }

    private long parsePositiveLong(String raw, long defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(raw.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
