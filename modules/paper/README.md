# Hyperion plugin

The plugin server owners install: it loads Blockbench models, builds the resource pack and shows
models to players. One jar runs on Paper, Folia and Canvas.

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
| `command` | One class per command, `HyperionCommands` to register them, `HyperionPermissions` |

More packages arrive with the features that need them: `blueprint` for model loading, `pack` for
the resource pack, `model` for live models and rendering, `animation`, `packet` for PacketEvents
listeners and `listener` for Bukkit events.

## Commands

The root is `/hyperion`, with the aliases `/hyp` and `/hy`.

| Command | Permission | What it does |
|---|---|---|
| `/hy` | none | Staff get the help menu; everyone else sees which plugin draws the models |
| `/hy help [query]` | `hyperion.command.help` | The paged help menu, filtered by a search |

`hyperion.admin` grants every Hyperion permission. Every node defaults to operators.

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
