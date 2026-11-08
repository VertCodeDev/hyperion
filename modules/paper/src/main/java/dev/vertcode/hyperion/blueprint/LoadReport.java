package dev.vertcode.hyperion.blueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every problem found while loading one model file.
 *
 * <p>One load task fills a report and then hands it over; after that it is only read. The hand
 * over goes through a future or a concurrent map, which makes the filled list visible to the
 * reading thread.
 */
public final class LoadReport {

    private static final String FILE = "file";

    private final String source;
    private final List<LoadIssue> issues = new ArrayList<>();

    /**
     * Start an empty report.
     *
     * @param source the name of the file being loaded
     */
    public LoadReport(final String source) {
        this.source = source;
    }

    /**
     * Return the name of the file this report is about.
     *
     * @return the file name
     */
    public String source() {
        return this.source;
    }

    /**
     * Note something that will not look or behave as it does in Blockbench.
     *
     * @param where the part of the model it is about
     * @param message what is wrong
     */
    public void warn(final String where, final String message) {
        this.issues.add(new LoadIssue(IssueSeverity.WARNING, where, message));
    }

    /**
     * Note something that stops the model from loading.
     *
     * @param where the part of the model it is about
     * @param message what is wrong
     */
    public void error(final String where, final String message) {
        this.issues.add(new LoadIssue(IssueSeverity.ERROR, where, message));
    }

    /**
     * Note something about the whole file that stops it from loading.
     *
     * @param message what is wrong
     */
    public void error(final String message) {
        this.error(FILE, message);
    }

    /**
     * Return every issue, in the order they were found.
     *
     * @return the issues
     */
    public List<LoadIssue> issues() {
        return Collections.unmodifiableList(this.issues);
    }

    /**
     * Return how many issues have a severity.
     *
     * @param severity the severity to count
     * @return the count
     */
    public int count(final IssueSeverity severity) {
        int count = 0;
        for (LoadIssue issue : this.issues) {
            if (issue.severity() == severity) {
                count++;
            }
        }

        return count;
    }

    /**
     * Return whether the model cannot load.
     *
     * @return true when there is at least one error
     */
    public boolean hasErrors() {
        return this.count(IssueSeverity.ERROR) > 0;
    }
}
