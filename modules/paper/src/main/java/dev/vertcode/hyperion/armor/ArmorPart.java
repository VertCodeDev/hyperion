package dev.vertcode.hyperion.armor;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A box vanilla draws armor on, other than the head: the body, the arms and the legs.
 *
 * <p>Numbers are vanilla's: where the box sits from its limb's pivot in the model's own space,
 * which has Y pointing down and X to the player's left, and where its faces are on the 64 by 32
 * armor texture. The left arm and leg are the right ones mirrored.
 */
public enum ArmorPart {

    /**
     * The body.
     */
    BODY(0, new float[] {0, 24, 0, -4, 0, -2, 8, 12, 4}, 16, 16, false),

    /**
     * The right arm.
     */
    RIGHT_ARM(1, new float[] {5, 22, 0, -3, -2, -2, 4, 12, 4}, 40, 16, false),

    /**
     * The left arm.
     */
    LEFT_ARM(2, new float[] {-5, 22, 0, -1, -2, -2, 4, 12, 4}, 40, 16, true),

    /**
     * The right leg.
     */
    RIGHT_LEG(3, new float[] {1.9f, 12, 0, -2, 0, -2, 4, 12, 4}, 0, 16, false),

    /**
     * The left leg.
     */
    LEFT_LEG(4, new float[] {-1.9f, 12, 0, -2, 0, -2, 4, 12, 4}, 0, 16, true);

    private final int index;
    private final float pivotX;
    private final float pivotY;
    private final float pivotZ;
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float sizeX;
    private final float sizeY;
    private final float sizeZ;
    private final int u;
    private final int v;
    private final boolean mirror;

    /**
     * Describe a box.
     *
     * @param index the box's number, which the shader knows it by
     * @param box the pivot, the lowest corner and the size, three numbers each
     * @param u where its faces start on the texture, horizontally
     * @param v where its faces start on the texture, vertically
     * @param mirror whether vanilla draws it mirrored
     */
    ArmorPart(final int index, final float[] box, final int u, final int v, final boolean mirror) {
        this.index = index;
        this.pivotX = box[0];
        this.pivotY = box[1];
        this.pivotZ = box[2];
        this.minX = box[3];
        this.minY = box[4];
        this.minZ = box[5];
        this.sizeX = box[6];
        this.sizeY = box[7];
        this.sizeZ = box[8];
        this.u = u;
        this.v = v;
        this.mirror = mirror;
    }

    /**
     * Return the limb's pivot in Blockbench's space, where a set's bones should pivot too.
     *
     * @return the pivot, in pixels
     */
    public Vector3fc pivot() {
        return new Vector3f(this.pivotX, this.pivotY, this.pivotZ);
    }

    /**
     * Return the box's lowest corner from the pivot, in the model's space, before growing.
     *
     * @return the corner, in pixels
     */
    public Vector3fc min() {
        return new Vector3f(this.minX, this.minY, this.minZ);
    }

    /**
     * Return the box's size, before growing.
     *
     * @return the size, in pixels
     */
    public Vector3fc size() {
        return new Vector3f(this.sizeX, this.sizeY, this.sizeZ);
    }

    /**
     * Return where the box's faces start on the armor texture, horizontally.
     *
     * @return the U offset, in texture pixels
     */
    public int u() {
        return this.u;
    }

    /**
     * Return where the box's faces start on the armor texture, vertically.
     *
     * @return the V offset, in texture pixels
     */
    public int v() {
        return this.v;
    }

    /**
     * Return whether vanilla draws this box mirrored.
     *
     * @return true for the left arm and leg
     */
    public boolean mirror() {
        return this.mirror;
    }

    /**
     * Return the box's number, which the shader knows it by.
     *
     * @return the number, from 0
     */
    public int index() {
        return this.index;
    }

    /**
     * Return this part's bit in a mask of parts.
     *
     * @return the bit
     */
    public int bit() {
        return 1 << this.index;
    }
}
