package dev.vertcode.hyperion.render;

import java.util.ArrayList;
import java.util.List;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import net.kyori.adventure.text.Component;

/**
 * The entity data of the entities behaviors place at bones: the interaction a hitbox is and the
 * text display a name tag is.
 *
 * <p>The indexes are the ones every version from 1.20.2 on uses.
 */
public final class MarkerData {

    private static final int INTERACTION_WIDTH = 8;
    private static final int INTERACTION_HEIGHT = 9;
    private static final int INTERACTION_RESPONSIVE = 10;
    private static final int TELEPORT_DURATION = 10;
    private static final int BILLBOARD = 15;
    private static final int TEXT = 23;
    private static final int LINE_WIDTH = 24;
    private static final int BACKGROUND = 25;
    private static final int TEXT_FLAGS = 27;
    private static final byte CENTER = 3;
    private static final byte SEE_THROUGH = 0x02;
    private static final int NAME_BACKGROUND = 0x40000000;
    private static final int LINE_WIDTH_PIXELS = 200;

    private MarkerData() {
    }

    /**
     * Build the size of an interaction, which also makes the player's arm swing on a click.
     *
     * @param width the width on x and z in blocks
     * @param height the height in blocks
     * @return the data
     */
    public static List<EntityData<?>> interaction(final float width, final float height) {
        List<EntityData<?>> data = new ArrayList<>(3);
        data.add(new EntityData<>(INTERACTION_WIDTH, EntityDataTypes.FLOAT, width));
        data.add(new EntityData<>(INTERACTION_HEIGHT, EntityDataTypes.FLOAT, height));
        data.add(new EntityData<>(INTERACTION_RESPONSIVE, EntityDataTypes.BOOLEAN, true));

        return data;
    }

    /**
     * Build everything a name tag needs when it spawns: it faces the viewer, with the dark
     * background of a vanilla name.
     *
     * @param text the text
     * @param seeThrough whether it shows through blocks
     * @param interpolation over how many ticks moves are smoothed
     * @return the data
     */
    public static List<EntityData<?>> nametag(final Component text, final boolean seeThrough, final int interpolation) {
        List<EntityData<?>> data = new ArrayList<>(6);
        data.add(new EntityData<>(TELEPORT_DURATION, EntityDataTypes.INT, interpolation));
        data.add(new EntityData<>(BILLBOARD, EntityDataTypes.BYTE, CENTER));
        data.add(new EntityData<>(LINE_WIDTH, EntityDataTypes.INT, LINE_WIDTH_PIXELS));
        data.add(new EntityData<>(BACKGROUND, EntityDataTypes.INT, NAME_BACKGROUND));
        data.addAll(text(text, seeThrough));

        return data;
    }

    /**
     * Build a change of a name tag's text.
     *
     * @param text the text
     * @param seeThrough whether it shows through blocks
     * @return the data
     */
    public static List<EntityData<?>> text(final Component text, final boolean seeThrough) {
        List<EntityData<?>> data = new ArrayList<>(2);
        data.add(new EntityData<>(TEXT, EntityDataTypes.ADV_COMPONENT, text));
        data.add(new EntityData<>(TEXT_FLAGS, EntityDataTypes.BYTE, seeThrough ? SEE_THROUGH : (byte) 0));

        return data;
    }
}
