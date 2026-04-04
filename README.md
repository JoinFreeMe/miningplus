# Mining+

Vein mining, tunnel boring, and excavation patterns for Hytale.

Break one ore - mine the entire vein. Dig tunnels with a single swing. Mining+ gives you powerful mining tools with a clean in-game config UI.

## Features

- **Vein Mining** - Mine one ore and all connected ores of the same type break automatically. Uses 26-directional adjacency so diagonal ores are included.
- **Tunnel Patterns** - Dig 1x1, 2x1 (player height), or 3x3 tunnels in the direction you're facing.
- **Area Patterns** - Break 3x3x3 or 5x5x5 cubes into the wall from the face you mine.
- **In-Game Config UI** - Open with `/mining+`. No items needed.
- **Activation Keys** - Always active, or require Crouch (Ctrl) / Walk (Alt) to be held.
- **Per-Player Settings** - Each player's preferences are saved to disk and persist across restarts.
- **Server Limits** - Admins set max blocks and search depth in `config.json`. Player values are capped at the server maximum.
- **Proper Drops** - All mined blocks drop their correct items using the game's drop tables.

## Installation

1. Download the latest release JAR from [CurseForge](https://www.curseforge.com/hytale/mods/miningplus) or the [Releases](../../releases) page.
2. Place the JAR in your server's `Server/mods/` directory.
3. Start the server. A `MiningPlus/` folder will be created in `mods/` with the default config.

## Commands

| Command | Description |
|---------|-------------|
| `/mining+` | Open the config UI |

## Configuration

### Server Config (`mods/MiningPlus/config.json`)

```json
{
  "enabled": true,
  "maxBlocksPerAction": 64,
  "maxSearchDepth": 32
}
```

These values are the server-wide maximums. Players can set their own values up to these limits.

### Player Config (`mods/MiningPlus/players/{uuid}.json`)

Created automatically when a player first opens the config UI. Stores their personal preferences:

```json
{
  "enabled": true,
  "activationMode": "walk",
  "pattern": "ORES_ONLY",
  "maxBlocksPerAction": 64,
  "maxSearchDepth": 32
}
```

## Excavation Patterns

| Pattern | Description |
|---------|-------------|
| Ores Only | Flood-fill connected ores of the same type |
| Tunnel 1x1 | Single-block tunnel |
| Tunnel 2x1 | Player-height tunnel (2 high, 1 wide) |
| Tunnel 3x3 | Full 3x3 tunnel, centered on mined block |
| 3x3x3 Cube | 3x3 area, 3 blocks deep |
| 5x5x5 Cube | 5x5 area, 5 blocks deep |

All patterns use block face detection - the tunnel extends in the direction away from where you're standing.

## Building from Source

Requires Java 25 and access to the Hytale Maven repository.

```bash
./gradlew build
```

The built JAR will be at `build/libs/MiningPlus-1.0.0.jar`.

## License

[MIT](LICENSE)

## Links

- [CurseForge](https://www.curseforge.com/hytale/mods/miningplus)
- [Discord](https://discord.com/invite/aCE6HqfCHj)
