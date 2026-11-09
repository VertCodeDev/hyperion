package dev.vertcode.hyperion.debug;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.config.DebugSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugViewsTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private final DebugViews views = new DebugViews(new DebugSettings(4, true, 0.4f));

    @Test
    void togglesOneViewAtATime() {
        assertTrue(this.views.toggle(PLAYER, DebugView.BONES));
        assertTrue(this.views.toggle(PLAYER, DebugView.SEATS));
        assertEquals(Set.of(DebugView.BONES, DebugView.SEATS), this.views.views(PLAYER));

        assertFalse(this.views.toggle(PLAYER, DebugView.BONES));
        assertEquals(Set.of(DebugView.SEATS), this.views.views(PLAYER));
    }

    @Test
    void forgetsAPlayerWithNothingOn() {
        this.views.toggle(PLAYER, DebugView.HITBOXES);
        this.views.toggle(PLAYER, DebugView.HITBOXES);

        assertTrue(this.views.views(PLAYER).isEmpty());
    }

    @Test
    void switchesEveryViewAtOnce() {
        this.views.showAll(PLAYER);
        assertEquals(EnumSet.allOf(DebugView.class), this.views.views(PLAYER));

        this.views.hideAll(PLAYER);
        assertTrue(this.views.views(PLAYER).isEmpty());
    }

    @Test
    void readsViewsByTheirCommandName() {
        assertEquals(DebugView.LOCATORS, DebugView.fromKey("locators").orElseThrow());
        assertTrue(DebugView.fromKey("everything").isEmpty());
    }
}
