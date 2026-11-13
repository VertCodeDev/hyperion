# API Models

Finding blueprints, spawning models, changing them and deciding who sees them. Read
[Developer API](Developer-API) first for setup and threading.

## Finding a blueprint

```java
Optional<ModelBlueprint> dragon = Hyperion.models().find(Key.key("hyperion", "bosses/dragon"));
Collection<ModelBlueprint> all = Hyperion.models().all();
```

Models from `plugins/Hyperion/models` are in the `hyperion` namespace, with their path as the
value.

A blueprint tells you what the file holds:

| Method | What it gives |
|---|---|
| `key()` | The blueprint's key |
| `bones()`, `rootBones()`, `bone(name)` | Its bones |
| `animations()`, `animation(name)` | Its animation names, and one animation's length and loop mode |
| `locators()`, `locator(name)` | Its locators |
| `boundingBox()` | The box around the whole model |
| `metadata()` | Extra fields from the Blockbench file |
| `owner()` | The plugin that registered it, or null for Hyperion's own folder |

## Spawning

```java
// On an entity: the model follows it.
Model boss = dragon.attach(zombie);

// On its own, at a location.
Model statue = dragon.spawn(location);

// With options.
Model big = dragon.attach(zombie, options -> options
        .scale(2.5f)
        .viewDistance(64)
        .animation("spawn")
        .glowing(true));
```

Call these on the thread that owns the entity or location.

### Spawn options

| Option | What it sets |
|---|---|
| `scale(float)` | The model's size |
| `scaler(ModelScaler)` | A size worked out every frame, see [Scale and rotation](#scale-and-rotation) |
| `rotator(ModelRotator)` | How the model turns |
| `transform(BoneTransform)` | An offset, turn or stretch of the whole model |
| `viewDistance(int)` | How far away players see it, in blocks |
| `viewers(ViewerPolicy)` | Who may see it |
| `interpolation(int)` | Over how many ticks clients smooth each move |
| `animation(String)` | An animation to start with |
| `autoAnimations(boolean)` | Whether idle and walk play by themselves |
| `hitboxMode(HitboxMode)` | `BONES`, `MODEL` or `NONE` |
| `hideBaseEntity(boolean)` | Whether the entity under the model is hidden |
| `hiddenParts(Set<EntityPart>)` | Exactly which parts of the entity are hidden: `BODY`, `EQUIPMENT`, `NAME`, `FIRE`, `GLOW` |
| `tint`, `glowing`, `glowColor`, `glint`, `brightness`, `billboard` | The look of every bone |

Anything you leave out comes from `config.yml`.

## Changing a live model

Every setter is safe from any thread and takes effect on the next frame. Setters return the model,
so they chain.

```java
boss.scale(3f).interpolation(3);
boss.hideBaseEntity(false);
boss.autoAnimations(false);

boss.teleport(location);   // a model on its own; call on the thread that owns it
boss.remove();
```

| Method | What it gives |
|---|---|
| `uniqueId()` | The model's id, for `Hyperion.tracker().byId` |
| `blueprint()` | Its blueprint |
| `anchor()` | An `EntityAnchor` or a `LocationAnchor` |
| `location()` | Where it is now |
| `bones()`, `bone(name)` | Its live bones, see [API Bones](API-Bones) |
| `locators()`, `locator(name)` | Its live locators |
| `animator()` | Its animations, see [API Animations](API-Animations) |
| `seats()`, `hitboxes()`, `behaviors(type)` | Its behaviors, see [API Behaviors](API-Behaviors) |
| `viewers()`, `view(player)` | Who sees it, and one player's own view |
| `isRemoved()` | Whether it is gone |

## Finding live models

```java
List<Model> onZombie = Hyperion.tracker().of(zombie);
Optional<Model> byId = Hyperion.tracker().byId(uuid);
List<Model> nearby = Hyperion.tracker().near(location, 16);
```

An entity can carry more than one model.

## Models are not saved

A model is only packets, so it is gone after a restart, and when its entity unloads. To keep a
model on an entity, mark the entity yourself and put the model back when it loads:

```java
private static final NamespacedKey MODEL = new NamespacedKey("myplugin", "model");

public void give(Entity entity, ModelBlueprint blueprint) {
    entity.getPersistentDataContainer().set(MODEL, PersistentDataType.STRING, blueprint.key().asString());
    blueprint.attach(entity);
}

@EventHandler
public void onLoad(EntitiesLoadEvent event) {
    for (Entity entity : event.getEntities()) {
        String key = entity.getPersistentDataContainer().get(MODEL, PersistentDataType.STRING);
        if (key != null) {
            Hyperion.models().find(Key.key(key)).ifPresent(blueprint -> blueprint.attach(entity));
        }
    }
}
```

`ModelRemoveEvent` tells you why a model left: `REMOVED`, `ENTITY_REMOVED`, `ENTITY_UNLOADED` or
`BLUEPRINT_UNREGISTERED`, among others.

## Who sees a model

```java
Viewers viewers = boss.viewers();

viewers.policy(ViewerPolicy.permission("myplugin.see.boss"));   // or everyone(), only(uuids)
viewers.viewDistance(96);

viewers.show(player);    // sees it whatever the policy says, while in range
viewers.hide(player);    // never sees it, whatever the policy says
viewers.reset(player);   // the policy decides again

Collection<Player> now = viewers.current();
```

### One player's own view

`Model#view(player)` is that player's own layer on top of what everyone sees: its own animations
and its own look for each bone.

```java
PlayerView view = boss.view(player);
view.animator().play("wave");                                   // only this player sees it
view.bone("head").ifPresent(head -> head.tint(Color.YELLOW));   // experimental
view.reset();                                                   // back to what everyone sees
```

## Scale and rotation

A fixed scale is `model.scale(2f)`. A `ModelScaler` works the scale out every frame instead:

```java
boss.scaler(ModelScaler.entityScale());             // follow the entity's scale attribute
boss.scaler(ModelScaler.entityScale().times(1.5f)); // the same, half again as big
```

A `ModelRotator` decides how the model turns:

```java
boss.rotator(ModelRotator.anchor());                // follow the entity (the default)
boss.rotator(ModelRotator.yawOffset(180f));         // turned around
boss.rotator(ModelRotator.fixed(new ModelRotation(0f, 90f)));
```

Both are functional interfaces, so a lambda works too. They run every frame: keep them cheap.

## Your own model files

Register models your plugin ships. They get your namespace and go when your plugin disables.

```java
Hyperion.models()
        .register(this, Key.key("myplugin", "golem"), ModelSource.resource(this, "models/golem.bbmodel"))
        .thenAccept(blueprint -> getLogger().info("Loaded " + blueprint.key().asString()));
```

| Source | Reads from |
|---|---|
| `ModelSource.resource(plugin, path)` | A file inside your jar |
| `ModelSource.file(path)` | A file on disk |
| `ModelSource.bytes(name, data)` | Bytes you already have |

Loading runs off the server thread, so `register` returns a future. It fails when the file cannot
be read or the key belongs to another plugin. A blueprint registered after startup joins the
resource pack the next time the pack is built.

```java
Hyperion.models().reload(key);          // one blueprint
Hyperion.models().unregister(key);      // its live models are removed
```

## Metadata

Fields in a `.bbmodel` that Blockbench itself does not use are kept as metadata on the project,
its groups and its locators. That is how a Blockbench plugin's settings reach your server code.

```java
Metadata metadata = blueprint.metadata();
String rarity = metadata.string("rarity").orElse("common");
double health = metadata.number("health").orElse(20);

blueprint.bone("head").ifPresent(bone -> bone.metadata().bool("weak_spot"));
```
