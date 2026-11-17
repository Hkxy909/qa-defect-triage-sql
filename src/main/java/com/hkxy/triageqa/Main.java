package com.hkxy.triageqa;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        try (TriageRepository repo = new TriageRepository("jdbc:sqlite::memory:")) {
            repo.addTestCase("AuthModule", "testOAuthTokenRefresh", true);
            int syncTestId = repo.addTestCase("SyncEngine", "testOfflineQueueRetry", false);
            int mediaTestId = repo.addTestCase("MediaUploader", "testLargeVideoChunking", false);

            repo.logDefect(syncTestId, "HIGH", 1, "SocketTimeoutException during background sync retry");
            repo.logDefect(mediaTestId, "MEDIUM", 2, "OutOfMemoryError when buffering 4K video frames");

            System.out.printf("Overall Pass Rate: %.1f%%%n", repo.getPassRate());

            System.out.println("\nFailures by Module:");
            for (Map.Entry<String, Integer> entry : repo.getFailureCountsByModule().entrySet()) {
                System.out.printf(" - %s: %d failure(s)%n", entry.getKey(), entry.getValue());
            }

            System.out.println("\nHigh-Severity Defects:");
            List<DefectTicket> highDefects = repo.getDefectsBySeverity("HIGH");
            for (DefectTicket ticket : highDefects) {
                System.out.printf(" [#%d | P%d] %s (%s): %s%n",
                    ticket.getId(),
                    ticket.getPriority(),
                    ticket.getModuleName(),
                    ticket.getTestName(),
                    ticket.getSummary()
                );
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }
}
