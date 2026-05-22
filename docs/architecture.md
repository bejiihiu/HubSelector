# Architecture

## Module Boundaries
- `bootstrap`: startup composition root. Creates config, services, listeners, and command bindings.
- `config`: default config factory, disk loader, and typed config view (`SelectorConfig`).
- `cloud`: CloudNet integration boundary (`CloudNetFacade`) and task classification (`ServiceFilter`).
- `inventory`: inventory construction, item rendering, placeholder expansion, click/drag behavior.
- `command`: command execution and permission validation.
- `model`: records passed between modules (`PlayerContext`, `ServiceView`, `ItemTemplate`).

## Request Flow
1. Player runs `/lobbyselector`.
2. `LobbySelectorCommand` validates sender and permission.
3. `LobbyInventoryService#buildFor(PlayerContext)` resolves available services from CloudNet.
4. `SelectorItemFactory` renders inventory items from config templates.
5. Player clicks an item in the GUI.
6. `LobbyInventoryListener` reads service id from item metadata and calls `CloudNetFacade#connectPlayerToService`.

## Fallback Behavior
- If `CloudServiceProvider` or `WrapperConfiguration` is unavailable, inventory building falls back to a minimal inventory instead of throwing.
- If `ServiceRegistry`, `PlayerManager`, or `PlayerExecutor` cannot be resolved, connect attempts fail safely (no crash).
- If item material is invalid in config, a barrier fallback item is rendered.
