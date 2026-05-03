
with open('D:\Desktop\MyLoacalReposity\AI大模型应用开发学习记录\Python贪吃蛇 \snake_pygame.py', 'r', encoding='utf-8') as f:
    code = f.read()

old = '''    def _load_fonts(self):
        self._fn = None  # 默认使用 pygame 内置字体
        for name in ['microsoftyahei', 'Microsoft YaHei', 'simhei', 'SimHei',
                     'notosanscjk', 'wqymicrohei']:
            try:
                f = pygame.font.SysFont(name, 18)
                if f and f.size('A')[0] > 0:
                    self._fn = name
                    break
            except Exception:
                pass
        self.f_big   = pygame.font.SysFont(self._fn, 34, bold=True)
        self.f_med   = pygame.font.SysFont(self._fn, 21, bold=True)
        self.f_sm    = pygame.font.SysFont(self._fn, 15)
        self.f_xs    = pygame.font.SysFont(self._fn, 12)'''

new = '''    def _load_fonts(self):
        # 优先尝试中文系统字体，找不到就用 pygame 内置字体
        cjk = ['microsoftyahei', 'Microsoft YaHei', 'simhei', 'SimHei',
               'notosanscjk', 'wqymicrohei']
        found = None
        for name in cjk:
            if name.lower() in [n.lower() for n in pygame.font.get_fonts()]:
                found = name
                break

        def make(size, bold=False):
            if found:
                return pygame.font.SysFont(found, size, bold=bold)
            else:
                # pygame 内置字体用 Font(None, size)
                f = pygame.font.Font(None, size)
                return f

        self.f_big = make(34, bold=True)
        self.f_med = make(21, bold=True)
        self.f_sm  = make(15)
        self.f_xs  = make(12)'''

assert old in code, "原文本未找到"
code = code.replace(old, new)

with open('D:\Desktop\MyLoacalReposity\AI大模型应用开发学习记录\Python贪吃蛇\snake_pygame.py', 'w', encoding='utf-8') as f:
    f.write(code)
print("修复完成！")