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

More packages arrive with the features that need them: `pack` for the resource pack, `model` for
live models and rendering, `animation`, `packet` for PacketEvents listeners and `listener` for
Bukkit events.

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

A model argument may leave out `hyperion:`, so `/hy info bosses/dragon` works.

`hyperion.admin` grants every Hyperion permission. Every node defaults to operators.

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

The jar is `build/libs/Hyperion-<version>.jar`. NullAway checks the JSpecify annotations on every
compile, so a null mistake fails the build.

The tests read small hand-written models from `src/test/resources/models`. To check that a folder
of your own models loads, point `HYPERION_MODELS` at it:

```bash
HYPERION_MODELS=~/my-models ./gradlew :paper:test --tests '*RealModelsTest'
```
