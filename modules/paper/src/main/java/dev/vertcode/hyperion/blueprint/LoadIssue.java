package dev.vertcode.hyperion.blueprint;

/**
 * One problem found while loading a model.
 *
 * @param severity how bad it is
 * @param where the part of the model it is about, such as {@code cube 'jaw'}, or the file
 * @param message what is wrong, written for the person who made the model
 */
public record LoadIssue(IssueSeverity severity, String where, String message) {

    private static final String FORMAT = "%s: %s";

    /**
     * Return the issue as one line for a log or a chat message.
     *
     * @return the place and the message
     */
    public String describe() {
        return String.format(FORMAT, this.where, this.message);
    }
}
