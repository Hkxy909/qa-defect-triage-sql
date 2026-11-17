package com.hkxy.triageqa;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TriageRepositoryTest {

    private TriageRepository repo;

    @BeforeEach
    void setUp() throws SQLException {
        repo = new TriageRepository("jdbc:sqlite::memory:");
    }

    @AfterEach
    void tearDown() throws SQLException {
        repo.close();
    }

    @Test
    void testInvalidPriorityBoundaryThrowsException() throws SQLException {
        int testId = repo.addTestCase("Auth", "testLogin", false);
        assertThrows(IllegalArgumentException.class, () -> repo.logDefect(testId, "HIGH", 0, "Invalid low priority"));
        assertThrows(IllegalArgumentException.class, () -> repo.logDefect(testId, "HIGH", 6, "Invalid high priority"));
    }

    @Test
    void testBlankDefectSummaryThrowsException() throws SQLException {
        int testId = repo.addTestCase("Sync", "testSync", false);
        assertThrows(IllegalArgumentException.class, () -> repo.logDefect(testId, "MEDIUM", 2, "   "));
    }

    @Test
    void testPassRateCalculationAndEmptyDbBoundary() throws SQLException {
        assertEquals(0.0, repo.getPassRate(), 0.001);
        repo.addTestCase("Auth", "testLogin", true);
        repo.addTestCase("Auth", "testLogout", true);
        repo.addTestCase("Sync", "testPush", false);
        assertEquals(66.666, repo.getPassRate(), 0.01);
    }

    @Test
    void testGroupByIdentifiesHighFailureModules() throws SQLException {
        repo.addTestCase("SyncEngine", "test1", false);
        repo.addTestCase("SyncEngine", "test2", false);
        repo.addTestCase("AuthModule", "test3", false);
        repo.addTestCase("AuthModule", "test4", true);

        Map<String, Integer> failures = repo.getFailureCountsByModule();
        assertEquals(2, failures.get("SyncEngine"));
        assertEquals(1, failures.get("AuthModule"));
    }

    @Test
    void testInnerJoinFetchesDefectsBySeverityInPriorityOrder() throws SQLException {
        int id1 = repo.addTestCase("SyncEngine", "testRetry", false);
        int id2 = repo.addTestCase("AuthModule", "testToken", false);
        repo.logDefect(id1, "HIGH", 3, "Minor retry delay");
        repo.logDefect(id2, "HIGH", 1, "Auth token crash");

        List<DefectTicket> highDefects = repo.getDefectsBySeverity("HIGH");
        assertEquals(2, highDefects.size());
        assertEquals("AuthModule", highDefects.get(0).getModuleName());
        assertEquals(1, highDefects.get(0).getPriority());
    }
}
