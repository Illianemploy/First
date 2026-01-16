# 3D FPS Shooter Game

A complete first-person shooter game built with Python and the Ursina engine. Features enemies, shooting mechanics, health system, and continuous gameplay.

## Features

- **First-Person Controls**: WASD movement with smooth mouse-look
- **Shooting Mechanics**: Left-click to fire with recoil animation and muzzle flash
- **Enemy System**: 10 enemies that respawn after being destroyed
- **Health System**: Take damage when enemies get close, screen flash effect
- **HUD**: Real-time health bar, ammo counter, and score display
- **Sound Effects**: Built-in audio for shooting, hits, and damage
- **Game Over Screen**: Shows final score with option to restart
- **Dynamic Environment**: Random obstacles/walls for cover and tactical gameplay
- **Auto-Reload**: Weapon automatically reloads when ammo runs out

## Installation

### Prerequisites

- Python 3.7 or higher
- pip (Python package manager)

### Install Ursina Engine

```bash
pip install ursina
```

Or if you're using Python 3 specifically:

```bash
pip3 install ursina
```

### Additional Dependencies

Ursina will automatically install its dependencies:
- panda3d (3D engine)
- screeninfo
- psutil

## How to Run

1. Navigate to the game directory:
```bash
cd /path/to/First
```

2. Run the game:
```bash
python fps_game.py
```

Or:
```bash
python3 fps_game.py
```

3. The game window will open automatically

## Controls

- **W/A/S/D**: Move forward/left/backward/right
- **Mouse**: Look around
- **Left Click**: Shoot
- **ESC**: Exit game
- **ENTER**: Restart game (when game over)

## Gameplay

### Objective
Survive as long as possible and rack up points by eliminating enemies!

### Mechanics

1. **Movement**: Use WASD keys to navigate the arena. Move strategically and use obstacles as cover.

2. **Shooting**:
   - Aim with your mouse and left-click to fire
   - Each enemy requires one shot to eliminate
   - You have 30 rounds per magazine
   - Auto-reload activates when ammo runs out (2-second delay)

3. **Enemies**:
   - Red cube-shaped enemies with white heads
   - 10 enemies spawn around the map
   - Enemies gently bob up and down to be more visible
   - When killed, enemies respawn after 3 seconds at a new location
   - Getting too close to enemies (within 3 units) causes damage

4. **Health**:
   - Start with 100 health
   - Take 10 damage per enemy hit (1-second cooldown)
   - Health bar changes color: Green > Yellow > Red
   - Screen flashes red when damaged
   - Game over when health reaches 0

5. **Scoring**:
   - Earn 100 points per enemy eliminated
   - Try to beat your high score!

## Game Configuration

You can easily modify game parameters by editing the `GAME_CONFIG` dictionary at the top of `fps_game.py`:

```python
GAME_CONFIG = {
    'max_health': 100,          # Starting health
    'max_ammo': 30,             # Starting ammo
    'enemy_damage': 10,         # Damage per enemy hit
    'enemy_health': 50,         # Enemy health (one-shot kill default)
    'enemy_count': 10,          # Number of enemies
    'enemy_spawn_radius': 40,   # How far enemies spawn
    'shoot_range': 100,         # How far bullets travel
    'score_per_kill': 100       # Points per kill
}
```

## Code Structure

The game is organized into clear, modular classes:

- **Enemy**: Handles enemy behavior, health, death, and respawning
- **Gun**: Manages weapon model, shooting animation, and effects
- **HUD**: Displays health bar, ammo count, score, and crosshair
- **GameOverScreen**: Shows game over state and restart option
- **Game**: Main controller managing game state, logic, and flow

## Customization Ideas

Want to modify the game? Here are some easy tweaks:

1. **Difficulty**: Increase `enemy_damage` or `enemy_count`
2. **Weapon**: Adjust `max_ammo` or `shoot_range`
3. **Enemy Behavior**: Modify respawn delay in `Enemy.die()` method
4. **Colors**: Change enemy colors in the `Enemy.__init__()` method
5. **Map Size**: Adjust ground scale and `enemy_spawn_radius`

## Troubleshooting

### Game won't start
- Ensure Ursina is installed: `pip install ursina`
- Check Python version: `python --version` (needs 3.7+)

### Performance issues
- Close other applications
- Reduce `enemy_count` in GAME_CONFIG
- Disable FPS counter: Set `window.fps_counter.enabled = False`

### Mouse not working
- Click on the game window to capture mouse
- Press ESC to release mouse and exit

## Technical Details

- **Engine**: Ursina (built on Panda3D)
- **Language**: Python 3
- **Graphics**: Procedural shapes (no external assets)
- **Audio**: Built-in Ursina audio system
- **Physics**: Basic collision detection with raycasting

## License

This game is provided as-is for educational and entertainment purposes.

## Credits

Built using the Ursina game engine (https://www.ursinaengine.org/)

---

Enjoy the game and happy shooting! 🎮
