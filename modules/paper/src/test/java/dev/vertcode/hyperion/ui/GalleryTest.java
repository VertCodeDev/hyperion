package dev.vertcode.hyperion.ui;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GalleryTest {

    private static final List<String> KEYS = List.of("hyperion:wolf", "shop:dragon", "hyperion:dragon_baby", "hyperion:cookie");

    @Test
    void sortsEverythingWithoutAFilter() {
        assertEquals(
                List.of("hyperion:cookie", "hyperion:dragon_baby", "hyperion:wolf", "shop:dragon"),
                Gallery.matching(KEYS, Function.identity(), null)
        );
    }

    @Test
    void keepsWhatContainsTheFilterIgnoringCase() {
        assertEquals(List.of("hyperion:dragon_baby", "shop:dragon"), Gallery.matching(KEYS, Function.identity(), " Dragon "));
        assertEquals(List.of(), Gallery.matching(KEYS, Function.identity(), "pig"));
    }
}
