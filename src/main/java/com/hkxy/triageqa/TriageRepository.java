package com.hkxy.triageqa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TriageRepository implements AutoCloseable {

    private final Connection conn;

    public TriageRepository(String jdbcUrl) throws SQLException {
        this.conn = DriverManager.getConnection(jdbcUrl);
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS test_cases (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  module_name TEXT NOT NULL," +
                "  test_name TEXT NOT NULL," +
                "  passed INTEGER NOT NULL CHECK (passed IN (0, 1))" +
                ");"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS defects (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  test_case_id INTEGER NOT NULL," +
                "  severity TEXT NOT NULL," +
                "  priority INTEGER NOT NULL CHECK (priority BETWEEN 1 AND 5)," +
                "  summary TEXT NOT NULL," +
                "  FOREIGN KEY (test_case_id) REFERENCES test_cases(id)" +
                ");"
            );
        }
    }

    public int addTestCase(String moduleName, String testName, boolean passed) throws SQLException {
        if (moduleName == null || moduleName.isBlank() || testName == null || testName.isBlank()) {
            throw new IllegalArgumentException("Module name and test name cannot be blank.");
        }
        String sql = "INSERT INTO test_cases (module_name, test_name, passed) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, moduleName.trim());
            ps.setString(2, testName.trim());
            ps.setInt(3, passed ? 1 : 0);
            ps.executeUpdate();
            return getLastInsertId();
        }
    }

    public int logDefect(int testCaseId, String severity, int priority, String summary) throws SQLException {
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Priority must be between 1 and 5.");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Defect summary cannot be blank.");
        }
        String sql = "INSERT INTO defects (test_case_id, severity, priority, summary) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testCaseId);
            ps.setString(2, severity == null ? "MEDIUM" : severity.trim().toUpperCase());
            ps.setInt(3, priority);
            ps.setString(4, summary.trim());
            ps.executeUpdate();
            return getLastInsertId();
        }
    }

    private int getLastInsertId() throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Unable to fetch last inserted row ID.");
        }
    }

    public double getPassRate() throws SQLException {
        String sql = "SELECT COUNT(*) AS total, SUM(passed) AS passed_count FROM test_cases";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                if (total == 0) {
                    return 0.0;
                }
                int passed = rs.getInt("passed_count");
                return (passed * 100.0) / total;
            }
            return 0.0;
        }
    }

    public Map<String, Integer> getFailureCountsByModule() throws SQLException {
        String sql =
            "SELECT module_name, COUNT(*) AS fail_count " +
            "FROM test_cases " +
            "WHERE passed = 0 " +
            "GROUP BY module_name " +
            "ORDER BY fail_count DESC, module_name ASC";

        Map<String, Integer> failures = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                failures.put(rs.getString("module_name"), rs.getInt("fail_count"));
            }
        }
        return failures;
    }

    public List<DefectTicket> getDefectsBySeverity(String severity) throws SQLException {
        String sql =
            "SELECT d.id, t.module_name, t.test_name, d.severity, d.priority, d.summary " +
            "FROM defects d " +
            "INNER JOIN test_cases t ON d.test_case_id = t.id " +
            "WHERE UPPER(d.severity) = UPPER(?) " +
            "ORDER BY d.priority ASC, d.id ASC";

        List<DefectTicket> tickets = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, severity.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(new DefectTicket(
                        rs.getInt("id"),
                        rs.getString("module_name"),
                        rs.getString("test_name"),
                        rs.getString("severity"),
                        rs.getInt("priority"),
                        rs.getString("summary")
                    ));
                }
            }
        }
        return tickets;
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
