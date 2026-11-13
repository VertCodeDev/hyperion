# Bedrock Support

Bedrock players who join through [Geyser](https://geysermc.org/) see Hyperion models as native
Bedrock entities. Cubes keep any rotation and size, and animations run on the player's device.

This needs Hyperion's Geyser extension. Without it, Bedrock players see the plain mob a model
rides.

## Setup

The extension goes wherever Geyser runs: standalone, on a Velocity or BungeeCord proxy, or on the
server itself.

1. Build Hyperion (see [Installation](Installation)). The extension is
   `modules/geyser/build/libs/hyperion-geyser-v<version>.jar`.
2. Put it in Geyser's `extensions` folder and start Geyser once. It writes
   `extensions/hyperion/config.json`.
3. For each server running Hyperion, add its link and token:

   ```json
   {
     "servers": [
       { "url": "http://10.0.0.5:8164", "token": "<the text in plugins/Hyperion/bedrock-token.txt>" }
     ]
   }
   ```

4. Restart Geyser.

The port is `bedrock.link-port` in Hyperion's `config.yml`, `8164` by default. Only the machine
Geyser runs on needs to reach it, so do not open it to the internet.

```yaml
bedrock:
  enabled: true
  link-port: 8164
```

## What Bedrock players get

| Feature | Bedrock |
|---|---|
| Models, on entities and on their own | Yes |
| Animations, speed and scale | Yes |
| Riding a model's entity | Yes |
| Head turning with the entity's look | Not yet |
| Name tags on a bone | Not yet |
| Steering from a seat | Not yet |
| Per-bone changes made by plugins | Java only |

## Limits

- **Models are registered when Geyser starts.** A model you add later, or change, shows on
  Bedrock after Geyser restarts.
- **A model brings its first 30 animations.** A Bedrock entity holds at most 32 properties.
- **It needs a recent Geyser**, a build with API 2.11 or newer. Geyser's custom entity api is
  still experimental.

## Floodgate

Floodgate is optional. The extension tells each server which players joined through Geyser, so
Hyperion knows every Bedrock player without it.
