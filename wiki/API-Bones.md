# API Bones

A bone is a Blockbench group, live on a model. You can change how it looks, move it on top of its
animation, and show an item in its place.

## Getting bones

```java
Optional<Bone> head = model.bone("head");
List<Bone> all = model.bones();
List<Bone> glowing = model.bones(Bone::glowing);
```

| Method | What it gives |
|---|---|
| `name()` | The bone's name from Blockbench |
| `parent()`, `children()`, `descendants()` | Its place in the tree |
| `location()` | Where its pivot is in the world, animation included |
| `rotation()` | Its rotation in the world, animation included |
| `blueprint()` | The loaded bone: its metadata, and whether it has cubes |
| `behaviors()`, `behavior(type)` | Its behaviors, see [API Behaviors](API-Behaviors) |

`location()` and `rotation()` are as of the last frame, and safe to read from any thread.

## Look

Every setter is safe from any thread, takes effect on the next frame, and returns the bone so
calls chain.

```java
head.tint(Color.RED)
        .glowing(true)
        .glowColor(Color.ORANGE);

head.visible(false);     // hide the bone
head.tint(null);         // back to no tint
```

| Setter | What it does |
|---|---|
| `visible(boolean)` | Shows or hides the bone |
| `tint(Color)` | Colors it. On 1.21 to 1.21.3 this needs a dyeable `pack.base-item`. |
| `glowing(boolean)`, `glowColor(Color)` | The outline, and its color |
| `glint(boolean)` | The enchantment shimmer |
| `brightness(Display.Brightness)` | A fixed light level, so it shows in the dark |
| `billboard(Display.Billboard)` | Makes it face the camera |

The same setters exist on the whole model through `SpawnOptions`.

## Moving a bone

`transform` offsets, turns or stretches a bone on top of whatever the animation does. Its children
follow.

```java
head.transform(BoneTransform.ofScale(1.5f));
head.transform(BoneTransform.ofTranslation(0f, 0.25f, 0f));
head.transform(BoneTransform.ofRotation(new Quaternionf().rotateY((float) Math.toRadians(45))));

head.transform(BoneTransform.IDENTITY);   // clear it
```

A `BoneTransform` is a translation, a rotation and a scale. Build on one with `withTranslation`,
`withRotation` and `withScale`.

## Modifiers: moving a bone every frame

A modifier changes a bone's transform every frame. Use it for things that keep moving, like a
tail that sways or a turret that tracks a target.

```java
Key key = Key.key("myplugin", "wobble");

tail.modifier(key, (bone, transform) -> {
    float angle = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.3f;
    return transform.withRotation(new Quaternionf(transform.rotation()).rotateY(angle));
});

tail.removeModifier(key);
```

A modifier gets the bone and its transform so far, and returns the new one. It runs inside the
model's frame, on the thread that owns the model, every tick: keep it cheap and do not block.
Adding one with a key that is already used replaces the old one.

Modifiers and bone transforms reach Java players only. Bedrock players see animations, speed and
scale.

## Showing an item on a bone

A bone can show an item instead of its own cubes. The item moves with the bone's pivot and
animation, which is handy for a held sword or a hat you can swap.

```java
hand.item(new ItemStack(Material.DIAMOND_SWORD));
hand.item(null);                     // the bone's own cubes again
hand.item(ItemStack.empty());        // nothing at all
```

An `ItemSource` picks the item for you, so it can follow something:

| Source | What it shows |
|---|---|
| `ItemSource.fixed(item)` | One item |
| `ItemSource.mainHand()`, `ItemSource.offHand()` | What the entity under the model holds |
| `ItemSource.equipment(slot)` | What the entity wears in a slot |
| `ItemSource.perViewer(player -> item)` | A different item for each player looking |
| `ItemSource.of(context -> item)` | Anything you work out |

```java
hat.itemSource(ItemSource.equipment(EquipmentSlot.HEAD));
trophy.itemSource(ItemSource.perViewer(player -> player.getInventory().getItemInMainHand()));
```

A source is asked again every few ticks. `source.every(ticks)` sets how often.

## Locators

A locator is a named point from Blockbench, with a direction. Effects attach to them, and you can
read where one is:

```java
model.locator("mouth").ifPresent(mouth -> {
    Location where = mouth.location();
    world.spawnParticle(Particle.FLAME, where, 5);
});
```

To play an effect at a locator, see [API Effects](API-Effects).
