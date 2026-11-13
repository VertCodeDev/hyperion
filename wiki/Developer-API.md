# Developer API

The api lets your plugin spawn, change and listen to Hyperion models, play effects and work with
custom armor. It holds interfaces and small data types only; the Hyperion plugin implements them.

> [!IMPORTANT]
> There is no release yet, so the api is not on Maven Central yet. Until the first release, build
> Hyperion and depend on `modules/api/build/libs/api-<version>.jar`, or run
> `./gradlew :api:publishToMavenLocal` and add `mavenLocal()` to your repositories.

## The pages

| Page | What it covers |
|---|---|
| [API Models](API-Models) | Finding, spawning and changing models, who sees them, your own model files |
| [API Bones](API-Bones) | Bones: look, transforms, modifiers, items, and locators |
| [API Animations](API-Animations) | Playing, looping, fading and stopping animations, per player too |
| [API Behaviors](API-Behaviors) | Heads, seats, hitboxes, name tags, and writing your own behavior |
| [API Effects](API-Effects) | Playing effects, building them in code, your own part types |
| [API Armor](API-Armor) | Armor sets, items, what a player wears, set bonuses |
| [API Player Models](API-Player-Models) | Animations on players in their own skin, and giving models a player's skin |
| [API Events](API-Events) | Every event, with examples |

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

Add the api as `compileOnly`. It is on Maven Central, with its sources and Javadoc, so your IDE
shows the documentation of every method.

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("dev.vertcode.hyperion:hyperion-api:VERSION")
}
```

```xml
<dependency>
    <groupId>dev.vertcode.hyperion</groupId>
    <artifactId>hyperion-api</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```

Do not shade it: the Hyperion plugin already carries it.

## The entry point

Everything starts at the `Hyperion` class:

| Call | What it gives |
|---|---|
| `Hyperion.models()` | Model blueprints: find, register, reload |
| `Hyperion.tracker()` | Live models: by entity, by id, near a location |
| `Hyperion.effects()` | Effects: find, register, play |
| `Hyperion.armor()` | Armor sets, items and what a player wears |
| `Hyperion.playerAnimations()` | Animations on players, in their own skin: play, stop, what is playing |
| `Hyperion.skins()` | Player skins for models to wear |
| `Hyperion.behaviors()` | Bone behaviors, and registering your own |
| `Hyperion.formats()` | Model file formats (experimental) |

These throw `IllegalStateException` when Hyperion is not enabled, so call them from `onEnable` or
later, not from a static initializer.

## A first example

```java
ModelBlueprint dragon = Hyperion.models().find(Key.key("hyperion", "bosses/dragon")).orElseThrow();

Model boss = dragon.attach(zombie, options -> options.scale(2.5f).viewDistance(64));
boss.bone("head").ifPresent(head -> head.tint(Color.RED).glowing(true));
boss.animator().play("roar");
```

## Two words to know

- A **blueprint** (`ModelBlueprint`) is a loaded model file: its bones, animations and locators.
  There is one per file.
- A **model** (`Model`) is one live copy of a blueprint in the world. There can be many.

Armor has the same split: an `ArmorSet` is a loaded file, an `ArmorPiece` is one slot of it.

## Threading

Hyperion runs on Folia and Canvas, so the rules are the same on every platform:

| What | Where to call it |
|---|---|
| `spawn`, `attach`, `teleport`, `remove` | On the thread that owns the location or entity. On Folia, that entity's or region's scheduler. On Paper, the main thread. |
| Getters | Any thread |
| Appearance setters: scale, tint, glow, bone transforms | Any thread. They take effect on the model's next frame. |
| Playing and stopping animations and effects | Any thread |
| Registries: find, register, reload | Any thread. Loading runs off the server thread and returns a future. |

Events fire on the thread that owns the model or player, except the two load events, which are
async. See [API Events](API-Events).

## Null safety

Every package is `@NullMarked` with [JSpecify](https://jspecify.dev): nothing is null unless it is
marked `@Nullable`. Things that may be missing come back as `Optional`. IntelliJ and Kotlin read
these annotations, so Kotlin sees `Color?` where the api says `@Nullable Color`.

## Checking the server version

`Feature` says what the running server has, so you do not compare version numbers:

```java
if (Feature.EQUIPPABLE.available()) {
    // 1.21.2 and later
}

MinecraftVersion version = MinecraftVersion.current();
```

| Feature | Since |
|---|---|
| `PLAIN_ATTRIBUTE_KEYS` | 1.21.2 |
| `PLAYER_INPUT_PACKET` | 1.21.2 |
| `EQUIPPABLE` | 1.21.2 |
| `ITEM_MODEL_DEFINITIONS` | 1.21.4 |
| `PACK_FORMAT_RANGE` | 1.21.9 |

## What is stable

Interfaces marked `@ApiStatus.NonExtendable` are for calling, not for implementing. Types marked
`@ApiStatus.Experimental` (model formats, per-player bones) may still change.
