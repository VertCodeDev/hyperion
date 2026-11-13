package dev.vertcode.hyperion.skin;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinTest {

    private static final int RED = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;
    private static final int FAINT = 0x10FFFFFF;

    @Test
    void readsTheVariantFromANamesEnd() {
        assertEquals(SkinVariant.SLIM, SkinVariant.ofName("right_arm_slim"));
        assertEquals(SkinVariant.WIDE, SkinVariant.ofName("Right_Arm_WIDE"));
        assertEquals(SkinVariant.WIDE, SkinVariant.ofName("left_arm_classic"));
        assertEquals(SkinVariant.ANY, SkinVariant.ofName("slim_shady"));
    }

    @Test
    void knowsTheOuterLayer() {
        assertFalse(SkinLayout.outer(8, 8));
        assertTrue(SkinLayout.outer(40, 8));
        assertFalse(SkinLayout.outer(20, 20));
        assertTrue(SkinLayout.outer(20, 36));
        assertFalse(SkinLayout.outer(20, 52));
        assertTrue(SkinLayout.outer(4, 52));
        assertTrue(SkinLayout.outer(52, 52));
    }

    @Test
    void mirrorsTheRightLimbsOfAnOldSkin() {
        BufferedImage old = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        old.setRGB(4, 20, RED);
        old.setRGB(40, 20, BLUE);
        HyperionSkin skin = HyperionSkin.of(old, false);

        // The leg's front face flips in place; the arm's outer side becomes the inner side.
        assertEquals(RED, skin.pixel(23, 52));
        assertEquals(BLUE, skin.pixel(43, 52));
        assertEquals(RED, skin.pixel(4, 20));
    }

    @Test
    void refusesAnImageThatIsNotASkin() {
        assertThrows(IllegalArgumentException.class, () -> HyperionSkin.of(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), false));
    }

    @Test
    void colorsEveryPixelAndFlagsTheOnesThatShow() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(8, 8, RED);
        image.setRGB(40, 8, BLUE);
        image.setRGB(41, 8, FAINT);
        HyperionSkin slim = HyperionSkin.of(image, true);
        SkinMap map = new SkinMap(
                List.of(new SkinPixel(8, 8, SkinVariant.ANY)),
                List.of(
                        new SkinPixel(40, 8, SkinVariant.ANY),
                        new SkinPixel(41, 8, SkinVariant.ANY),
                        new SkinPixel(44, 20, SkinVariant.SLIM),
                        new SkinPixel(44, 20, SkinVariant.WIDE)
                )
        );
        SkinColors colors = slim.colors(map);

        assertEquals(5, colors.colors().size());
        assertEquals(RED, colors.colors().getFirst());
        assertEquals(List.of(true, false, true, false), colors.flags());
        assertSame(colors, slim.colors(map));
    }

    @Test
    void paintsAPlainSkinWithoutAnOuterLayer() {
        HyperionSkin plain = HyperionSkin.plain();

        assertEquals(0xFF, plain.pixel(8, 8) >>> 24);
        assertEquals(0, plain.pixel(40, 8));
        assertEquals(0, plain.pixel(-1, 0));
    }
}
