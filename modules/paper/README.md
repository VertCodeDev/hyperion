# Hyperion plugin

The plugin server owners install: it loads Blockbench models, builds the resource pack and shows
models to players. One jar runs on Paper, Folia and Canvas, from Minecraft 1.21 to 26.3.

The jar carries the [api](../api/README.md) and one library, renamed under
`dev.vertcode.hyperion.libs` so it never clashes with another plugin's copy. The server installs
one more plugin next to it:

| Library | How it ships | What Hyperion uses it for |
|---|---|---|
| [Incendo Cloud](https://github.com/Incendo/cloud) | Inside the jar | The `/hyperion` commands, registered through Paper's Brigadier api |
| [PacketEvents](https://github.com/retrooper/packetevents) | Separate plugin, required | Sending model packets, and editing the server's own packets to hide parts of an entity or add seats |

PacketEvents stays a separate plugin so a new Minecraft version only needs a PacketEvents update,
not a new Hyperion release. Hyperion does not start without it.

## Packages

| Package | Holds |
|---|---|
| `dev.vertcode.hyperion` | `HyperionPlugin`: startup and shutdown only |
| `blueprint` | `BlueprintRegistry` (loading and reloading), `Blueprint` and its bones and locators, load reports |
| `blueprint.bbmodel` | `BbmodelFormat` and `BbmodelReader`: `.bbmodel` JSON into plain records |
| `blueprint.data` | The plain records a file is read into: cubes, groups, textures, locators, animations |
| `blueprint.metadata` | `JsonMetadata`: the extra fields Blockbench plugins store, read only |
| `command` | One class per command, `HyperionCommands` to register them, `HyperionPermissions` |
| `config` | `HyperionConfig` and `PackSettings` from `config.yml`; `HyperionMessages`, one constant per line of `messages.yml`, and the `MessageBook` they render from |
| `pack` | `PackBuilder` turns blueprints into the resource pack; `PackManager` builds, hosts and sends it |
| `listener` | `PackListener`: offers the pack on join and tracks how players do with it |

More packages arrive with the features that need them: `model` for live models and rendering,
`animation`, and `packet` for PacketEvents listeners.

## Models

Put `.bbmodel` files in `plugins/Hyperion/models/`, in subfolders if you like. The path becomes
the model's key: `models/bosses/dragon.bbmodel` loads as `hyperion:bosses/dragon`. Capitals and
spaces are turned into lowercase and `_` with a warning; any other character a key cannot hold
stops that file from loading.

Models load on the async scheduler when the plugin starts and on `/hy reload`. A file with
problems never stops the others: what Hyperion cannot use (meshes, missing textures, unknown
keyframe channels) is a warning in `/hy validate`, and only a file that cannot be read at all
fails. A failed reload keeps the model that was loaded before.

Both group layouts Blockbench has saved are read: groups inside the outliner (before 5.0) and
the top-level `groups` list (5.0 and later). Fields Blockbench itself does not use are kept as
metadata on the project, groups and locators, which is how a Blockbench plugin's settings reach
the server.

## Commands

The root is `/hyperion`, with the aliases `/hyp` and `/hy`.

| Command | Permission | What it does |
|---|---|---|
| `/hy` | none | Staff get the help menu; everyone else sees which plugin draws the models |
| `/hy help [query]` | `hyperion.command.help` | The paged help menu, filtered by a search |
| `/hy list [filter]` | `hyperion.command.list` | Every loaded model with its counts, each a click away from its info |
| `/hy info <model>` | `hyperion.command.info` | Source, size, bone tree, animations, locators and warnings |
| `/hy reload [model]` | `hyperion.command.reload` | Load every model again, or one, and show what changed |
| `/hy validate [model]` | `hyperion.command.validate` | The warnings and errors from the last load |

| `/hy pack build` | `hyperion.command.pack.build` | Build the resource pack again |
| `/hy pack send [players]` | `hyperion.command.pack.send` | Offer the pack again, to some players or everyone |
| `/hy pack status` | `hyperion.command.pack.status` | Hosting, link, size and hash, and how players are doing with it |
| `/hy pack export` | `hyperion.command.pack.export` | Write the pack to `plugins/Hyperion/hyperion-pack.zip` |

A model argument may leave out `hyperion:`, so `/hy info bosses/dragon` works. `/hy reload` also
reads `config.yml` and `messages.yml` again and rebuilds the pack.

`hyperion.admin` grants every Hyperion permission. Every node defaults to operators.

## Resource pack

Every loaded model is built into one resource pack, for the version the server runs, after
startup and after every `/hy reload`. The pack format comes from the server jar itself, so a new
Minecraft version needs no update for it. The same models give the same zip and hash, so players
who already have the pack do not download it again.

Each bone becomes one item model centered on its pivot. Item models only hold cubes within -16
to 32 pixels, turned on one axis by 22.5 or 45 degrees, so a bone that is too big is shrunk and
scaled back up by its display, and a cube turned any other way becomes a part of its own that the
display turns. Those are 1.21's limits, so a model renders the same on every version. From 1.21.4
each part is an item definition; on 1.21 to 1.21.3 it is a custom model data number on the base
item.

`config.yml` decides how players get the pack:

| `pack.hosting` | What happens |
|---|---|
| `self-host` (default) | A small web server built into Hyperion serves it on `pack.self-host.port`. Players download from the address they joined with, unless `pack.self-host.address` says otherwise. |
| `url` | You upload the zip from `/hy pack export` and put its link in `pack.url`. Upload it again after every change. |
| `off` | Nothing is sent; merge the exported zip into your own pack. |

`pack.required` kicks players who decline or fail the download, `pack.send-on-join` and
`pack.resend-on-change` decide when it is offered, and `pack.base-item` (default `paper`) is the
item parts are shown as. On 1.21 to 1.21.3 only a dyeable base item such as
`leather_horse_armor` can be tinted.

## Messages

Every chat message lives in `plugins/Hyperion/messages.yml`, written in
[MiniMessage](https://docs.advntr.dev/minimessage/format.html). Cloud's own error texts and the
help menu come from the same file. The `palette` section sets the colors, which every message
uses as tags (`<primary>`, `<muted>`, `<error>`...), and `<prefix>` puts the prefix in front.
Keys a newer version adds are written into the server's file on the next start; lines the server
changed are left alone. `/hy reload` reads the file again.

In code, each line is a constant whose name spells its key (`RELOAD__MODEL_DONE` is
`reload.model-done`), and values are passed by name:

```java
HyperionMessages.RELOAD__DONE.send(sender, value("loaded", 40), value("time", 312));
```

The one text not in the file is the about card `/hy` shows players without the help permission:
it is how players learn which plugin draws the models.

## Versions

One jar supports every version from 1.21 to 26.3. To keep it that way:

- The code is compiled for Java 21, the oldest Java those servers run on.
- It is compiled against the Paper 1.21 api, so a method only newer servers have cannot be used
  by accident.
- `./gradlew build` compiles it a second time against the 26.3 api, so a method newer servers
  removed fails the build too.
- A difference between versions is a `Feature` in the api, named once with the version it arrived
  in. Code asks `Feature.ITEM_MODEL_DEFINITIONS.available()` rather than comparing version numbers,
  and never names a Paper constant some versions lack: the scale attribute, for one, is looked up
  by the key `Feature.PLAIN_ATTRIBUTE_KEYS` says the server uses.
- A Paper method only newer servers have will go in a small module per version range, compiled
  against that version's api and loaded only on servers that have it.

## Threading

Cloud runs a command on the thread that sent it: a player's command on that player's region, the
console's on the global region. A command that changes something owned elsewhere hops to its
owner first.

## Building

```bash
./gradlew build
```

The jar is `build/libs/hyperion-paper-v<version>.jar`. NullAway checks the JSpecify annotations on every
compile, so a null mistake fails the build.

The tests read small hand-written models from `src/test/resources/models`. To check that a folder
of your own models loads, point `HYPERION_MODELS` at it:

```bash
HYPERION_MODELS=~/my-models ./gradlew :paper:test --tests '*RealModelsTest'
```
