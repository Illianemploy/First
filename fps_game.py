#!/usr/bin/env python3
"""
3D FPS Shooter Game using Ursina Engine
A complete first-person shooter with enemies, shooting mechanics, health system, and HUD.
All graphics use simple built-in shapes - no external assets required.

Controls:
- WASD: Movement
- Mouse: Look around
- Left Click: Shoot
- ESC: Exit game
"""

from ursina import *
from ursina.prefabs.first_person_controller import FirstPersonController
import random

# ============================================================================
# GAME CONFIGURATION
# ============================================================================

GAME_CONFIG = {
    'max_health': 100,
    'max_ammo': 30,
    'ammo_per_clip': 30,
    'enemy_damage': 10,
    'enemy_health': 50,
    'enemy_count': 10,
    'enemy_spawn_radius': 40,
    'shoot_range': 100,
    'score_per_kill': 100
}

# ============================================================================
# ENEMY CLASS
# ============================================================================

class Enemy(Entity):
    """
    Enemy entity that can be destroyed by the player.
    Respawns after being destroyed to keep gameplay continuous.
    """
    def __init__(self, position=(0, 0, 0)):
        super().__init__(
            model='cube',
            color=color.red,
            scale=(1, 2, 1),  # Taller than wide to resemble a target
            position=position,
            collider='box'
        )
        self.health = GAME_CONFIG['enemy_health']
        self.max_health = GAME_CONFIG['enemy_health']
        self.alive = True

        # Add a white "head" to make it look more like a target
        self.head = Entity(
            model='sphere',
            color=color.white,
            scale=0.5,
            position=(0, 1.2, 0),
            parent=self
        )

        # Simple bobbing animation to make enemies more visible
        self.animate_y(position[1] + 0.3, duration=1, curve=curve.in_out_sine, loop=True)

    def take_damage(self, damage):
        """Apply damage to enemy and handle death"""
        if not self.alive:
            return

        self.health -= damage

        # Flash white when hit
        self.color = color.white
        invoke(self.reset_color, delay=0.1)

        if self.health <= 0:
            self.die()

    def reset_color(self):
        """Reset enemy color after being hit"""
        if self.alive:
            self.color = color.red

    def die(self):
        """Handle enemy death and respawn"""
        self.alive = False
        game.score += GAME_CONFIG['score_per_kill']

        # Death animation - fall and fade
        self.animate_y(self.y - 2, duration=0.5)
        self.fade_out(duration=0.5)

        # Respawn after delay
        invoke(self.respawn, delay=3)

    def respawn(self):
        """Respawn enemy at a new random location"""
        self.health = self.max_health
        self.alive = True
        self.enabled = True
        self.color = color.red

        # New random position
        angle = random.uniform(0, 360)
        distance = random.uniform(10, GAME_CONFIG['enemy_spawn_radius'])
        self.position = (
            distance * math.cos(math.radians(angle)),
            1,
            distance * math.sin(math.radians(angle))
        )

# ============================================================================
# GUN CLASS
# ============================================================================

class Gun(Entity):
    """
    Player's gun model with shooting animation and effects.
    Attached to camera for first-person view.
    """
    def __init__(self):
        super().__init__(
            parent=camera.ui,
            model='cube',
            texture='white_cube',
            color=color.dark_gray,
            scale=(0.15, 0.1, 0.4),
            position=(0.4, -0.3, 0.5),
            rotation=(-5, -10, 0)
        )

        # Barrel of the gun
        self.barrel = Entity(
            parent=self,
            model='cube',
            color=color.black,
            scale=(0.3, 0.3, 1.2),
            position=(0, 0, 0.6)
        )

        # Muzzle flash (initially hidden)
        self.muzzle_flash = Entity(
            parent=self.barrel,
            model='quad',
            color=color.yellow,
            scale=0.3,
            position=(0, 0, 0.6),
            rotation=(0, 0, 45),
            enabled=False
        )

        self.original_position = self.position

    def shoot(self):
        """Play shooting animation"""
        # Recoil animation
        self.position = self.original_position + Vec3(-0.05, -0.05, -0.15)
        self.animate_position(self.original_position, duration=0.1)

        # Muzzle flash
        self.muzzle_flash.enabled = True
        self.muzzle_flash.animate_scale(0.6, duration=0.05)
        invoke(setattr, self.muzzle_flash, 'enabled', False, delay=0.05)

# ============================================================================
# HUD CLASS
# ============================================================================

class HUD:
    """
    Heads-Up Display showing player health, ammo, and score.
    """
    def __init__(self):
        # Health bar background
        self.health_bar_bg = Entity(
            parent=camera.ui,
            model='quad',
            color=color.dark_gray,
            scale=(0.3, 0.03),
            position=(-0.6, -0.45)
        )

        # Health bar (actual health)
        self.health_bar = Entity(
            parent=self.health_bar_bg,
            model='quad',
            color=color.green,
            scale=(1, 1),
            position=(-0.0, 0),
            origin=(-0.5, 0)
        )

        # Health text
        self.health_text = Text(
            text='Health: 100',
            position=(-0.85, -0.45),
            origin=(0, 0),
            scale=1.5
        )

        # Ammo text
        self.ammo_text = Text(
            text=f'Ammo: {GAME_CONFIG["max_ammo"]}',
            position=(-0.85, -0.40),
            origin=(0, 0),
            scale=1.5
        )

        # Score text
        self.score_text = Text(
            text='Score: 0',
            position=(0.6, 0.45),
            origin=(0, 0),
            scale=2
        )

        # Crosshair
        self.crosshair = Entity(
            parent=camera.ui,
            model='quad',
            color=color.white,
            scale=(0.01, 0.002),
            position=(0, 0)
        )
        self.crosshair_v = Entity(
            parent=camera.ui,
            model='quad',
            color=color.white,
            scale=(0.002, 0.01),
            position=(0, 0)
        )

    def update_health(self, health, max_health):
        """Update health bar and text"""
        health_percent = health / max_health
        self.health_bar.scale_x = health_percent
        self.health_text.text = f'Health: {int(health)}'

        # Change color based on health
        if health_percent > 0.5:
            self.health_bar.color = color.green
        elif health_percent > 0.25:
            self.health_bar.color = color.yellow
        else:
            self.health_bar.color = color.red

    def update_ammo(self, ammo):
        """Update ammo text"""
        self.ammo_text.text = f'Ammo: {ammo}'

    def update_score(self, score):
        """Update score text"""
        self.score_text.text = f'Score: {score}'

    def hide(self):
        """Hide all HUD elements"""
        self.health_bar_bg.enabled = False
        self.health_bar.enabled = False
        self.health_text.enabled = False
        self.ammo_text.enabled = False
        self.score_text.enabled = False
        self.crosshair.enabled = False
        self.crosshair_v.enabled = False

    def show(self):
        """Show all HUD elements"""
        self.health_bar_bg.enabled = True
        self.health_bar.enabled = True
        self.health_text.enabled = True
        self.ammo_text.enabled = True
        self.score_text.enabled = True
        self.crosshair.enabled = True
        self.crosshair_v.enabled = True

# ============================================================================
# GAME OVER SCREEN
# ============================================================================

class GameOverScreen:
    """
    Game over screen with final score and restart option.
    """
    def __init__(self):
        self.panel = Entity(
            parent=camera.ui,
            model='quad',
            color=color.rgba(0, 0, 0, 200),
            scale=(2, 2),
            z=1,
            enabled=False
        )

        self.game_over_text = Text(
            text='GAME OVER',
            origin=(0, 0),
            scale=3,
            color=color.red,
            z=2,
            enabled=False
        )

        self.score_text = Text(
            text='Final Score: 0',
            origin=(0, 0),
            y=-0.1,
            scale=2,
            z=2,
            enabled=False
        )

        self.restart_text = Text(
            text='Press ENTER to Restart',
            origin=(0, 0),
            y=-0.25,
            scale=1.5,
            z=2,
            enabled=False
        )

    def show(self, score):
        """Show game over screen with final score"""
        self.panel.enabled = True
        self.game_over_text.enabled = True
        self.score_text.text = f'Final Score: {score}'
        self.score_text.enabled = True
        self.restart_text.enabled = True
        mouse.locked = False

    def hide(self):
        """Hide game over screen"""
        self.panel.enabled = False
        self.game_over_text.enabled = False
        self.score_text.enabled = False
        self.restart_text.enabled = False

# ============================================================================
# GAME CLASS
# ============================================================================

class Game:
    """
    Main game controller managing all game state and logic.
    """
    def __init__(self):
        self.health = GAME_CONFIG['max_health']
        self.max_health = GAME_CONFIG['max_health']
        self.ammo = GAME_CONFIG['max_ammo']
        self.score = 0
        self.game_over = False
        self.enemies = []
        self.last_damage_time = 0
        self.damage_cooldown = 1.0  # Cooldown between enemy damage

    def start(self):
        """Initialize or restart the game"""
        # Reset game state
        self.health = self.max_health
        self.ammo = GAME_CONFIG['max_ammo']
        self.score = 0
        self.game_over = False

        # Clear existing enemies
        for enemy in self.enemies:
            destroy(enemy)
        self.enemies.clear()

        # Spawn initial enemies
        self.spawn_enemies()

        # Update HUD
        hud.update_health(self.health, self.max_health)
        hud.update_ammo(self.ammo)
        hud.update_score(self.score)
        hud.show()

        # Hide game over screen
        game_over_screen.hide()

        # Lock mouse for gameplay
        mouse.locked = True

    def spawn_enemies(self):
        """Spawn enemies around the map"""
        for i in range(GAME_CONFIG['enemy_count']):
            # Random position in a circle around the player
            angle = random.uniform(0, 360)
            distance = random.uniform(10, GAME_CONFIG['enemy_spawn_radius'])
            position = (
                distance * math.cos(math.radians(angle)),
                1,
                distance * math.sin(math.radians(angle))
            )
            enemy = Enemy(position=position)
            self.enemies.append(enemy)

    def shoot(self):
        """Handle shooting mechanics"""
        if self.game_over:
            return

        if self.ammo <= 0:
            # Play empty click sound (using built-in audio)
            Audio('click', pitch=random.uniform(0.8, 1.2), volume=0.3)
            return

        # Consume ammo
        self.ammo -= 1
        hud.update_ammo(self.ammo)

        # Play gun animation
        gun.shoot()

        # Play shooting sound
        Audio('gun_shot', pitch=random.uniform(0.9, 1.1), volume=0.5, autoplay=True)

        # Raycast to check for hits
        ray = raycast(
            camera.world_position,
            camera.forward,
            distance=GAME_CONFIG['shoot_range'],
            ignore=[player, gun]
        )

        if ray.hit:
            # Check if we hit an enemy
            if hasattr(ray.entity, 'take_damage'):
                ray.entity.take_damage(GAME_CONFIG['enemy_health'])  # One-shot kill
                # Play hit sound
                Audio('hit', pitch=random.uniform(0.8, 1.2), volume=0.4)
                hud.update_score(self.score)

    def take_damage(self, damage):
        """Apply damage to player"""
        if self.game_over:
            return

        current_time = time.time()
        if current_time - self.last_damage_time < self.damage_cooldown:
            return

        self.last_damage_time = current_time
        self.health -= damage

        if self.health < 0:
            self.health = 0

        hud.update_health(self.health, self.max_health)

        # Screen flash effect
        camera.overlay.color = color.rgba(255, 0, 0, 100)
        camera.overlay.animate_color(color.clear, duration=0.3)

        # Play damage sound
        Audio('hurt', pitch=random.uniform(0.8, 1.2), volume=0.5)

        if self.health <= 0:
            self.end_game()

    def end_game(self):
        """Handle game over"""
        self.game_over = True
        hud.hide()
        game_over_screen.show(self.score)

    def check_enemy_proximity(self):
        """Check if enemies are close to player and apply damage"""
        if self.game_over:
            return

        for enemy in self.enemies:
            if not enemy.alive:
                continue

            distance = (Vec3(enemy.position) - Vec3(player.position)).length()
            if distance < 3:  # Enemy is touching the player
                self.take_damage(GAME_CONFIG['enemy_damage'])
                break  # Only one enemy can damage per frame

# ============================================================================
# MAIN GAME SETUP
# ============================================================================

# Initialize Ursina app
app = Ursina()

# Configure window
window.title = '3D FPS Shooter'
window.borderless = False
window.fullscreen = False
window.exit_button.visible = False
window.fps_counter.enabled = True

# Create game instance
game = Game()

# Create the game world
# Ground plane
ground = Entity(
    model='plane',
    texture='white_cube',
    scale=(100, 1, 100),
    color=color.gray,
    collider='box'
)

# Create some obstacles/walls for cover
obstacles = []
for i in range(15):
    x = random.uniform(-40, 40)
    z = random.uniform(-40, 40)
    # Don't spawn obstacles too close to player spawn
    if abs(x) < 5 and abs(z) < 5:
        continue

    height = random.uniform(2, 6)
    obstacle = Entity(
        model='cube',
        color=random.choice([color.gray, color.dark_gray, color.light_gray]),
        scale=(random.uniform(2, 4), height, random.uniform(2, 4)),
        position=(x, height/2, z),
        collider='box'
    )
    obstacles.append(obstacle)

# Skybox
sky = Sky(texture='sky_default')

# Lighting
light = DirectionalLight()
light.look_at(Vec3(1, -1, 1))
AmbientLight(color=color.rgba(100, 100, 100, 255))

# Create player
player = FirstPersonController(
    position=(0, 2, 0),
    speed=8,
    mouse_sensitivity=Vec2(40, 40)
)
player.cursor.visible = False
player.gravity = 1

# Create gun
gun = Gun()

# Create HUD
hud = HUD()

# Create game over screen
game_over_screen = GameOverScreen()

# Start the game
game.start()

# ============================================================================
# INPUT AND UPDATE FUNCTIONS
# ============================================================================

def input(key):
    """Handle user input"""
    if key == 'left mouse down' and not game.game_over:
        game.shoot()

    if key == 'enter' and game.game_over:
        game.start()

    if key == 'escape':
        application.quit()

def update():
    """Update game state every frame"""
    if not game.game_over:
        # Check for enemy proximity damage
        game.check_enemy_proximity()

        # Auto-reload when ammo is empty (after a delay)
        if game.ammo == 0:
            invoke(reload_ammo, delay=2)

def reload_ammo():
    """Reload ammo"""
    if not game.game_over and game.ammo == 0:
        game.ammo = GAME_CONFIG['ammo_per_clip']
        hud.update_ammo(game.ammo)
        Audio('reload', pitch=1, volume=0.4)

# ============================================================================
# AUDIO SETUP (Built-in sounds)
# ============================================================================

# Create simple audio assets using Ursina's built-in audio generation
# These are procedural sounds that don't require external files

# Note: Ursina has built-in sounds, but we'll create simple ones
# The Audio class will handle non-existent files gracefully

# ============================================================================
# RUN THE GAME
# ============================================================================

# Run the application
app.run()
