package com.mycompany.javaphone_nir2.logging;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 *
 *
 */
public class SessionLogger {

    private static SessionLogger instance;
    private final PrintWriter writer;
    private final long sessionStartTime;
    private final Path logDir;

    private static final DateTimeFormatter FILE_TS
            = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter LOG_TS
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private SessionLogger() {
        this.sessionStartTime = System.currentTimeMillis();
        this.logDir = Paths.get(System.getProperty("user.home"), ".javaphone", "logs");

        try {
            Files.createDirectories(logDir);
            String sessionName = "session_" + FILE_TS.format(Instant.now()) + ".log";
            Path logFile = logDir.resolve(sessionName);

            // true = append mode, true = auto-flush (garanted write also after crash)
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile.toFile(), true)), true);
            log("SESSION STARTED | JVM: " + System.getProperty("java.version") + " | OS: " + System.getProperty("os.name"));
        } catch (IOException e) {
            System.err.println("Failed to initialize SessionLogger: " + e.getMessage());
            throw new UncheckedIOException(e);
        }
    }

    public static synchronized SessionLogger getInstance() {
        if (instance == null) {
            instance = new SessionLogger();
        }
        return instance;
    }

    public synchronized void log(String message) {
        String absTime = LOG_TS.format(Instant.now());
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        String relTime = String.format("[+%02d:%02d:%02d.%03d]",
                elapsed / 3600000, (elapsed % 3600000) / 60000, (elapsed % 60000) / 1000, elapsed % 1000);

        String threadName = Thread.currentThread().getName();
        writer.printf("[%s] %s [%s] %s%n", absTime, relTime, threadName, message);
    }

    public synchronized void close() {
        if (writer != null && !writer.checkError()) {
            log("SESSION ENDED | Uptime: " + formatUptime());
            writer.flush();
            writer.close();
        }
    }

    private String formatUptime() {
        Duration d = Duration.ofMillis(System.currentTimeMillis() - sessionStartTime);
        return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }
}
