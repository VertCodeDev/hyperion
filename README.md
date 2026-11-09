<div align="center">

# Hyperion
*Models, animations and custom armor for Minecraft servers, built to stay fast*

[![](https://img.shields.io/github/license/VertCodeDev/hyperion?style=flat-square)](LICENSE)
[![](https://img.shields.io/badge/minecraft-1.21%20to%2026.3-8FCA5C?style=flat-square)](https://www.minecraft.net/en-us/download/server)
[![](https://img.shields.io/badge/java-21%2B-ED8B00?style=flat-square)](https://adoptium.net/)
[![](https://img.shields.io/badge/folia%20%26%20canvas-ready-5865F2?style=flat-square)](https://github.com/VertCodeDev/hyperion#readme)
[![](https://img.shields.io/github/issues/VertCodeDev/hyperion?style=flat-square&logo=github)](https://github.com/VertCodeDev/hyperion/issues)

</div>

* * *

> [!IMPORTANT]
> Hyperion is being planned and built in the open. There is no release yet: everything below is
> what Hyperion is being built to do, and the roadmap further down shows what is done.

# ✨ Introduction

**Hyperion** is an open source, server-side engine for Paper, Folia and Canvas that brings
[Blockbench](https://www.blockbench.net/) models into the game: animated models for bosses, NPCs,
pets, mounts and props, effects that play along with them, and custom armor, with the resource pack
built for you. Players need no mods.

It does three things, and aims to do each of them as fast and as cleanly as a plugin can:

| | |
|---|---|
| 🧊 **Models** | Your Blockbench models in game, on an entity or on their own, shown with display entity packets |
| 🎬 **Animations** | Blockbench animations with particles, sounds and effects on the timeline, played per model or per player |
| 🛡️ **Armor** | Custom armor sets from your own textures and models, worn like vanilla armor |

## 🚀 Why Hyperion

**The most optimised and cleanest library for models, animations and custom armor.** Everything is
designed around that one goal:

- **Nothing on the tick that can be off it.** Model loading, pack building, animation baking and
  packet building run off the server thread. The tick only does what has to happen there.
- **Folia and Canvas first.** Built on region threading from day one, so a model is always driven
  by the thread that owns it, and the same jar runs on plain Paper.
- **Packets only.** Models are display entity packets sent to the players who can see them. No real
  entities, no armor stands, nothing for the server to tick or save.
- **Send changes, not frames.** Bones that did not move are not sent again; models out of view cost
  nothing.
- **Low memory, low allocation.** Animations are baked once into compact keyframe data and shared
  by every model that plays them.
- **A small, clean API.** Few concepts, clear names, documented with real Javadoc, stable between
  releases.
- **Free and open.** MIT licensed: use it on any server, change it, ship it.

# 📦 Features

### 🧊 Models

- Blockbench models (`.bbmodel`): cubes, groups as bones, pivots and rotations, multiple textures.
- Models on an entity (following its movement, head and body rotation) or standing on their own.
- Automatic resource pack generation, no core shaders needed.
- Scaling, tinting, glow and per-bone visibility at runtime.
- Hitboxes and interaction for models that should be clicked or hit.
- Per-player visibility: show a model to some players only.
- **Bedrock support**: Bedrock players (through Geyser) see the models too. Better Bedrock support
  than existing engines is one of Hyperion's goals.

### 🎬 Animations

- Blockbench keyframe animations: rotation, position and scale, with linear, smooth and step
  interpolation.
- Loop, play once and hold on the last frame; blending between animations; speed control.
- **Effects on the timeline**: particles, sounds and custom events at a keyframe, played at a bone,
  so a sword swing can leave a trail and a stomp can shake the ground.
- Per-player animation: the same model can play something different for each viewer.
- Molang expressions in keyframes.

### 🛡️ Armor

- Custom armor sets from your own textures, worn in the vanilla armor slots.
- Full 3D armor pieces from Blockbench models.
- Works with the trims, enchant glint and dyes players already know.
- Bound to items through data components, no custom model data juggling.

### 🧩 For developers

- A Java API to spawn, animate and control models, listen to animation events and register armor.
- Every call safe to make from the thread that owns the entity or location, on Paper and on Folia.
- Events for model spawn, removal, animation start and end, and timeline events.

# 🗺️ Roadmap

- [x] Repository, build and license
- [ ] Planning and API design
- [x] Plugin bootstrap and the `/hyperion` command
- [x] Blockbench model loading
- [x] Resource pack generation
- [x] Display entity rendering and tracking
- [x] Animations
- [ ] Timeline effects (particles, sounds, events)
- [ ] Custom armor
- [ ] Bedrock support
- [ ] Public API release

# 🖥️ Platforms

| Platform | Supported |
|---|---|
| [Paper](https://papermc.io/downloads/paper) | ✅ |
| [Folia](https://papermc.io/downloads/folia) | ✅ |
| [Canvas](https://canvasmc.io/) | ✅ |
| [Purpur](https://purpurmc.org/) | ✅ (Paper fork) |
| Spigot | ❌ Hyperion uses Paper APIs |

# 🛠️ Building

[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)

Building needs Java 21 and Java 25: Hyperion compiles for Java 21 so it runs on 1.21 servers, and
checks itself a second time against the 26.3 api, which needs Java 25. The Gradle toolchain
downloads both when they are missing.

```
./gradlew build
```

The plugin jar is `modules/paper/build/libs/hyperion-paper-v<version>.jar`.

| Module | What it is |
|---|---|
| `modules/api` | The API other plugins compile against |
| `modules/paper` | The plugin, for Paper, Folia and Canvas |
| `modules/menu` | What a menu shows, drawn by `menu-invui1` (1.21.x) or `menu-invui2` (26.x) |

# 💻 API

The API is not published yet. Once it is, adding Hyperion to a plugin will look like this:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("dev.vertcode.hyperion:hyperion-api:VERSION")
}
```

# 🤝 Contributing

Issues and pull requests are welcome. For anything bigger than a small fix, open an issue first so
we can talk about the approach before you spend time on it.

# 🙏 Credits

Hyperion is inspired by the work of [BetterModel](https://github.com/toxicity188/BetterModel) and
ModelEngine, and builds on [Blockbench](https://www.blockbench.net/) and
[PaperMC](https://papermc.io/).

# 📄 License

Hyperion is released under the [MIT License](LICENSE).
