package com.hkxy.triageqa;

public class DefectTicket {

    private final int id;
    private final String moduleName;
    private final String testName;
    private final String severity;
    private final int priority;
    private final String summary;

    public DefectTicket(int id, String moduleName, String testName, String severity, int priority, String summary) {
        this.id = id;
        this.moduleName = moduleName;
        this.testName = testName;
        this.severity = severity;
        this.priority = priority;
        this.summary = summary;
    }

    public int getId() {
        return id;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getTestName() {
        return testName;
    }

    public String getSeverity() {
        return severity;
    }

    public int getPriority() {
        return priority;
    }

    public String getSummary() {
        return summary;
    }
}
