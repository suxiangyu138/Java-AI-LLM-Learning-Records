import pygame
import random
import sys
import math
from collections import deque

# ── 配置 ──────────────────────────────────────────────────
TILE      = 24
COLS      = 25
ROWS      = 20
W         = COLS * TILE
H         = ROWS * TILE
HUD_H     = 56
WIN_W     = W
WIN_H     = H + HUD_H

FPS           = 60
BASE_INTERVAL = 130
SPEED_SCALE   = 0.88
LEVEL_EVERY   = 5
BONUS_CHANCE  = 0.35
BONUS_LIFE    = 6000  # ms

C_BG       = (13,  15,  20)
C_GRID     = (22,  25,  33)
C_HUD_BG   = (21,  24,  32)
C_HUD_LINE = (40,  45,  60)
C_TEXT     = (226, 230, 240)
C_MUTED    = (122, 131, 153)
C_PRIMARY  = (61,  255, 160)
C_SECOND   = (91,  140, 255)
C_FOOD     = (255, 107, 107)
C_BONUS    = (255, 215,   0)
C_DEAD     = (255,  77, 109)
C_BAR_BG   = (40,  45,  60)
SNAKE_HEAD = (61,  235, 175)
SNAKE_TAIL = (20,  110,  60)
LEVEL_NAMES = ['慢速','普通','快速','极快','疯狂']

DIR      = {'UP':(0,-1),'DOWN':(0,1),'LEFT':(-1,0),'RIGHT':(1,0)}
OPPOSITE = {'UP':'DOWN','DOWN':'UP','LEFT':'RIGHT','RIGHT':'LEFT'}


class Particle:
    def __init__(self, x, y, color):
        angle = random.uniform(0, 2*math.pi)
        speed = random.uniform(1.5, 5.0)
        self.x, self.y = float(x), float(y)
        self.vx = math.cos(angle) * speed
        self.vy = math.sin(angle) * speed
        self.r  = random.randint(2, 5)
        self.life = self.max_life = random.uniform(0.4, 0.9)
        self.color = color

    def update(self, dt):
        self.x += self.vx * dt * 60
        self.y += self.vy * dt * 60
        self.vy += 0.1 * dt * 60
        self.life -= dt
        return self.life > 0

    def draw(self, surf):
        a = max(0, self.life / self.max_life)
        r = int(self.r * a)
        if r < 1: return
        pygame.draw.circle(surf, tuple(int(v*a) for v in self.color), (int(self.x), int(self.y)), r)


def draw_rrect(surf, color, rect, radius):
    x, y, w, h = rect
    r = min(radius, w//2, h//2)
    pygame.draw.rect(surf, color, (x+r, y,   w-2*r, h))
    pygame.draw.rect(surf, color, (x,   y+r, w,     h-2*r))
    for cx2, cy2 in [(x+r,y+r),(x+w-r-1,y+r),(x+r,y+h-r-1),(x+w-r-1,y+h-r-1)]:
        pygame.draw.circle(surf, color, (cx2, cy2), r)


class SnakeGame:
    def __init__(self):
        pygame.init()
        pygame.display.set_caption('贪吃蛇  Snake')
        self.screen = pygame.display.set_mode((WIN_W, WIN_H))
        self.clock  = pygame.time.Clock()
        self._load_fonts()
        self.particles = []
        self.best  = 0
        self.phase = 'idle'
        self._init_state()

    def _load_fonts(self):
        # 完全使用 pygame 内置字体，避免调用系统字体枚举（兼容 pygame 2.6+）
        self.f_big = pygame.font.Font(None, 40)
        self.f_med = pygame.font.Font(None, 26)
        self.f_sm  = pygame.font.Font(None, 20)
        self.f_xs  = pygame.font.Font(None, 16)

    def _init_state(self):
        cx, cy = COLS//2, ROWS//2
        self.snake     = deque([(cx,cy),(cx-1,cy),(cx-2,cy)])
        self.snake_set = set(self.snake)
        self.dir = self.next_dir = 'RIGHT'
        self.food  = None
        self.bonus = None
        self.bonus_expire = 0
        self.score = 0
        self.level = 1
        self.tick_acc      = 0
        self.tick_interval = BASE_INTERVAL
        self.blink_on  = True
        self.blink_t   = 0
        self._spawn_food()

    def _interval(self):
        return int(BASE_INTERVAL * (SPEED_SCALE ** (self.level-1)))

    def _rand_cell(self, ex=None):
        ex = ex or set()
        for _ in range(3000):
            p = (random.randint(0,COLS-1), random.randint(0,ROWS-1))
            if p not in ex: return p
        return None

    def _spawn_food(self):
        self.food = self._rand_cell(self.snake_set)

    def _spawn_bonus(self):
        if random.random() < BONUS_CHANCE:
            ex = self.snake_set | ({self.food} if self.food else set())
            p = self._rand_cell(ex)
            if p:
                self.bonus = {'pos':p, 'value':random.choice([3,5,8])}
                self.bonus_expire = pygame.time.get_ticks() + BONUS_LIFE

    def _emit(self, gx, gy, color, n=12):
        px = gx*TILE + TILE//2
        py = gy*TILE + HUD_H + TILE//2
        for _ in range(n): self.particles.append(Particle(px, py, color))

    # ── 游戏 tick ──────────────────────────────────────────
    def _tick(self):
        self.dir = self.next_dir
        dx, dy = DIR[self.dir]
        hx, hy = self.snake[0]
        nx, ny = hx+dx, hy+dy

        if not (0 <= nx < COLS and 0 <= ny < ROWS):
            self._die(); return

        tail = self.snake[-1]
        self.snake_set.discard(tail)
        if (nx,ny) in self.snake_set:
            self.snake_set.add(tail); self._die(); return

        self.snake.appendleft((nx,ny))
        self.snake_set.add((nx,ny))
        grew = False

        if self.food and (nx,ny) == self.food:
            grew = True; self.score += 1
            self._emit(nx, ny, C_FOOD, 12)
            self._spawn_food(); self._spawn_bonus()
        elif self.bonus and (nx,ny) == self.bonus['pos']:
            grew = True; self.score += self.bonus['value']
            self._emit(nx, ny, C_BONUS, 18)
            self.bonus = None

        if self.bonus and pygame.time.get_ticks() > self.bonus_expire:
            self.bonus = None

        if not grew:
            self.snake_set.discard(self.snake.pop())
        else:
            self.snake_set.add(tail)

        if self.score > self.best: self.best = self.score

        nl = self.score // LEVEL_EVERY + 1
        if nl != self.level:
            self.level = nl
            self.tick_interval = self._interval()

    def _die(self):
        hx, hy = self.snake[0]
        self._emit(hx, hy, C_DEAD, 22)
        self.phase = 'dead'

    # ── 绘制 ──────────────────────────────────────────────
    def _draw(self, dt, now):
        # 背景
        self.screen.fill(C_BG)
        for x in range(0, W+1, TILE):
            pygame.draw.line(self.screen, C_GRID, (x, HUD_H), (x, WIN_H))
        for y in range(HUD_H, WIN_H+1, TILE):
            pygame.draw.line(self.screen, C_GRID, (0, y), (W, y))

        # HUD
        pygame.draw.rect(self.screen, C_HUD_BG,   (0,0,WIN_W,HUD_H))
        pygame.draw.line(self.screen, C_HUD_LINE, (0,HUD_H-1),(WIN_W,HUD_H-1))
        self._hud_col(60,  '得分',   str(self.score),  C_PRIMARY)
        self._hud_col(180, '最高分', str(self.best),   C_SECOND)
        lv_name = LEVEL_NAMES[min(self.level-1, len(LEVEL_NAMES)-1)]
        self._hud_col(300, f'等级 {self.level}', lv_name, C_TEXT)
        self._hud_col(430, '长度', str(len(self.snake)), C_MUTED)
        # 等级进度条
        bx, by, bw, bh = WIN_W-110, 18, 90, 5
        pct = (self.score % LEVEL_EVERY) / LEVEL_EVERY
        pygame.draw.rect(self.screen, C_BAR_BG,  (bx,by,bw,bh), border_radius=2)
        pygame.draw.rect(self.screen, C_PRIMARY, (bx,by,int(bw*pct),bh), border_radius=2)
        hl = self.f_xs.render('NEXT LV', True, C_MUTED)
        self.screen.blit(hl, (bx, by+8))

        # 食物
        if self.food:
            fx, fy = self.food
            pulse = 0.85 + 0.15*abs(math.sin(now/600))
            r = int((TILE//2-2)*pulse)
            cx2 = fx*TILE+TILE//2; cy2 = fy*TILE+HUD_H+TILE//2
            gs = pygame.Surface((TILE*3,TILE*3), pygame.SRCALPHA)
            pygame.draw.circle(gs, (*C_FOOD,50), (TILE*3//2,TILE*3//2), TILE)
            self.screen.blit(gs, (cx2-TILE*3//2, cy2-TILE*3//2))
            pygame.draw.circle(self.screen, C_FOOD, (cx2,cy2), r)
            pygame.draw.circle(self.screen, (255,200,200), (cx2-r//4,cy2-r//3), max(1,r//3))

        # 金星
        if self.bonus:
            rem = (self.bonus_expire - now) / BONUS_LIFE
            if rem < 0.25:
                if now - self.blink_t > 100:
                    self.blink_t = now; self.blink_on = not self.blink_on
                if not self.blink_on: pass
                else: self._draw_star(now)
            else:
                self._draw_star(now)

        # 蛇
        snake_list = list(self.snake)
        n = len(snake_list)
        for i in range(n-1, -1, -1):
            sx, sy = snake_list[i]
            t = i / max(n-1, 1)
            color = (
                int(SNAKE_HEAD[0]*(1-t)+SNAKE_TAIL[0]*t),
                int(SNAKE_HEAD[1]*(1-t)+SNAKE_TAIL[1]*t),
                int(SNAKE_HEAD[2]*(1-t)+SNAKE_TAIL[2]*t),
            ) if self.phase != 'dead' else C_DEAD
            pad = 1 if i==0 else 2
            rect = (sx*TILE+pad, sy*TILE+HUD_H+pad, TILE-pad*2, TILE-pad*2)
            draw_rrect(self.screen, color, rect, TILE//4)

        # 蛇头眼睛
        if n > 0 and self.phase != 'dead':
            hx2, hy2 = snake_list[0]
            cx3 = hx2*TILE+TILE//2; cy3 = hy2*TILE+HUD_H+TILE//2
            er = max(2, TILE//8); off = TILE//5
            pts = {
                'RIGHT':((cx3+off,cy3-off),(cx3+off,cy3+off)),
                'LEFT': ((cx3-off,cy3-off),(cx3-off,cy3+off)),
                'UP':   ((cx3-off,cy3-off),(cx3+off,cy3-off)),
                'DOWN': ((cx3-off,cy3+off),(cx3+off,cy3+off)),
            }
            for ep in pts[self.dir]:
                pygame.draw.circle(self.screen, (255,255,255), ep, er)
                pygame.draw.circle(self.screen, C_BG, ep, max(1,er//2))

        # 粒子
        self.particles = [p for p in self.particles if p.update(dt)]
        for p in self.particles: p.draw(self.screen)

        # 覆盖层
        if self.phase == 'idle':
            self._overlay('贪吃蛇  Snake',
                [('Enter / Space  开始游戏', C_TEXT),
                 ('WASD 或 方向键  控制方向', C_MUTED)],
                hint='P=暂停   R=重开   ESC=退出')
        elif self.phase == 'paused':
            self._overlay('已  暂  停',
                [('P / Space  继续', C_PRIMARY),
                 ('R  重新开始',      C_MUTED)])
        elif self.phase == 'dead':
            tag = '新纪录！' if self.score >= self.best and self.score > 0 else '本次得分'
            self._overlay('游 戏 结 束',
                [(f'{tag}  {self.score} 分',             C_PRIMARY),
                 (f'长度 {len(self.snake)}   等级 {self.level}', C_MUTED),
                 ('R  再来一次',                           C_TEXT)],
                hint='ESC=退出')

        pygame.display.flip()

    def _draw_star(self, now):
        bx, by = self.bonus['pos']
        cx2 = bx*TILE+TILE//2; cy2 = by*TILE+HUD_H+TILE//2
        gs = pygame.Surface((TILE*3,TILE*3), pygame.SRCALPHA)
        pygame.draw.circle(gs, (*C_BONUS,60),(TILE*3//2,TILE*3//2),TILE)
        self.screen.blit(gs,(cx2-TILE*3//2,cy2-TILE*3//2))
        star = self.f_med.render('★', True, C_BONUS)
        self.screen.blit(star,(cx2-star.get_width()//2, cy2-star.get_height()//2))
        vt = self.f_xs.render(f'+{self.bonus["value"]}', True, C_HUD_BG)
        self.screen.blit(vt,(cx2-vt.get_width()//2, cy2+TILE//4))

    def _hud_col(self, x, label, value, vc):
        l = self.f_xs.render(label, True, C_MUTED)
        v = self.f_med.render(value, True, vc)
        self.screen.blit(l,(x-l.get_width()//2, 8))
        self.screen.blit(v,(x-v.get_width()//2, 26))

    def _overlay(self, title, lines, hint=''):
        ow, oh = 380, 220
        ox = (WIN_W-ow)//2; oy = (WIN_H-oh)//2
        s = pygame.Surface((ow,oh), pygame.SRCALPHA)
        s.fill((13,15,20,215))
        self.screen.blit(s,(ox,oy))
        pygame.draw.rect(self.screen, C_HUD_LINE,(ox,oy,ow,oh),1,border_radius=12)
        t = self.f_big.render(title, True, C_PRIMARY)
        self.screen.blit(t,(ox+ow//2-t.get_width()//2, oy+18))
        for i,(txt,col) in enumerate(lines):
            ls = self.f_sm.render(txt, True, col)
            self.screen.blit(ls,(ox+ow//2-ls.get_width()//2, oy+72+i*32))
        if hint:
            hs = self.f_xs.render(hint, True, C_MUTED)
            self.screen.blit(hs,(ox+ow//2-hs.get_width()//2, oy+oh-22))

    # ── 主循环 ────────────────────────────────────────────
    def run(self):
        while True:
            dt  = self.clock.tick(FPS) / 1000.0
            now = pygame.time.get_ticks()

            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    pygame.quit(); sys.exit()
                if event.type == pygame.KEYDOWN:
                    self._key(event.key)

            if self.phase == 'playing':
                self.tick_acc += dt * 1000
                while self.tick_acc >= self.tick_interval:
                    self.tick_acc -= self.tick_interval
                    self._tick()
                    if self.phase != 'playing': break

            self._draw(dt, now)

    def _key(self, key):
        dm = {pygame.K_UP:'UP',pygame.K_w:'UP',
              pygame.K_DOWN:'DOWN',pygame.K_s:'DOWN',
              pygame.K_LEFT:'LEFT',pygame.K_a:'LEFT',
              pygame.K_RIGHT:'RIGHT',pygame.K_d:'RIGHT'}
        if key in dm:
            d = dm[key]
            if self.phase == 'playing' and d != OPPOSITE[self.dir]:
                self.next_dir = d
            if self.phase == 'idle': self._start()
            return
        if key in (pygame.K_RETURN, pygame.K_SPACE):
            if self.phase in ('idle','dead'): self._start()
            elif self.phase == 'playing':  self.phase = 'paused'
            elif self.phase == 'paused':   self.phase = 'playing'
        if key == pygame.K_p:
            if self.phase == 'playing': self.phase = 'paused'
            elif self.phase == 'paused': self.phase = 'playing'
        if key == pygame.K_r and self.phase in ('dead','paused'):
            self._start()
        if key == pygame.K_ESCAPE:
            pygame.quit(); sys.exit()

    def _start(self):
        self._init_state()
        self.particles.clear()
        self.phase = 'playing'


if __name__ == '__main__':
    SnakeGame().run()
