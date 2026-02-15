<p align="center"><img src="https://i.ibb.co/j96hVLsS/Simple-Chairs.png"/></p>

Simple Minecraft plugin for Paper that lets players sit on stairs (and other configurable blocks) by right-clicking.

## Features

- All configuration is read from `config.yml`
- Configurable prefix for plugin messages (`prefix`)
- **Right-click the top** of allowed blocks to sit
- **/sit** – sit on the ground
- **/crawl** – crawl on the ground (optional feature)
- **WorldGuard** (optional): Sitting is blocked in regions that have the **chairs-deny** flag set
- Minecraft color codes supported using `&`

## Commands & Permissions

| Command | Aliases | Description |
|---------|---------|-------------|
| `/simplechairs reload` | `/chairs reload` | Reloads the plugin configuration |
| `/sit` | — | Sit on the ground at your current position |
| `/crawl` | — | Crawl on the ground |

- **Sitting on blocks** (right-click): **`schairs.sit`** (default: true)
- **/sit**: **`schairs.sit.command`** (default: false)
- **/crawl**: **`schairs.crawl`** (default: fasle)
- **reload**: **`schairs.reload`** (default: OP)

## Requirements

- Java 21 (LTS)
- Paper server (tested with `api-version: "1.21"`)
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
