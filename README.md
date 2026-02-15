# SimpleChairs

Simple Minecraft plugin for Paper that lets players sit on stairs (and other configurable blocks) by right-clicking.

## Features

- All configuration is read from `config.yml`
- Configurable prefix for plugin messages (`prefix`) at the top of the config
- **Right-click the top** of allowed blocks to sit (block face UP only; empty hand if `sit-empty-hand-only` is true; cannot be sneaking)
- **Sneak (Shift)** to stand up when `sneak-to-stand` is true
- **Configurable block list** (`sit-blocks`): stairs, slabs, carpets. Entries starting with `#` are disabled
- **One player per chair** (`one-block-per-chair`): prevent two players on the same block
- **Safe position** (`allow-unsafe-sit: false`): no sitting in lava, fire or suffocating spots; **material-blacklist** to disable specific materials (e.g. LAVA, FIRE)
- **Bottom part only** (`sit-bottom-part-only`): only bottom half of stairs and bottom slabs
- **Max distance** (`sit-max-distance`): blocks from which to sit (0 = unlimited)
- **/sit** – sit on the ground (requires `schairs.sit.command`). When you stand up, you are placed on top of the block (no clipping).
- **/crawl** – crawl on the ground; sneak to stop (optional feature, requires `schairs.crawl`). Stops in water or flying.
- **Optional features** can be turned off in config (`features.crawl-enabled`). When disabled, the command is disabled too.
- **Full 360° rotation** while sitting: the seat follows your look direction on slabs, stairs and any block (no abrupt snaps).
- **stand-when-damaged**: stand up after taking damage (sit, crawl)
- **stand-when-block-breaks**: stand when the block beneath is broken
- **return-on-stand**: teleport back to position before sitting when standing up
- **World filter**: restrict sit/crawl to listed worlds when enabled
- **WorldGuard** (optional): if installed and `worldguard-integration` is true, sitting is blocked in regions that have the **chairs-deny** flag set
- **Ghost mount cleanup**: on server start and on player join, any leftover chair armor stands (e.g. after a crash) are detected and removed
- **Two message types**: plugin messages (chat, with prefix) and player messages (action bar, no prefix)
- **reload** command to reload configuration without restarting the server
- Minecraft color codes supported using `&`

## Commands & Permissions

| Command | Aliases | Description |
|---------|---------|-------------|
| `/simplechairs reload` | `/chairs reload` | Reloads the plugin configuration |
| `/sit` | — | Sit on the ground at your current position |
| `/crawl` | — | Crawl on the ground (sneak to stop) |

- **Sitting on blocks** (right-click): **`schairs.sit`** (default: true) – only permission enabled by default for everyone
- **/sit**: **`schairs.sit.command`** (default: op)
- **/crawl**: **`schairs.crawl`** (default: op) – only if `features.crawl-enabled` is true
- **reload**: **`schairs.reload`** (default: op)
- OPs have permission to run /sit and /crawl by default; other players need the permission granted explicitly.
- Commands work in-game; reload works from console too

## Requirements

- Java 21 (LTS)
- Paper server (tested with `api-version: "1.21"`, build 111)
- Gradle installed on your system (this project only includes `build.gradle`, not the wrapper)
- Optional: **WorldGuard** for region-based sitting deny (flag **chairs-deny**)

## Build

From the project root (`simple-chairs`), run:

```bash
gradle build
```

The plugin JAR will be generated at:

`build/libs/SimpleChairs-1.0.0.jar`

## Installation

1. Copy the built JAR to your Paper server `plugins` folder
2. Start or restart the server
3. The `config.yml` file will be created automatically in `plugins/SimpleChairs/` if it does not exist

## Configuration

Example configuration (defaults):

```yaml
prefix: "&7[&6Chairs&7] &r"
stand-when-damaged: false
sneak-to-stand: true
return-on-stand: false
stand-when-block-breaks: true
allow-unsafe-sit: false
one-block-per-chair: true
sit-center-on-block: true
sit-bottom-part-only: true
sit-empty-hand-only: true
sit-max-distance: 0
sit-max-block-height: 2
features:
  crawl-enabled: true
crawl-sneak-to-stand: true
crawl-double-sneak: false
worldguard-integration: true
world-filter-enabled: false
worlds:
  - "world"
material-blacklist:
  - "LAVA"
  - "FIRE"
persist-sit-hint: false
persist-crawl-hint: false

sit-blocks:
  - "stairs"
  - "slabs"
  - "#carpets"

plugin-messages:
  no-permission: "&cYou do not have permission"
  world-not-allowed: "&cYou cannot use this in this world"
  player-only: "&cThis command can only be executed by a player"
  reload-success: "&aConfiguration reloaded successfully"
  reload-no-permission: "&cYou do not have permission to reload the configuration"
  usage: "&7Usage: &f/<command> [reload]"
  feature-disabled: "&cThis feature is disabled"

player-messages:
  sit-hint: "&6Sneak&f to get up"
  already-sitting: "&cYou're already sitting"
  chair-occupied: "&cSomeone is already sitting here"
```

- **stand-when-damaged**, **sneak-to-stand**, **return-on-stand**, **stand-when-block-breaks**: Stand up on damage, on sneak, return to previous position, or when block breaks (defaults: false, true, false, true)
- **allow-unsafe-sit**: Allow sitting in unsafe locations (default: false)
- **one-block-per-chair**, **sit-center-on-block**, **sit-bottom-part-only**, **sit-empty-hand-only**: One per block; center on block; bottom part only; empty hand to sit (default: true)
- **sit-max-distance**, **sit-max-block-height**: Max distance (0 = unlimited) and max height to sit (default: 0, 2)
- **features.crawl-enabled**: Enable/disable /crawl (default: true). When false, the command is disabled.
- **crawl-sneak-to-stand**, **crawl-double-sneak**: Sneak to stop crawl; double-sneak (looking down) to start crawl (default: true, false)
- **worldguard-integration**, **world-filter-enabled**, **worlds**, **material-blacklist**: WorldGuard, world filter, material blacklist
- **sit-blocks**: **stairs**, **slabs**, **carpets**. Entries starting with `#` are disabled
- **plugin-messages**, **player-messages**: Chat (with prefix) and action bar (no prefix). **feature-disabled** when a feature is off.

Sitting position: on **stairs** the character sits on the low part of the step; on **slabs** the character sits flush on top of the slab.

## Notes

- Sitting uses an invisible armor stand marked with PDC metadata; the stand is removed when the player dismounts or when the plugin is disabled
- WorldGuard is optional (soft dependency). If present and `worldguard-integration` is true, the plugin registers the **chairs-deny** flag and blocks sitting in regions where it is set to *allow*
- On enable and on player join, any armor stands marked as SimpleChairs (e.g. left after a crash) are removed so players do not stay mounted on “ghost” chairs
- Compatible with Paper 1.21.1 (api-version "1.21")
