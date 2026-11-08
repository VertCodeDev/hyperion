package dev.vertcode.hyperion.render;

import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.EntityPart;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HiddenPartsTest {

    private static final byte BURNING_AND_GLOWING = (byte) (DisplayData.ON_FIRE | DisplayData.GLOWING | 0x02);

    @Test
    void hidesTheBodyFireAndOutline() {
        byte flags = HiddenParts.flags(BURNING_AND_GLOWING, EntityPart.ALL);

        assertEquals((byte) (DisplayData.INVISIBLE | 0x02), flags);
    }

    @Test
    void leavesWhatIsNotHidden() {
        assertEquals(BURNING_AND_GLOWING, HiddenParts.flags(BURNING_AND_GLOWING, EntityPart.NONE));
        assertEquals(BURNING_AND_GLOWING, HiddenParts.flags(BURNING_AND_GLOWING, Set.of(EntityPart.NAME, EntityPart.EQUIPMENT)));

        byte withoutFire = HiddenParts.flags(BURNING_AND_GLOWING, Set.of(EntityPart.FIRE));
        assertEquals((byte) (DisplayData.GLOWING | 0x02), withoutFire);
    }
}
