package dev.vertcode.hyperion.armor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.armor.ArmorData;
import dev.vertcode.hyperion.api.armor.ArmorPiece;
import dev.vertcode.hyperion.api.armor.ArmorSet;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.ArmorStats;
import dev.vertcode.hyperion.blueprint.data.Group;
import dev.vertcode.hyperion.blueprint.data.ModelData;

/**
 * A loaded armor set: its model, its settings and its pieces.
 *
 * <p>Immutable, so any thread may read it. Its pieces build items through the shared
 * {@link ArmorItems}, which learns the pack's numbers after every build.
 */
public final class HyperionArmorSet implements ArmorSet {

    private final Key key;
    private final String displayName;
    private final ArmorMaterial material;
    private final boolean dyeable;
    private final ArmorData data;
    private final ModelData model;
    private final Map<ArmorBone, Group> bones;
    private final ArmorTextures textures;
    private final Map<ArmorSlot, HyperionArmorPiece> pieces = new EnumMap<>(ArmorSlot.class);

    /**
     * Create a set.
     *
     * @param key the set's key
     * @param displayName its name for players, in MiniMessage
     * @param material the vanilla armor it is made from
     * @param stats the stats of each piece it has
     * @param dyeable whether players may dye it
     * @param data the free values for plugins
     * @param model the set's model
     * @param bones the armor bones the model has
     * @param items builds the pieces' items
     */
    public HyperionArmorSet(
            final Key key,
            final String displayName,
            final ArmorMaterial material,
            final Map<ArmorSlot, ArmorStats> stats,
            final boolean dyeable,
            final ArmorData data,
            final ModelData model,
            final Map<ArmorBone, Group> bones,
            final ArmorItems items
    ) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.dyeable = dyeable;
        this.data = data;
        this.model = model;
        this.bones = Collections.unmodifiableMap(new EnumMap<>(bones));
        this.textures = new ArmorTextures(model.textures());
        for (ArmorBone bone : bones.keySet()) {
            ArmorSlot slot = bone.slot();
            if (!this.pieces.containsKey(slot)) {
                ArmorStats pieceStats = stats.getOrDefault(slot, material.stats(slot));
                this.pieces.put(slot, new HyperionArmorPiece(this, slot, pieceStats, items));
            }
        }
    }

    @Override
    public Key key() {
        return this.key;
    }

    @Override
    public String displayName() {
        return this.displayName;
    }

    @Override
    public Collection<ArmorPiece> pieces() {
        return Collections.unmodifiableList(new ArrayList<>(this.pieces.values()));
    }

    @Override
    public Optional<ArmorPiece> piece(final ArmorSlot slot) {
        return Optional.ofNullable(this.pieces.get(slot));
    }

    @Override
    public boolean dyeable() {
        return this.dyeable;
    }

    @Override
    public ArmorData data() {
        return this.data;
    }

    /**
     * Return the vanilla armor this set is made from.
     *
     * @return the material
     */
    public ArmorMaterial material() {
        return this.material;
    }

    /**
     * Return the set's model.
     *
     * @return the model data
     */
    public ModelData model() {
        return this.model;
    }

    /**
     * Return the armor bones the model has.
     *
     * @return the bones' groups, by bone
     */
    public Map<ArmorBone, Group> bones() {
        return this.bones;
    }

    /**
     * Return the bones of one piece.
     *
     * @param slot the piece's slot
     * @return the bones, in bone order
     */
    public List<ArmorBone> bones(final ArmorSlot slot) {
        List<ArmorBone> list = new ArrayList<>();
        for (ArmorBone bone : this.bones.keySet()) {
            if (bone.slot() == slot) {
                list.add(bone);
            }
        }

        return list;
    }

    /**
     * Return what each texture is for.
     *
     * @return the textures
     */
    public ArmorTextures textures() {
        return this.textures;
    }

    /**
     * Return the set's file name part of its key, used for its asset names.
     *
     * @return the key's value, such as {@code frost}
     */
    public String id() {
        return this.key.value();
    }

    @Override
    public String toString() {
        return "ArmorSet[" + this.key.asString() + ", pieces=" + this.pieces.keySet() + "]";
    }
}
