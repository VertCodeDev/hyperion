# Hyperion

Hyperion brings [Blockbench](https://www.blockbench.net/) models into Minecraft: animated models
for bosses, NPCs, pets, mounts and props, effects that play along with them, and custom armor. It
builds the resource pack for you, and players need no mods.

It is a free, open source plugin for Paper, Folia and Canvas, for Minecraft 1.21 to 26.3, released
under the MIT license.

> [!IMPORTANT]
> There is no release yet. Until there is one, you build the jar yourself: see
> [Installation](Installation).

## Start here

| I want to | Read |
|---|---|
| Install the plugin | [Installation](Installation) |
| Get my first model in game | [Models](Models) |
| Make it move | [Animations](Animations) |
| Make heads turn, add seats and hitboxes | [Bone Behaviors](Bone-Behaviors) |
| Add particles, sounds and camera scenes | [Effects](Effects) |
| Make custom armor | [Custom Armor](Custom-Armor) |
| Fix "failed to download" | [Resource Pack](Resource-Pack) |
| Show models to Bedrock players | [Bedrock Support](Bedrock-Support) |
| Use Hyperion from my own plugin | [Developer API](Developer-API) |

## Reference

- [Commands and Permissions](Commands-and-Permissions)
- [Configuration](Configuration)
- [Staff Tools](Staff-Tools): the gallery, the model menu and the debug views
- [API Events](API-Events)
- [Troubleshooting](Troubleshooting)

## What makes it fast

- **Packets only.** A model is display entity packets sent to the players who can see it. The
  server never ticks, saves or collides with it.
- **Changes, not frames.** A bone that did not move is not sent again. A model nobody sees costs
  almost nothing.
- **Off the tick.** Loading models, building the pack and baking animations run off the server
  thread.
- **Folia and Canvas first.** A model is always driven by the thread that owns it, and the same
  jar runs on plain Paper.

## Platforms

| Platform | Supported |
|---|---|
| [Paper](https://papermc.io/downloads/paper) | Yes |
| [Folia](https://papermc.io/downloads/folia) | Yes |
| [Canvas](https://canvasmc.io/) | Yes |
| [Purpur](https://purpurmc.org/) | Yes, as a Paper fork |
| Spigot | No, Hyperion uses Paper APIs |
