# Hyperion for Geyser

A Geyser extension that shows Hyperion models to Bedrock players as native Bedrock entities. It
runs wherever Geyser runs: standalone, on a Velocity or BungeeCord proxy, or on the server.

On Bedrock a model is one entity with real Bedrock geometry, so cubes keep any rotation and size,
and its animations run on the player's device: the server only says which ones play.

## Setup

1. Put `hyperion-geyser-v<version>.jar` in Geyser's `extensions` folder and start Geyser once. It
   writes `extensions/hyperion/config.json`.
2. For each server running Hyperion, add its Bedrock link and token:

   ```json
   {
     "servers": [
       { "url": "http://10.0.0.5:8164", "token": "<plugins/Hyperion/bedrock-token.txt>" }
     ]
   }
   ```

   The port is `bedrock.link-port` in Hyperion's `config.yml`. Only the machine Geyser runs on
   needs to reach it.
3. Restart Geyser.

## How it works

At startup the extension downloads each server's model list and Bedrock pack, registers every
model as a custom Bedrock entity (`hyperion:<model>`) with one property per animation, and gives
the packs to Geyser, which sends them to Bedrock players.

When the server spawns an entity carrying a model for a Bedrock player, Hyperion puts the model in
the spawn packet's UUID, and the extension tells Geyser to show that model instead. A model on its
own gets a stand-in entity only Bedrock players are sent. The server's event stream then says which
animations play, and the extension switches the matching properties.

The extension tells each server which players joined through Geyser, so Hyperion knows every
Bedrock player even without Floodgate.

## Limits

- Models are registered when Geyser starts: a model added to a server later shows on Bedrock after
  Geyser restarts. Changes to existing models need a restart too.
- A model brings its first 30 animations; a Bedrock entity holds at most 32 properties.
- Per-bone control from plugins (bone transforms, modifiers) reaches Java players only; animations,
  speed and scale reach both.
- Needs a Geyser build with API 2.11 or newer, whose custom entity api is still experimental.
