# Developer API

The api lets your plugin spawn, change and listen to Hyperion models, play effects and work with
custom armor. It holds interfaces and small data types only; the Hyperion plugin implements them.

> [!IMPORTANT]
> The api is not published to a Maven repository yet. Until it is, build Hyperion and depend on
> `modules/api/build/libs/api-<version>.jar`. Once published, it will be
> `compileOnly("dev.vertcode.hyperion:hyperion-api:VERSION")`.

## Setup

Depend on Hyperion so it loads before your plugin.

`paper-plugin.yml`:

```yaml
dependencies:
  server:
    Hyperion:
      load: BEFORE
      required: true
      join-classpath: true
```

`plugin.yml`:

```yaml
depend: [Hyperion]
```

Everything starts at the `Hyperion` class:

| Call | What it gives |
|---|---|
| `Hyperion.models()` | Model blueprints: find, register, reload |
| `Hyperion.tracker()` | Live models: by entity, by id, near a location |
| `Hyperion.effects()` | Effects: find, register, play |
| `Hyperion.armor()` | Armor sets, items and what a player wears |
| `Hyperion.behaviors()` | Bone behaviors, and registering your own |
| `Hyperion.formats()` | Model file formats |

## Spawning a model

```java
ModelBlueprint dragon = Hyperion.models().find(Key.key("hyperion", "bosses/dragon")).orElseThrow();

// On an entity: the model follows it.
Model boss = dragon.attach(zombie, options -> options.scale(2.5f).viewDistance(64));

// On its own, at a location.
Model statue = dragon.spawn(location, options -> options.glowing(true));
```

`SpawnOptions` also sets the tint, glow color, brightness, a starting animation, who can see the
model, and whether the automatic idle and walk animations run.

Models are not saved. To keep one over a restart, spawn it again yourself, for models on entities
in `EntitiesLoadEvent`.

## Changing a live model

```java
boss.scale(3f);
boss.bone("head").ifPresent(head -> head.tint(Color.RED).glowing(true));
boss.teleport(location);     // for a model on its own
boss.remove();
```

Find live models with the tracker:

```java
List<Model> onZombie = Hyperion.tracker().of(zombie);
List<Model> nearby = Hyperion.tracker().near(location, 16);
```

## Animations

```java
AnimationHandle roar = boss.animator().play("roar");

boss.animator().play("walk", options -> options
        .speed(1.5f)
        .loop(LoopMode.LOOP)
        .fadeIn(5)
        .fadeOut(5));

roar.onEnd(() -> boss.animator().play("idle"));
boss.animator().stop("walk");
```

`LoopMode` is `ONCE`, `LOOP` or `HOLD` (stay on the last frame).

## Per-player views

A player's view of a model has its own animator and look. What you set there, only that player
sees, on top of what everyone sees.

```java
PlayerView view = boss.view(player);
view.animator().play("wave");
view.reset();   // back to what everyone sees
```

Who sees a model at all:

```java
boss.viewers().policy(ViewerPolicy.permission("myplugin.see.boss"));
boss.viewers().hide(player);
```

## Seats and heads

```java
for (SeatBehavior seat : boss.seats()) {
    seat.mount(player);
    seat.locked(true);    // sneaking no longer gets them off
}

boss.behaviors(HeadBehavior.class).forEach(head -> head.target(player.getLocation()));
```

## Effects

```java
Hyperion.effects().play(Key.key("hyperion", "level_up"), EffectAnchor.on(boss));

Hyperion.effects().play(Key.key("hyperion", "aura"), EffectAnchor.on(player), options -> options
        .parameter("color", "#FF5555")
        .viewers(EffectViewers.nearby(32)));
```

An anchor is a location (`EffectAnchor.at`), an entity or model (`on`), or a bone or locator of a
model (`bone`, `locator`).

Ship effects with your plugin by registering a folder of effect files in your own namespace:

```java
Hyperion.effects().registerFolder(this, "myplugin", getDataPath().resolve("effects"));
```

## Your own models

Register models from your plugin's jar or its data folder. They get your namespace:

```java
Hyperion.models()
        .register(this, Key.key("myplugin", "golem"), ModelSource.resource(this, "models/golem.bbmodel"))
        .thenAccept(blueprint -> getLogger().info("Loaded " + blueprint.key()));
```

Loading runs off the server thread, so `register` returns a future. Hyperion rebuilds the resource
pack for you.

## Armor

```java
ArmorSet frost = Hyperion.armor().find(Key.key("hyperion", "frost")).orElseThrow();

// Items
frost.piece(ArmorSlot.HEAD).ifPresent(helmet -> player.getInventory().addItem(helmet.item()));

// What a player wears
WornArmor worn = Hyperion.armor().worn(player);
if (worn.hasFullSet(frost)) {
    // ...
}

// Is this item custom armor?
Hyperion.armor().pieceOf(item).ifPresent(piece -> { /* piece.set(), piece.slot(), piece.stats() */ });

// Values from the set's .yml "data" section
String element = frost.data().getString("element", "none");
```

A set bonus is a few lines with the armor events:

```java
@EventHandler
public void onFullSet(ArmorSetCompleteEvent event) {
    if (event.set().key().asString().equals("hyperion:frost")) {
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0));
    }
}

@EventHandler
public void onSetBroken(ArmorSetBreakEvent event) {
    if (event.set().key().asString().equals("hyperion:frost")) {
        event.getPlayer().removePotionEffect(PotionEffectType.SPEED);
    }
}
```

See [API Events](API-Events) for every event.

## Threading

Hyperion runs on Folia and Canvas, so the rules are the same on every platform:

- **`spawn`, `attach`, `teleport` and `remove`** must run on the thread that owns the location or
  entity. On Folia, use that entity's or region's scheduler. On Paper, that is the main thread.
- **Getters and appearance setters** (scale, tint, glow, animations) are safe from any thread and
  take effect on the model's next frame.
- **Events** fire on the thread that owns the model or player, except the two load events, which
  are async.

## Null safety

Every package is `@NullMarked` with [JSpecify](https://jspecify.dev): nothing is null unless it is
marked `@Nullable`. IntelliJ and Kotlin read these annotations.

## Checking the server version

`Feature` says what the running server has, so you do not compare version numbers:

```java
if (Feature.EQUIPPABLE.available()) {
    // 1.21.2 and later
}
```
