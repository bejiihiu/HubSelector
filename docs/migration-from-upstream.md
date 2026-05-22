# Migration from Upstream LobbySelector

## What Changed
- Previous single-class implementation was decomposed into layered modules under `kz.bejiihiu.hub.*`.
- Startup wiring moved into bootstrap (`kz.bejiihiu.hub.bootstrap.Main`).
- CloudNet access now goes through `CloudNetFacade`.
- Inventory rendering and event handling were split into dedicated classes.
- Config access is centralized through typed `SelectorConfig`.

## Compatibility Guarantees
- Core behavior remains the same: open selector GUI, list lobby services, click to connect.
- No config schema changes were introduced in this fork pass.
- Existing command/permission surfaces remain available through the plugin entrypoint contract.

## Namespace Policy
- Fork-specific improvements must be implemented in `kz.bejiihiu.*`.
- Upstream namespace usage (`net.jandie1505.*`) should remain a compatibility shell only.
