package dev.vertcode.hyperion.model;

/**
 * Something that runs inside a model's frame, every tick, on the thread that owns the model,
 * such as an effect played on it.
 */
public interface ModelTask {

    /**
     * Run one tick, after the model's bones moved.
     *
     * @return false when the task is done and leaves the model
     */
    boolean step();

    /**
     * Stop, because the model is removed.
     */
    void cancel();
}
