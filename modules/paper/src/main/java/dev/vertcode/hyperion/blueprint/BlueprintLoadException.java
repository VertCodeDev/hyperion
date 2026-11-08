package dev.vertcode.hyperion.blueprint;

import java.io.Serial;

/**
 * A model that could not load. The report says why.
 */
public final class BlueprintLoadException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "%s could not load: %s";

    private final transient LoadReport report;

    /**
     * Create the exception.
     *
     * @param report the report holding at least one error
     */
    public BlueprintLoadException(final LoadReport report) {
        super(String.format(MESSAGE, report.source(), firstError(report)));
        this.report = report;
    }

    /**
     * Return the report that explains the failure.
     *
     * @return the report
     */
    public LoadReport report() {
        return this.report;
    }

    /**
     * Return the first error of a report, for the exception message.
     *
     * @param report the report
     * @return the first error, described
     */
    private static String firstError(final LoadReport report) {
        for (LoadIssue issue : report.issues()) {
            if (issue.severity() == IssueSeverity.ERROR) {
                return issue.describe();
            }
        }

        return "unknown error";
    }
}
