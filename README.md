# HubSelector (Fork of LobbySelector)

HubSelector is an improved fork of the original `net.jandie1505` LobbySelector plugin.  
It displays CloudNet lobby services in a clickable inventory menu and supports silent lobby routing.

## Fork Notice
- This repository is a fork and keeps compatibility entrypoints where needed.
- Fork-specific architecture and extensions live under `kz.bejiihiu.hub.*`.
- The plugin keeps the same core user-facing behavior: open GUI, pick service, connect through CloudNet Bridge.

## What's Improved
- Monolith split into focused modules (bootstrap/config/cloud/inventory/command/listener/model).
- Typed config boundary (`SelectorConfig`) instead of runtime-wide raw JSON access.
- Explicit CloudNet facade with null-safe dependency access and connection flow.
- Better maintainability for future fork-only features.

## Requirements
- Java 21
- Paper API `1.21.4-R0.1-SNAPSHOT` (provided scope in `pom.xml`)
- CloudNet v4 modules:
  - `bridge-api` `4.0.0-RC12`
  - `wrapper-jvm-api` `4.0.0-RC12`
  - `platform-inject-api` `4.0.0-RC12`

## Installation
1. Build the jar from this fork.
2. Place it in your lobby template `plugins/` directory.
3. Start one lobby instance to generate `config.json`.
4. Tune config values.
5. Roll out to all lobby instances.

## Commands and Permissions
| Item | Value |
|--|--|
| Command | `/lobbyselector` |
| Permission (use) | `lobbyselector.use` |
| Permission (silent lobbies) | `lobbyselector.silentlobby` |

## Configuration
Top-level keys:

| Key | Description |
|--|--|
| `lobbyTask` | Main lobby task name. |
| `inventoryTitle` | Inventory GUI title (legacy color codes supported). |
| `hideFullServices` | Hides non-current services when player count is at max. |
| `enableSilentLobby` | Enables integration of services from `silentLobbyTask`. |
| `silentLobbyTask` | Task name for silent lobbies. |
| `serverItems` | Item templates for `default`, `full`, `silentHub`, `current`. |

`serverItems.<variant>` fields:

| Key | Description |
|--|--|
| `material` | Bukkit material name. |
| `name` | Display name with placeholders. |
| `lore` | String array (one entry per line). |
| `enchanted` | Adds enchantment glint when `true`. |
| `customModelData` | Non-negative value enables custom model data. |

Supported placeholders:

| Placeholder | Description |
|--|--|
| `{service}` | Service name. |
| `{players}` | Current players. |
| `{max_players}` | Maximum players. |

## Architecture Overview
Internal modules are split by responsibility:
- `bootstrap`: wiring, registration, startup lifecycle.
- `config`: defaults + load + typed projection.
- `cloud`: CloudNet lookup and connect abstraction.
- `inventory`: GUI build, item rendering, click/drag handling.
- `command`: permission checks and inventory open action.
- `listener`: event glue around inventory interactions.
- `model`: compact records shared between layers.

See detailed docs in:
- `docs/architecture.md`
- `docs/migration-from-upstream.md`

## Known Limitation
If players are not visible to CloudNet player management, clicking a service item may not connect the player.  
This plugin uses CloudNet Bridge APIs for transfer, so upstream CloudNet visibility issues directly affect routing.
