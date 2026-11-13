# Installation

## What you need

| | |
|---|---|
| Server | Paper, Folia, Canvas or Purpur, Minecraft 1.21 to 26.3 |
| Java | 21 or newer |
| [PacketEvents](https://github.com/retrooper/packetevents) | Required, as a separate plugin. Hyperion does not start without it. |
| [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) | Optional. Fills its placeholders into effect texts. |
| [Geyser](https://geysermc.org/) | Optional. Shows models to Bedrock players, see [Bedrock Support](Bedrock-Support). |

Spigot is not supported: Hyperion uses Paper APIs.

## Get the jar

There is no release yet, so build it from source. You need Git and a JDK; Gradle downloads the
Java versions it needs when they are missing.

```bash
git clone https://github.com/VertCodeDev/hyperion.git
cd hyperion
./gradlew build
```

The plugin is `modules/paper/build/libs/hyperion-paper-v<version>.jar`.

## Install it

1. Put PacketEvents and the Hyperion jar in the server's `plugins` folder.
2. Start the server. The first start needs internet access: the server downloads the menu
   library once, into its `libraries` folder. Later starts do not need it.
3. Hyperion writes its files into `plugins/Hyperion`:

   | File or folder | What it is |
   |---|---|
   | `config.yml` | Every setting, see [Configuration](Configuration) |
   | `messages.yml` | Every chat message, in MiniMessage |
   | `models/` | Your `.bbmodel` files, see [Models](Models) |
   | `effects/` | Effect files, with `example.yml` to learn from, see [Effects](Effects) |
   | `armor/` | Armor sets, see [Custom Armor](Custom-Armor) |
   | `armor-template.bbmodel` | The Blockbench file to start an armor set from |
   | `bedrock-token.txt` | The token the Geyser extension connects with |

4. Open the pack port. By default Hyperion serves the resource pack itself on port `8163`, and
   players must be able to reach it. Open it in your firewall, or allocate it in your host's
   panel. If you cannot open a port, see the other hosting options in
   [Resource Pack](Resource-Pack).

## Check that it works

1. Drop a `.bbmodel` into `plugins/Hyperion/models/`.
2. Run `/hy reload`.
3. Run `/hy list`, then `/hy spawn <model>`.

If the model does not show, run `/hy validate` and `/hy pack status`, and see
[Troubleshooting](Troubleshooting).

## Updating

Replace the jar and restart. Settings and messages a new version adds are written into your
`config.yml` and `messages.yml` on the next start. The lines you changed stay as they are.
