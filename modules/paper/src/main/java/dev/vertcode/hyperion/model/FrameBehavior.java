package dev.vertcode.hyperion.model;

import java.util.List;

import com.github.retrooper.packetevents.protocol.player.User;

/**
 * A built-in behavior that places entities of its own at its bone after every frame, once the
 * bones are where this frame put them.
 *
 * <p>Runs on the thread that owns the model, only while the model has viewers.
 */
public interface FrameBehavior {

    /**
     * Place the behavior's entities for this frame.
     *
     * @param viewers the connections of every Java player who sees the model now; Bedrock
     *     players are left out, Geyser draws the model for them
     */
    void afterFrame(List<User> viewers);
}
