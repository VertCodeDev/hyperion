# hyperion-api

The api other plugins compile against to spawn, change and listen to Hyperion models. It holds
interfaces and small data types only; the Hyperion plugin implements them and ships this api
inside its jar.

## Using it

Depend on Hyperion in your `plugin.yml` or `paper-plugin.yml`, add the api as `compileOnly`, and
start at `Hyperion`:

```java
ModelBlueprint dragon = Hyperion.models().find(Key.key("bosses", "dragon")).orElseThrow();

Model boss = dragon.attach(zombie, options -> options.scale(2.5f).viewDistance(64));
boss.bone("head").ifPresent(head -> head.tint(Color.RED).glowing(true));
boss.animator().play("roar");
```

## Packages

| Package | What it holds |
|---|---|
| `api` | `Hyperion`, the entry point, and `HyperionApi` behind it |
| `api.model` | `ModelRegistry`, `ModelBlueprint`, the live `Model`, spawn options and the tracker |
| `api.model.bone` | Bones as loaded and as live parts of a model, and `BoneTransform` |
| `api.model.locator` | Locators: named points where effects attach |
| `api.model.behavior` | Bone behaviors and how to register your own |
| `api.model.behavior.builtin` | The head, seat, name tag, hand and ghost behaviors Hyperion ships, and the keys and controllers seats steer with |
| `api.effect` | Effects: particles and sounds on a timeline, their anchors, viewers and your own part types |
| `api.model.hitbox` | Bones players can hit and click, and the hitbox mode of a model |
| `api.model.view` | Who sees a model, and per-player overrides |
| `api.model.source` | Where models come from and the formats that read them |
| `api.model.metadata` | Custom data kept from Blockbench files |
| `api.animation` | Playing, layering, fading and stopping animations, for everyone or one player |
| `api.event` | Spawn, removal, interaction, viewer and blueprint load events |
| `api.version` | `MinecraftVersion` and `Feature`: what the running server has, for code that runs on 1.21 to 26.3 |

## Threading

Spawning, attaching, teleporting and removing run on the region that owns the location or
entity, on Paper and on Folia or Canvas alike. Getters and every appearance setter are safe from
any thread and take effect on the model's next frame.

## Null safety

Every package is `@NullMarked` with [JSpecify](https://jspecify.dev): nothing is null unless it is
marked `@Nullable`. IntelliJ and Kotlin read these annotations, so Kotlin sees `Color?` where the
api says `@Nullable Color`.

## Building

`./gradlew :api:build` from the repository root.
