# Troubleshooting

Start with these three commands. They answer most questions:

| Command | What it tells you |
|---|---|
| `/hy validate` | Every warning and error from loading your models and effects |
| `/hy pack status` | How the pack is hosted, its link, and how players are doing with it |
| `/hy info <model>` | What Hyperion read from one model |

## The plugin does not start

- **PacketEvents is missing.** Hyperion needs it as a separate plugin.
- **The server is Spigot.** Hyperion needs Paper or a fork of it.
- **The first start had no internet.** The server downloads the menu library once. Start it once
  with internet access.

## "1 out of 1 pack(s) failed to download"

The player's client could not fetch the pack. Look at the server console: Hyperion logs a warning
that says why. The fixes are in [Resource Pack](Resource-Pack#failed-to-download). The usual
causes:

- The pack port (`8163` by default) is not open.
- The server is behind a proxy and `pack.self-host.address` is not set.

## Models are invisible, or show as paper or purple and black cubes

The player does not have the pack. Run `/hy pack status` to see how they are doing with it, and
`/hy pack send <player>` to offer it again. If they declined it, they have to accept it; set
`pack.required: true` to make it mandatory.

## My model does not load

Run `/hy validate`. Common causes:

- The file name has characters a key cannot hold. Use lowercase letters, numbers, `_`, `-` and
  `.`.
- The file is not a `.bbmodel`. Exported formats such as `.json` or `.geo.json` are not read.
- A texture is missing from the file. Hyperion reads textures embedded in the `.bbmodel`.

## Part of my model is missing

- **Meshes are not supported.** Use cubes.
- **The bone has a behavior.** A bone named `hitbox...` or `ghost...` is never drawn. See
  [Bone Behaviors](Bone-Behaviors).

## I changed a model and nothing happened

Run `/hy reload`. Players get the new pack by themselves when `pack.resend-on-change` is on.

## The animation does not play

- The name is case sensitive: check `/hy info <model>` for the exact name.
- For automatic idle and walk, the animations must be named `idle` and `walk`, or the names you
  set under `models.auto-animations`. They only play on models on entities.

## Config and message changes do not apply

If the console says a file "could not be written", the server cannot write to
`plugins/Hyperion`. The folder belongs to another user, often because it was created or copied
with `sudo`. Give the user that runs the server ownership of the folder and restart:

```bash
sudo chown -R <user-that-runs-the-server> plugins/Hyperion
```

## Armor shows flat

That is the default. 3D chestplates, leggings and boots need `armor.shader: true` and Minecraft
1.21.2 or newer. Players with Iris or OptiFine always see the flat look. See
[Custom Armor](Custom-Armor#the-3d-shader).

## Bedrock players see a pig

The Geyser extension is not installed or not connected. See [Bedrock Support](Bedrock-Support).
A model added after Geyser started shows on Bedrock after Geyser restarts.

## Models are gone after a restart

That is how Hyperion works: models are only packets and are never saved. A plugin that spawns
them puts them back, see [Developer API](Developer-API).

## Still stuck

[Open an issue](https://github.com/VertCodeDev/hyperion/issues) with your server version, the
output of `/hy validate` and `/hy pack status`, and the console log from startup.
