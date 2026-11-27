package dev.vertcode.hyperion.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Worlds that are copies of one another, which count as one world for who sees a model: set by
 * another plugin through {@code HyperionApi#worldGroups}. With nothing set, every world is its own.
 */
@NullMarked
public final class WorldLinks {

    private static volatile @Nullable Function<World, String> groupOf;

    private WorldLinks() {
    }

    /**
     * Set the group name of every world.
     *
     * @param lookup the group name of a world, or {@code null} to make every world its own again
     */
    public static void groupOf(final @Nullable Function<World, String> lookup) {
        groupOf = lookup;
    }

    /**
     * Returns whether two worlds are the same world or copies of one world.
     *
     * @param one a world
     * @param other another world
     * @return whether they are the same world or copies of one world
     */
    static boolean same(final @Nullable World one, final @Nullable World other) {
        if (one == null || other == null) {
            return false;
        }

        if (one.equals(other)) {
            return true;
        }

        Function<World, String> lookup = groupOf;
        if (lookup == null) {
            return false;
        }

        try {
            return lookup.apply(one).equals(lookup.apply(other));
        } catch (RuntimeException broken) {
            // A lookup that breaks leaves every world its own, rather than breaking the model.
            return false;
        }
    }

    /**
     * Returns the other loaded copies of a world.
     *
     * @param world a world
     * @return the other loaded copies of it, empty when it has none or nothing is set
     */
    static List<World> copiesOf(final World world) {
        if (groupOf == null) {
            return List.of();
        }

        List<World> copies = new ArrayList<>();
        for (World other : Bukkit.getWorlds()) {
            if (!other.equals(world) && same(world, other)) {
                copies.add(other);
            }
        }

        return copies;
    }

    /**
     * Returns the squared distance between two spots' coordinates, whichever copy each is in.
     *
     * @param one a spot
     * @param other a spot in the same world or a copy of it
     * @return the squared distance between their coordinates
     */
    static double distanceSquared(final Location one, final Location other) {
        double x = one.getX() - other.getX();
        double y = one.getY() - other.getY();
        double z = one.getZ() - other.getZ();

        return x * x + y * y + z * z;
    }
}
