# Custom Armor

Hyperion turns a Blockbench file into an armor set players wear in the normal armor slots. The
items are real armor underneath, so enchanting, repairing and dyeing work as players expect.

> [!NOTE]
> The 3D look for chestplates, leggings and boots is new and off by default. Flat armor and 3D
> helmets work without it. See [The 3D shader](#the-3d-shader).

## Making a set

1. On first start, Hyperion writes `plugins/Hyperion/armor-template.bbmodel`. Open it in
   Blockbench. Its bones sit where the player's limbs turn, so build on it.
2. Save your set as `plugins/Hyperion/armor/<name>.bbmodel`. The file name becomes the key:
   `armor/frost.bbmodel` is `hyperion:frost`.
3. Run `/hy reload`, then `/hy armor give <player> frost`.

## Bones

A set has a piece for each slot it has bones for. A file with only a `helmet` bone is a hat.

| Piece | Bones |
|---|---|
| Helmet | `helmet` |
| Chestplate | `chestplate`, `right_arm`, `left_arm` |
| Leggings | `leggings`, `right_leg`, `left_leg` |
| Boots | `right_boot`, `left_boot` |

Groups under a bone belong to it, with their rotations.

## Textures

Textures are told apart by name:

| Texture | What it is |
|---|---|
| `flat_outer` | The flat look of the helmet, chestplate and boots, on vanilla's 64 by 32 armor layout |
| `flat_inner` | The flat look of the leggings |
| `flat_outer_tint`, `flat_inner_tint` | The part of the flat look that takes the dye color |
| `flat_outer_glow`, `flat_inner_glow` | The part of the flat look that lights up, where the shader runs |
| `icon_<piece>` | A piece's inventory icon, such as `icon_helmet`. Without one, the icon is the piece in 3D. |
| Any other | The model's own texture. A name ending in `_tint` takes the dye color, one ending in `_glow` lights up. |

You do not have to draw a flat texture. Without one, Hyperion paints it by projecting your model
onto vanilla's armor shape.

## Name and stats

Put a `.yml` with the same name next to the model, such as `armor/frost.yml`. Every line is
optional:

```yaml
name: "<aqua>Frost"
base: netherite          # the vanilla armor the items are made from: sounds, enchanting, Bedrock
armor: [3, 8, 6, 3]      # helmet, chestplate, leggings, boots; the base's when left out
toughness: 2
knockback-resistance: 0
dyeable: false
data:                    # free values for plugins to read
  element: ice
```

Hyperion adds no set bonuses itself. Plugins build those on the armor events, see
[Developer API](Developer-API).

## What each version shows

| Version | Helmet | Chestplate, leggings, boots |
|---|---|---|
| 1.21 to 1.21.1 | 3D | Flat |
| 1.21.2 to 26.3 | 3D | Flat, or 3D with the shader |

The helmet is an item model, so its cubes snap to turns of 22.5 degrees on one axis.

On 1.21 to 1.21.1 the body pieces are leather armor in a color that names the set, so those pieces
cannot be dyed there.

## The 3D shader

```yaml
armor:
  # Draw chestplates, leggings and boots in 3D with a core shader, from 1.21.2.
  shader: false
  # How far from the camera armor is drawn in 3D, in blocks. Beyond it, flat.
  render-distance: 32
  # The most equipment layers a set may use for 3D.
  max-layers: 16
```

Set `armor.shader: true` and run `/hy reload` to try it. What to know first:

- **Players with Iris or OptiFine always see the flat look.** They do not run core shaders.
- **Each layer draws two faces per limb.** 16 layers give a limb 32 faces, about five cubes. The
  console says when a set needs more.
- **Arms can shift a little.** The shader follows the body and legs exactly. Arms also roll while
  idle and while swinging, which it cannot see, so faces far from the arm can move by a few pixels.
- **It clashes with other packs that replace the entity shader.**
- **Hyperion downloads the Minecraft client jar once**, from Mojang, into
  `plugins/Hyperion/cache`, and reads the vanilla shader from it. It never ships Mojang's files.
- **Players on another version through ViaVersion see the flat look**, not a broken shader.

## Giving armor

| Command | What it does |
|---|---|
| `/hy armor list` | Every armor set and its pieces |
| `/hy armor give <player> <set>` | Gives a whole set |
| `/hy armor give <player> <set> <piece>` | Gives one piece: `helmet`, `chestplate`, `leggings` or `boots` |

`/hy gallery --armor` lists every set in a menu: left-click gives you the set, right-click puts it
on.

Pieces are known by a tag on the item, so renaming, enchanting, dyeing or trimming one keeps it
the same piece.
