#!/usr/bin/env python3
"""
NEON SNAKE vs AI
A neon-styled snake game where you compete against an AI opponent.
The AI uses BFS pathfinding to hunt food. Can you beat the machine?

Controls:
  Arrow keys / WASD - Move
  SPACE - Start / Restart
  ESC - Quit
  P - Pause

pip install pygame
"""

import pygame
import sys
import random
import math
from collections import deque
from enum import Enum

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
CELL = 20                       # pixel size of one grid cell
COLS, ROWS = 40, 30             # grid dimensions
WIDTH, HEIGHT = COLS * CELL, ROWS * CELL
PANEL_H = 60                    # score panel height at top
WIN_W, WIN_H = WIDTH, HEIGHT + PANEL_H
FPS = 12                        # base game speed

# Colours (R, G, B)
BLACK       = (0,   0,   0)
DARK_BG     = (5,   5,  15)
NEON_GREEN  = (57,  255, 20)
NEON_PINK   = (255, 20,  147)
NEON_CYAN   = (0,   255, 255)
NEON_YELLOW = (255, 255, 0)
NEON_ORANGE = (255, 165, 0)
NEON_PURPLE = (180, 0,   255)
NEON_RED    = (255, 40,  40)
WHITE       = (255, 255, 255)
DIM_WHITE   = (100, 100, 100)
GRID_COLOR  = (15,  15,  35)

# Directions
UP    = (0, -1)
DOWN  = (0,  1)
LEFT  = (-1, 0)
RIGHT = (1,  0)

class PowerUpType(Enum):
    SPEED_BOOST = 1   # player moves faster temporarily
    SLOW_ENEMY  = 2   # AI moves slower temporarily
    DOUBLE_POINTS = 3 # 2x score for a bit
    WALL_PHASE  = 4   # pass through walls briefly


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def glow_surface(radius, color, intensity=1.0):
    """Create a soft circular glow surface."""
    size = radius * 2
    surf = pygame.Surface((size, size), pygame.SRCALPHA)
    for r in range(radius, 0, -1):
        alpha = int(40 * intensity * (r / radius))
        alpha = min(255, max(0, alpha))
        c = (*color[:3], alpha)
        pygame.draw.circle(surf, c, (radius, radius), r)
    return surf


def lerp_color(c1, c2, t):
    t = max(0, min(1, t))
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


class Particle:
    __slots__ = ('x', 'y', 'vx', 'vy', 'life', 'max_life', 'color', 'size')

    def __init__(self, x, y, color):
        angle = random.uniform(0, 2 * math.pi)
        speed = random.uniform(1, 5)
        self.x = x
        self.y = y
        self.vx = math.cos(angle) * speed
        self.vy = math.sin(angle) * speed
        self.life = random.uniform(0.3, 1.0)
        self.max_life = self.life
        self.color = color
        self.size = random.uniform(2, 5)

    def update(self, dt):
        self.x += self.vx
        self.y += self.vy
        self.vx *= 0.96
        self.vy *= 0.96
        self.life -= dt
        return self.life > 0

    def draw(self, surface):
        t = self.life / self.max_life
        alpha = int(255 * t)
        r = max(1, int(self.size * t))
        s = pygame.Surface((r * 2, r * 2), pygame.SRCALPHA)
        pygame.draw.circle(s, (*self.color[:3], alpha), (r, r), r)
        surface.blit(s, (int(self.x) - r, int(self.y) - r))


class ScreenShake:
    def __init__(self):
        self.intensity = 0
        self.decay = 0.85

    def trigger(self, intensity=6):
        self.intensity = intensity

    def update(self):
        if self.intensity > 0.5:
            self.intensity *= self.decay
            ox = random.randint(-int(self.intensity), int(self.intensity))
            oy = random.randint(-int(self.intensity), int(self.intensity))
            return ox, oy
        self.intensity = 0
        return 0, 0


# ---------------------------------------------------------------------------
# Sound generator (no asset files needed)
# ---------------------------------------------------------------------------

class SoundFX:
    def __init__(self):
        try:
            pygame.mixer.init(44100, -16, 1, 512)
            self.enabled = True
        except Exception:
            self.enabled = False

    def _make_sound(self, freq, duration_ms, wave='square', volume=0.15):
        if not self.enabled:
            return None
        sample_rate = 44100
        n_samples = int(sample_rate * duration_ms / 1000)
        buf = bytearray(n_samples * 2)
        for i in range(n_samples):
            t = i / sample_rate
            if wave == 'square':
                val = volume if math.sin(2 * math.pi * freq * t) > 0 else -volume
            elif wave == 'saw':
                val = volume * (2 * (freq * t % 1) - 1)
            else:  # sine
                val = volume * math.sin(2 * math.pi * freq * t)
            # fade out
            fade = 1.0 - (i / n_samples)
            sample = int(val * fade * 32767)
            sample = max(-32768, min(32767, sample))
            buf[i * 2] = sample & 0xFF
            buf[i * 2 + 1] = (sample >> 8) & 0xFF
        sound = pygame.mixer.Sound(buffer=bytes(buf))
        return sound

    def eat(self):
        s = self._make_sound(880, 80, 'square', 0.12)
        if s: s.play()

    def die(self):
        s = self._make_sound(150, 300, 'saw', 0.18)
        if s: s.play()

    def powerup(self):
        s = self._make_sound(1200, 120, 'sine', 0.10)
        if s: s.play()

    def ai_eat(self):
        s = self._make_sound(440, 80, 'square', 0.08)
        if s: s.play()


# ---------------------------------------------------------------------------
# Snake
# ---------------------------------------------------------------------------

class Snake:
    def __init__(self, start_pos, color, glow_color, name="Snake"):
        self.body = deque([start_pos])
        self.direction = random.choice([UP, DOWN, LEFT, RIGHT])
        self.grow_pending = 3  # start with length 4
        self.color = color
        self.glow_color = glow_color
        self.name = name
        self.alive = True
        self.score = 0
        self.combo = 1
        self.speed_mod = 1.0
        self.effects = {}  # PowerUpType -> frames remaining
        self.trail_timer = 0

    @property
    def head(self):
        return self.body[0]

    def set_direction(self, d):
        # prevent 180-degree reversal
        if len(self.body) > 1:
            opposite = (-self.direction[0], -self.direction[1])
            if d == opposite:
                return
        self.direction = d

    def move(self, walls_phase=False):
        if not self.alive:
            return
        hx, hy = self.head
        dx, dy = self.direction
        nx, ny = hx + dx, hy + dy
        # wall wrapping or collision
        if walls_phase or PowerUpType.WALL_PHASE in self.effects:
            nx %= COLS
            ny %= ROWS
        self.body.appendleft((nx, ny))
        if self.grow_pending > 0:
            self.grow_pending -= 1
        else:
            self.body.pop()

    def check_wall_collision(self):
        hx, hy = self.head
        if hx < 0 or hx >= COLS or hy < 0 or hy >= ROWS:
            return True
        return False

    def check_self_collision(self):
        return self.head in list(self.body)[1:]

    def check_collision_with(self, other_body):
        return self.head in other_body

    def grow(self, amount=1):
        self.grow_pending += amount

    def tick_effects(self):
        expired = []
        for etype in list(self.effects):
            self.effects[etype] -= 1
            if self.effects[etype] <= 0:
                expired.append(etype)
        for e in expired:
            del self.effects[e]
            if e == PowerUpType.SPEED_BOOST:
                self.speed_mod = 1.0
            if e == PowerUpType.SLOW_ENEMY:
                pass  # handled externally
            if e == PowerUpType.DOUBLE_POINTS:
                pass

    def draw(self, surface, offset=(0, 0)):
        ox, oy = offset
        body_list = list(self.body)

        # Draw glow under each segment
        glow = glow_surface(CELL, self.glow_color, 0.6)
        for (bx, by) in body_list:
            px = bx * CELL + ox
            py = by * CELL + oy + PANEL_H
            surface.blit(glow, (px + CELL // 2 - CELL, py + CELL // 2 - CELL))

        # Draw body segments
        for i, (bx, by) in enumerate(body_list):
            px = bx * CELL + ox
            py = by * CELL + oy + PANEL_H
            t = i / max(len(body_list), 1)
            c = lerp_color(self.color, (30, 30, 60), t * 0.6)
            rect = pygame.Rect(px + 1, py + 1, CELL - 2, CELL - 2)
            pygame.draw.rect(surface, c, rect, border_radius=4)

            # head highlight
            if i == 0:
                pygame.draw.rect(surface, WHITE, rect, 1, border_radius=4)
                # eyes
                dx, dy = self.direction
                eye_offset = 5
                ex1 = px + CELL // 2 + dy * eye_offset - dx * 3
                ey1 = py + CELL // 2 - dx * eye_offset - dy * 3
                ex2 = px + CELL // 2 - dy * eye_offset - dx * 3
                ey2 = py + CELL // 2 + dx * eye_offset - dy * 3
                pygame.draw.circle(surface, WHITE, (int(ex1), int(ey1)), 3)
                pygame.draw.circle(surface, WHITE, (int(ex2), int(ey2)), 3)
                pygame.draw.circle(surface, BLACK, (int(ex1 + dx), int(ey1 + dy)), 1)
                pygame.draw.circle(surface, BLACK, (int(ex2 + dx), int(ey2 + dy)), 1)


# ---------------------------------------------------------------------------
# AI Brain (BFS pathfinding)
# ---------------------------------------------------------------------------

class AIBrain:
    def __init__(self):
        self.path = []
        self.wander_dir = random.choice([UP, DOWN, LEFT, RIGHT])
        self.recalc_timer = 0

    def bfs(self, start, goal, obstacles):
        """BFS to find shortest path from start to goal avoiding obstacles."""
        if start == goal:
            return []
        visited = {start}
        queue = deque([(start, [])])
        while queue:
            (cx, cy), path = queue.popleft()
            for d in [UP, DOWN, LEFT, RIGHT]:
                nx, ny = cx + d[0], cy + d[1]
                if 0 <= nx < COLS and 0 <= ny < ROWS and (nx, ny) not in visited and (nx, ny) not in obstacles:
                    new_path = path + [d]
                    if (nx, ny) == goal:
                        return new_path
                    visited.add((nx, ny))
                    queue.append(((nx, ny), new_path))
        return []  # no path found

    def decide(self, snake, food_pos, obstacles, power_ups):
        """Pick the best direction for the AI snake."""
        self.recalc_timer -= 1
        # Try to reach food via BFS
        if self.recalc_timer <= 0 or not self.path:
            obstacle_set = set(obstacles)
            # also look for nearest power-up
            targets = [food_pos] + [p[0] for p in power_ups]
            best_path = None
            for target in targets:
                p = self.bfs(snake.head, target, obstacle_set)
                if p and (best_path is None or len(p) < len(best_path)):
                    best_path = p
            self.path = best_path if best_path else []
            self.recalc_timer = 4  # recalculate every N frames

        if self.path:
            d = self.path.pop(0)
            snake.set_direction(d)
            return

        # Fallback: wander safely
        hx, hy = snake.head
        safe_dirs = []
        for d in [UP, DOWN, LEFT, RIGHT]:
            nx, ny = hx + d[0], hy + d[1]
            if 0 <= nx < COLS and 0 <= ny < ROWS and (nx, ny) not in obstacles:
                safe_dirs.append(d)
        if safe_dirs:
            # prefer current direction if safe
            if snake.direction in safe_dirs:
                return
            snake.set_direction(random.choice(safe_dirs))
        # else: doomed


# ---------------------------------------------------------------------------
# Power-Up
# ---------------------------------------------------------------------------

class PowerUp:
    COLORS = {
        PowerUpType.SPEED_BOOST:   NEON_YELLOW,
        PowerUpType.SLOW_ENEMY:    NEON_ORANGE,
        PowerUpType.DOUBLE_POINTS: NEON_PURPLE,
        PowerUpType.WALL_PHASE:    NEON_CYAN,
    }
    SYMBOLS = {
        PowerUpType.SPEED_BOOST:   "S",
        PowerUpType.SLOW_ENEMY:    "W",
        PowerUpType.DOUBLE_POINTS: "2x",
        PowerUpType.WALL_PHASE:    "G",
    }

    def __init__(self, pos, ptype):
        self.pos = pos
        self.ptype = ptype
        self.timer = FPS * 15  # disappear after 15 sec
        self.pulse = 0

    def update(self):
        self.timer -= 1
        self.pulse += 0.15
        return self.timer > 0

    def draw(self, surface, offset=(0, 0)):
        ox, oy = offset
        px = self.pos[0] * CELL + ox
        py = self.pos[1] * CELL + oy + PANEL_H
        color = self.COLORS[self.ptype]
        pulse_size = int(2 * math.sin(self.pulse))
        rect = pygame.Rect(px - pulse_size, py - pulse_size,
                           CELL + pulse_size * 2, CELL + pulse_size * 2)
        # glow
        g = glow_surface(CELL + 6, color, 0.5)
        surface.blit(g, (px + CELL // 2 - CELL - 6, py + CELL // 2 - CELL - 6))
        pygame.draw.rect(surface, color, rect, border_radius=6)
        # symbol
        font = pygame.font.SysFont("monospace", 12, bold=True)
        txt = font.render(self.SYMBOLS[self.ptype], True, BLACK)
        surface.blit(txt, (px + CELL // 2 - txt.get_width() // 2,
                           py + CELL // 2 - txt.get_height() // 2))


# ---------------------------------------------------------------------------
# Game
# ---------------------------------------------------------------------------

class Game:
    def __init__(self, screen):
        self.screen = screen
        self.clock = pygame.time.Clock()
        self.sfx = SoundFX()
        self.shake = ScreenShake()
        self.font_big = pygame.font.SysFont("monospace", 48, bold=True)
        self.font_med = pygame.font.SysFont("monospace", 28, bold=True)
        self.font_sm = pygame.font.SysFont("monospace", 18)
        self.font_xs = pygame.font.SysFont("monospace", 14)
        self.state = "menu"  # menu, playing, paused, gameover
        self.particles = []
        self.powerups = []
        self.high_score = 0
        self.reset()

    def reset(self):
        mid = (COLS // 2, ROWS // 2)
        self.player = Snake((COLS // 4, ROWS // 2), NEON_GREEN, (0, 180, 0), "YOU")
        self.player.direction = RIGHT
        self.ai = Snake((3 * COLS // 4, ROWS // 2), NEON_PINK, (180, 0, 100), "AI")
        self.ai.direction = LEFT
        self.ai_brain = AIBrain()
        self.food = self._spawn_food()
        self.particles = []
        self.powerups = []
        self.powerup_cooldown = FPS * 8  # first power-up after 8 sec
        self.frame = 0
        self.ai_tick_accum = 0
        self.player_tick_accum = 0
        self.game_time = 0

    def _occupied(self):
        s = set(self.player.body) | set(self.ai.body)
        s |= {p.pos for p in self.powerups}
        return s

    def _spawn_food(self):
        occupied = self._occupied()
        while True:
            pos = (random.randint(0, COLS - 1), random.randint(0, ROWS - 1))
            if pos not in occupied:
                return pos

    def _spawn_powerup(self):
        occupied = self._occupied() | {self.food}
        for _ in range(50):
            pos = (random.randint(0, COLS - 1), random.randint(0, ROWS - 1))
            if pos not in occupied:
                ptype = random.choice(list(PowerUpType))
                self.powerups.append(PowerUp(pos, ptype))
                return

    def _spawn_particles(self, x, y, color, count=12):
        px = x * CELL + CELL // 2
        py = y * CELL + CELL // 2 + PANEL_H
        for _ in range(count):
            self.particles.append(Particle(px, py, color))

    def _apply_powerup(self, snake, ptype, other_snake):
        duration = FPS * 6  # 6 seconds
        if ptype == PowerUpType.SPEED_BOOST:
            snake.effects[ptype] = duration
            snake.speed_mod = 1.5
        elif ptype == PowerUpType.SLOW_ENEMY:
            other_snake.effects[ptype] = duration
            other_snake.speed_mod = 0.5
        elif ptype == PowerUpType.DOUBLE_POINTS:
            snake.effects[ptype] = duration
        elif ptype == PowerUpType.WALL_PHASE:
            snake.effects[ptype] = duration

    def handle_input(self):
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                pygame.quit()
                sys.exit()
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    pygame.quit()
                    sys.exit()
                if self.state == "menu":
                    if event.key == pygame.K_SPACE:
                        self.state = "playing"
                elif self.state == "gameover":
                    if event.key == pygame.K_SPACE:
                        self.reset()
                        self.state = "playing"
                elif self.state == "playing":
                    if event.key == pygame.K_p:
                        self.state = "paused"
                    # Player movement
                    if event.key in (pygame.K_UP, pygame.K_w):
                        self.player.set_direction(UP)
                    elif event.key in (pygame.K_DOWN, pygame.K_s):
                        self.player.set_direction(DOWN)
                    elif event.key in (pygame.K_LEFT, pygame.K_a):
                        self.player.set_direction(LEFT)
                    elif event.key in (pygame.K_RIGHT, pygame.K_d):
                        self.player.set_direction(RIGHT)
                elif self.state == "paused":
                    if event.key == pygame.K_p:
                        self.state = "playing"

    def update(self):
        if self.state != "playing":
            return

        self.frame += 1
        self.game_time += 1

        dt = 1 / FPS

        # Update particles
        self.particles = [p for p in self.particles if p.update(dt)]

        # Tick effects
        self.player.tick_effects()
        self.ai.tick_effects()

        # Power-up spawning
        self.powerup_cooldown -= 1
        if self.powerup_cooldown <= 0 and len(self.powerups) < 3:
            self._spawn_powerup()
            self.powerup_cooldown = FPS * random.randint(8, 15)

        # Update power-ups
        self.powerups = [p for p in self.powerups if p.update()]

        # --- Player movement (with speed mod) ---
        player_speed = FPS * self.player.speed_mod
        if PowerUpType.SLOW_ENEMY in self.player.effects:
            player_speed *= 0.5
        self.player_tick_accum += player_speed / FPS
        while self.player_tick_accum >= 1.0 and self.player.alive:
            self.player_tick_accum -= 1.0
            self.player.move()
            self._check_player()

        # --- AI movement (with speed mod) ---
        ai_speed = FPS * self.ai.speed_mod
        if PowerUpType.SLOW_ENEMY in self.ai.effects:
            ai_speed *= 0.5

        # AI gets slightly faster as game progresses
        ai_speed *= min(1.3, 1.0 + self.game_time / (FPS * 120))

        self.ai_tick_accum += ai_speed / FPS
        while self.ai_tick_accum >= 1.0 and self.ai.alive:
            self.ai_tick_accum -= 1.0
            obstacles = set(list(self.player.body) + list(self.ai.body)[1:])
            self.ai_brain.decide(self.ai, self.food, obstacles, [(p.pos, p.ptype) for p in self.powerups])
            self.ai.move()
            self._check_ai()

        # Check game over
        if not self.player.alive or not self.ai.alive:
            self.state = "gameover"
            self.sfx.die()
            self.shake.trigger(12)
            if self.player.score > self.high_score:
                self.high_score = self.player.score

    def _check_player(self):
        p = self.player
        if p.check_wall_collision() and PowerUpType.WALL_PHASE not in p.effects:
            p.alive = False
            self._spawn_particles(*p.head, NEON_GREEN, 25)
            return
        # wrap if wall phase
        if PowerUpType.WALL_PHASE in p.effects:
            hx, hy = p.body[0]
            p.body[0] = (hx % COLS, hy % ROWS)
        if p.check_self_collision():
            p.alive = False
            self._spawn_particles(*p.head, NEON_GREEN, 25)
            return
        if p.check_collision_with(list(self.ai.body)):
            p.alive = False
            self._spawn_particles(*p.head, NEON_GREEN, 25)
            return
        # eat food
        if p.head == self.food:
            mult = 2 if PowerUpType.DOUBLE_POINTS in p.effects else 1
            p.score += 10 * p.combo * mult
            p.combo = min(p.combo + 1, 8)
            p.grow(1)
            self._spawn_particles(*self.food, NEON_YELLOW, 15)
            self.sfx.eat()
            self.shake.trigger(4)
            self.food = self._spawn_food()
        # eat power-up
        for pu in self.powerups[:]:
            if p.head == pu.pos:
                self._apply_powerup(p, pu.ptype, self.ai)
                self._spawn_particles(*pu.pos, PowerUp.COLORS[pu.ptype], 10)
                self.sfx.powerup()
                self.powerups.remove(pu)

    def _check_ai(self):
        a = self.ai
        if a.check_wall_collision():
            a.alive = False
            self._spawn_particles(*a.head, NEON_PINK, 25)
            return
        if a.check_self_collision():
            a.alive = False
            self._spawn_particles(*a.head, NEON_PINK, 25)
            return
        if a.check_collision_with(list(self.player.body)):
            a.alive = False
            self._spawn_particles(*a.head, NEON_PINK, 25)
            return
        # eat food
        if a.head == self.food:
            a.score += 10
            a.grow(1)
            self._spawn_particles(*self.food, NEON_YELLOW, 15)
            self.sfx.ai_eat()
            self.food = self._spawn_food()
            # Reset player combo when AI eats
            self.player.combo = 1
        # eat power-up
        for pu in self.powerups[:]:
            if a.head == pu.pos:
                self._apply_powerup(a, pu.ptype, self.player)
                self._spawn_particles(*pu.pos, PowerUp.COLORS[pu.ptype], 10)
                self.powerups.remove(pu)

    def draw(self):
        shake_offset = self.shake.update()

        self.screen.fill(DARK_BG)

        # Grid lines
        for x in range(0, WIDTH, CELL):
            pygame.draw.line(self.screen, GRID_COLOR, (x, PANEL_H), (x, WIN_H))
        for y in range(PANEL_H, WIN_H, CELL):
            pygame.draw.line(self.screen, GRID_COLOR, (0, y), (WIDTH, y))

        # Border glow
        border_color = (30, 30, 80)
        pygame.draw.rect(self.screen, border_color, (0, PANEL_H, WIDTH, HEIGHT), 2)

        # Food with pulsing glow
        fx, fy = self.food
        pulse = 0.7 + 0.3 * math.sin(self.frame * 0.2)
        food_glow = glow_surface(CELL + 8, NEON_RED, pulse)
        fpx = fx * CELL + shake_offset[0]
        fpy = fy * CELL + shake_offset[1] + PANEL_H
        self.screen.blit(food_glow, (fpx + CELL // 2 - CELL - 8, fpy + CELL // 2 - CELL - 8))
        food_rect = pygame.Rect(fpx + 2, fpy + 2, CELL - 4, CELL - 4)
        fc = lerp_color(NEON_RED, NEON_ORANGE, 0.5 + 0.5 * math.sin(self.frame * 0.15))
        pygame.draw.rect(self.screen, fc, food_rect, border_radius=CELL // 2)

        # Power-ups
        for pu in self.powerups:
            pu.draw(self.screen, shake_offset)

        # Snakes
        self.player.draw(self.screen, shake_offset)
        self.ai.draw(self.screen, shake_offset)

        # Particles
        for p in self.particles:
            p.draw(self.screen)

        # --- HUD Panel ---
        pygame.draw.rect(self.screen, (10, 10, 25), (0, 0, WIDTH, PANEL_H))
        pygame.draw.line(self.screen, (40, 40, 80), (0, PANEL_H), (WIDTH, PANEL_H), 2)

        # Player info (left)
        p_label = self.font_sm.render("YOU", True, NEON_GREEN)
        p_score = self.font_med.render(str(self.player.score), True, NEON_GREEN)
        self.screen.blit(p_label, (10, 5))
        self.screen.blit(p_score, (10, 25))

        # Combo
        if self.player.combo > 1:
            combo_text = self.font_xs.render(f"x{self.player.combo} COMBO", True, NEON_YELLOW)
            self.screen.blit(combo_text, (100, 35))

        # Active effects
        eff_x = 100
        for etype, frames in self.player.effects.items():
            secs = max(1, frames // FPS)
            color = PowerUp.COLORS[etype]
            txt = self.font_xs.render(f"{PowerUp.SYMBOLS[etype]}:{secs}s", True, color)
            self.screen.blit(txt, (eff_x, 8))
            eff_x += 60

        # AI info (right)
        a_label = self.font_sm.render("AI", True, NEON_PINK)
        a_score = self.font_med.render(str(self.ai.score), True, NEON_PINK)
        self.screen.blit(a_label, (WIDTH - 80, 5))
        self.screen.blit(a_score, (WIDTH - 80, 25))

        # AI effects
        eff_x = WIDTH - 200
        for etype, frames in self.ai.effects.items():
            secs = max(1, frames // FPS)
            color = PowerUp.COLORS[etype]
            txt = self.font_xs.render(f"{PowerUp.SYMBOLS[etype]}:{secs}s", True, color)
            self.screen.blit(txt, (eff_x, 8))
            eff_x += 60

        # Center info
        length_txt = self.font_xs.render(
            f"Length: {len(self.player.body)} vs {len(self.ai.body)}", True, DIM_WHITE
        )
        self.screen.blit(length_txt, (WIDTH // 2 - length_txt.get_width() // 2, 5))

        # High score
        if self.high_score > 0:
            hs_txt = self.font_xs.render(f"Best: {self.high_score}", True, DIM_WHITE)
            self.screen.blit(hs_txt, (WIDTH // 2 - hs_txt.get_width() // 2, 25))

        # Time
        time_secs = self.game_time // FPS
        time_txt = self.font_xs.render(f"{time_secs // 60}:{time_secs % 60:02d}", True, DIM_WHITE)
        self.screen.blit(time_txt, (WIDTH // 2 - time_txt.get_width() // 2, 42))

        # --- Overlay screens ---
        if self.state == "menu":
            self._draw_overlay()
            self._draw_text_center("NEON SNAKE vs AI", self.font_big, NEON_CYAN, -60)
            self._draw_text_center("You are GREEN. The AI is PINK.", self.font_sm, DIM_WHITE, 0)
            self._draw_text_center("Arrow keys / WASD to move", self.font_sm, DIM_WHITE, 25)
            self._draw_text_center("Collect food. Avoid walls and snakes.", self.font_sm, DIM_WHITE, 50)
            self._draw_text_center("Grab power-ups for the edge!", self.font_sm, NEON_YELLOW, 75)
            self._draw_text_center("[ SPACE to start ]", self.font_med, WHITE, 130)

        elif self.state == "paused":
            self._draw_overlay()
            self._draw_text_center("PAUSED", self.font_big, NEON_YELLOW, -20)
            self._draw_text_center("[ P to resume ]", self.font_med, WHITE, 40)

        elif self.state == "gameover":
            self._draw_overlay()
            if self.player.alive and not self.ai.alive:
                result = "YOU WIN!"
                result_color = NEON_GREEN
            elif not self.player.alive and self.ai.alive:
                result = "AI WINS!"
                result_color = NEON_PINK
            elif not self.player.alive and not self.ai.alive:
                result = "DRAW!"
                result_color = NEON_YELLOW
            else:
                result = "GAME OVER"
                result_color = WHITE
            self._draw_text_center(result, self.font_big, result_color, -50)
            self._draw_text_center(
                f"You: {self.player.score}  |  AI: {self.ai.score}",
                self.font_med, DIM_WHITE, 10
            )
            self._draw_text_center("[ SPACE to restart ]", self.font_med, WHITE, 60)

        pygame.display.flip()

    def _draw_overlay(self):
        overlay = pygame.Surface((WIN_W, WIN_H), pygame.SRCALPHA)
        overlay.fill((0, 0, 0, 160))
        self.screen.blit(overlay, (0, 0))

    def _draw_text_center(self, text, font, color, y_offset):
        txt = font.render(text, True, color)
        x = WIN_W // 2 - txt.get_width() // 2
        y = WIN_H // 2 + y_offset
        self.screen.blit(txt, (x, y))

    def run(self):
        while True:
            self.handle_input()
            self.update()
            self.draw()
            self.clock.tick(FPS)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    pygame.init()
    screen = pygame.display.set_mode((WIN_W, WIN_H))
    pygame.display.set_caption("NEON SNAKE vs AI")
    game = Game(screen)
    game.run()


if __name__ == "__main__":
    main()
