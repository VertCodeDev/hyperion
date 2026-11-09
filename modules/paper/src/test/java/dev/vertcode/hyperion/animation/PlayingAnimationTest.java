package dev.vertcode.hyperion.animation;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.api.model.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayingAnimationTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void wrapsALoopingAnimation() {
        PlayingAnimation playing = playing(LoopMode.LOOP, 1f, 0, 0, 0);
        advance(playing, 25);

        assertTrue(playing.isPlaying());
        assertEquals(5f, playing.frame(), EPSILON);
    }

    @Test
    void endsAnAnimationPlayedOnce() {
        PlayingAnimation playing = playing(LoopMode.ONCE, 1f, 0, 0, 0);
        advance(playing, 19);
        assertTrue(playing.isPlaying());

        advance(playing, 1);
        assertFalse(playing.isPlaying());
        assertTrue(playing.finished());
    }

    @Test
    void holdsTheLastFrame() {
        PlayingAnimation playing = playing(LoopMode.HOLD, 2f, 0, 0, 0);
        advance(playing, 40);

        assertTrue(playing.isPlaying());
        assertEquals(20f, playing.frame(), EPSILON);
    }

    @Test
    void fadesInAndOut() {
        PlayingAnimation playing = playing(LoopMode.LOOP, 1f, 4, 4, 0);
        advance(playing, 2);
        assertEquals(0.5f, playing.weight(), EPSILON);

        advance(playing, 4);
        assertEquals(1f, playing.weight(), EPSILON);

        playing.stop();
        advance(playing, 2);
        assertEquals(0.5f, playing.weight(), EPSILON);
        assertTrue(playing.isPlaying());

        advance(playing, 2);
        assertFalse(playing.isPlaying());
        assertFalse(playing.finished());
    }

    @Test
    void waitsForTheStartDelay() {
        PlayingAnimation playing = playing(LoopMode.LOOP, 1f, 0, 0, 5);
        advance(playing, 5);

        assertFalse(playing.applies());
        assertEquals(0f, playing.frame(), EPSILON);

        advance(playing, 1);
        assertTrue(playing.applies());
    }

    @Test
    void passesEveryEffectKeyframeOncePerRunLoopsIncluded() {
        PlayingAnimation playing = playing(LoopMode.LOOP, 1f, 0, 0, 0);
        int[] keyframes = {0, 10, 20};
        int[] fired = new int[keyframes.length];
        for (int i = 0; i < 45; i++) {
            playing.advance();
            for (int k = 0; k < keyframes.length; k++) {
                if (playing.passed(keyframes[k])) {
                    fired[k]++;
                }
            }
        }

        // 45 ticks of a 20 tick loop: the start and two wraps reach tick 0, two runs reach 10 and 20.
        assertEquals(3, fired[0]);
        assertEquals(2, fired[1]);
        assertEquals(2, fired[2]);
    }

    @Test
    void passesKeyframesItSkipsOverAtSpeed() {
        PlayingAnimation playing = playing(LoopMode.ONCE, 3f, 0, 0, 0);
        int fired = 0;
        for (int i = 0; i < 10; i++) {
            playing.advance();
            if (playing.passed(7)) {
                fired++;
            }
        }

        assertEquals(1, fired);
    }

    private static PlayingAnimation playing(
            final LoopMode loop,
            final float speed,
            final int fadeIn,
            final int fadeOut,
            final int startDelay
    ) {
        Model model = (Model) Proxy.newProxyInstance(
                Model.class.getClassLoader(),
                new Class<?>[] {Model.class},
                (proxy, method, arguments) -> null
        );
        ModelAnimator animator = new ModelAnimator(model, name -> Optional.empty(), null, Logger.getAnonymousLogger());
        BakedAnimation baked = new BakedAnimation("test", loop, 20f, false, startDelay, 0, Map.of(), List.of());
        PlaySettings settings = new PlaySettings(loop);
        settings.speed(speed).fadeIn(fadeIn).fadeOut(fadeOut);
        PlayingAnimation playing = new PlayingAnimation(animator, baked, settings);
        playing.markStarted();

        return playing;
    }

    private static void advance(final PlayingAnimation playing, final int ticks) {
        for (int i = 0; i < ticks; i++) {
            playing.advance();
        }
    }
}
