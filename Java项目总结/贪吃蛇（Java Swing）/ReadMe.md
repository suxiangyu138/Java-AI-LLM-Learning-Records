Java Swing 贪吃蛇这类项目通常就是围绕窗口绘制、键盘事件、定时更新、碰撞检测和对象拆分来实现，常见实现组合是 `JFrame + JPanel + KeyListener + Timer + Snake/Food`。 [blog.csdn](https://blog.csdn.net/u012970287/article/details/148202567)

## 设计思路

这个项目最适合做成“窗口类 + 游戏面板类 + 数据对象类”的结构：主窗口负责展示，游戏面板负责绘制和逻辑更新，蛇和食物作为独立对象保存状态，这符合 Swing 贪吃蛇常见的面向对象拆分方式。 [developer.aliyun](https://developer.aliyun.com/article/1649867)
在驱动方式上，Swing 项目里常用 `Timer` 周期性触发游戏刷新，再通过 `KeyListener` 监听方向键控制蛇移动，这比直接 `Thread.sleep()` 更适合图形界面场景。 [cloud.tencent](https://cloud.tencent.com/developer/article/2164168)

你可以这样理解 4 个核心点：

- GUI 编程：用 `JFrame` 创建窗口、`JPanel` 绘制地图、蛇和食物。 [blog.csdn](https://blog.csdn.net/m0_74549522/article/details/131187612)
- 事件监听：监听键盘上下左右和空格键，实现转向、开始、重新开始。 [juejin](https://juejin.cn/post/7055284056577015838)
- 面向对象：拆成 `SnakeGame`、`GamePanel`、`Node` 等类，职责清晰。 [blog.51cto](https://blog.51cto.com/u_16213711/11628291)
- 多线程/动态刷新：通过 `Timer` 定时执行移动、碰撞检测和重绘，形成持续运行的游戏循环。 [cnblogs](https://www.cnblogs.com/XXZ-JAVA/articles/15313008.html)

## 完整代码

下面这份代码是 **可直接运行** 的简化版，单文件即可运行，适合你交作业、练 Swing、做课堂展示。  
文件名建议就叫 `SnakeGame.java`。

```java
class SnakeGame extends javax.swing.JFrame {
    SnakeGame() {
        setTitle("贪吃蛇");
        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(new GamePanel());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(SnakeGame::new);
    }
}

class GamePanel extends javax.swing.JPanel implements java.awt.event.KeyListener, java.awt.event.ActionListener {
    static final int CELL = 25;
    static final int ROWS = 24;
    static final int COLS = 24;
    static final int WIDTH = COLS * CELL;
    static final int HEIGHT = ROWS * CELL;
    static final int DELAY = 120;

    java.util.LinkedList<Node> snake = new java.util.LinkedList<>();
    Node food;
    char direction;
    boolean running;
    boolean started;
    int score;
    javax.swing.Timer timer;
    java.util.Random random = new java.util.Random();

    GamePanel() {
        setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT + 50));
        setBackground(java.awt.Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        initGame();
        timer = new javax.swing.Timer(DELAY, this);
        timer.start();
    }

    void initGame() {
        snake.clear();
        snake.add(new Node(8, 10));
        snake.add(new Node(7, 10));
        snake.add(new Node(6, 10));
        direction = 'R';
        running = true;
        started = false;
        score = 0;
        spawnFood();
        repaint();
    }

    void spawnFood() {
        while (true) {
            int x = random.nextInt(COLS);
            int y = random.nextInt(ROWS);
            boolean ok = true;
            for (Node n : snake) {
                if (n.x == x && n.y == y) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                food = new Node(x, y);
                return;
            }
        }
    }

    void move() {
        Node head = snake.getFirst();
        int x = head.x;
        int y = head.y;

        if (direction == 'U') y--;
        else if (direction == 'D') y++;
        else if (direction == 'L') x--;
        else if (direction == 'R') x++;

        Node newHead = new Node(x, y);
        snake.addFirst(newHead);

        if (x == food.x && y == food.y) {
            score += 10;
            spawnFood();
        } else {
            snake.removeLast();
        }
    }

    void checkCollision() {
        Node head = snake.getFirst();

        if (head.x < 0 || head.x >= COLS || head.y < 0 || head.y >= ROWS) {
            running = false;
            return;
        }

        for (int i = 1; i < snake.size(); i++) {
            Node body = snake.get(i);
            if (head.x == body.x && head.y == body.y) {
                running = false;
                return;
            }
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (started && running) {
            move();
            checkCollision();
        }
        repaint();
    }

    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);

        g.setColor(new java.awt.Color(30, 30, 30));
        for (int i = 0; i <= COLS; i++) {
            g.drawLine(i * CELL, 0, i * CELL, HEIGHT);
        }
        for (int i = 0; i <= ROWS; i++) {
            g.drawLine(0, i * CELL, WIDTH, i * CELL);
        }

        g.setColor(java.awt.Color.RED);
        g.fillOval(food.x * CELL, food.y * CELL, CELL, CELL);

        for (int i = 0; i < snake.size(); i++) {
            Node n = snake.get(i);
            if (i == 0) g.setColor(java.awt.Color.GREEN);
            else g.setColor(new java.awt.Color(0, 180, 0));
            g.fillRect(n.x * CELL, n.y * CELL, CELL, CELL);
        }

        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("微软雅黑", java.awt.Font.BOLD, 20));
        g.drawString("分数: " + score, 20, HEIGHT + 30);

        if (!started) {
            g.setFont(new java.awt.Font("微软雅黑", java.awt.Font.BOLD, 24));
            g.drawString("按空格开始游戏", WIDTH / 2 - 90, HEIGHT / 2);
        }

        if (!running) {
            g.setColor(java.awt.Color.YELLOW);
            g.setFont(new java.awt.Font("微软雅黑", java.awt.Font.BOLD, 28));
            g.drawString("游戏结束", WIDTH / 2 - 70, HEIGHT / 2 - 20);
            g.setFont(new java.awt.Font("微软雅黑", java.awt.Font.PLAIN, 22));
            g.drawString("按空格重新开始", WIDTH / 2 - 95, HEIGHT / 2 + 20);
        }
    }

    public void keyPressed(java.awt.event.KeyEvent e) {
        int code = e.getKeyCode();

        if (code == java.awt.event.KeyEvent.VK_SPACE) {
            if (!started) {
                started = true;
            } else if (!running) {
                initGame();
                started = true;
            }
            return;
        }

        if (!started || !running) return;

        if (code == java.awt.event.KeyEvent.VK_UP && direction != 'D') direction = 'U';
        else if (code == java.awt.event.KeyEvent.VK_DOWN && direction != 'U') direction = 'D';
        else if (code == java.awt.event.KeyEvent.VK_LEFT && direction != 'R') direction = 'L';
        else if (code == java.awt.event.KeyEvent.VK_RIGHT && direction != 'L') direction = 'R';
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
    }

    public void keyTyped(java.awt.event.KeyEvent e) {
    }
}

class Node {
    int x;
    int y;

    Node(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
```

## 代码实现说明

这份代码的运行逻辑是：程序启动后创建 Swing 窗口和游戏面板，面板内部用 `Timer` 按固定时间间隔触发更新，每次更新会让蛇按当前方向移动，再检查是否吃到食物、是否撞墙或撞到自己，最后重绘界面。 [cloud.tencent](https://cloud.tencent.com/developer/article/2164168)
玩家输入则通过 `KeyListener` 监听方向键和空格键实现，方向键负责控制蛇头方向，空格键负责开始游戏和失败后重新开始，这也是很多 Swing 贪吃蛇案例里的标准交互方式。 [youtube](https://www.youtube.com/watch?v=KtksjrI0pnc)

你答辩时可以重点讲这几个方法：

- `initGame()`：初始化蛇、方向、分数和食物位置。
- `spawnFood()`：随机生成食物，并避免和蛇身重叠。
- `move()`：根据方向移动蛇头，判断是否吃到食物。
- `checkCollision()`：判断是否撞墙或撞到自己。
- `paintComponent()`：负责绘制网格、蛇、食物、分数和提示文字。 [blog.csdn](https://blog.csdn.net/u012970287/article/details/148202567)

## 课程设计描述

你可以直接把下面这段拿去改成自己的课程设计简介：

### 项目简介
本项目基于 Java Swing 实现了一个图形化贪吃蛇小游戏。系统通过图形界面展示游戏区域，玩家使用键盘方向键控制蛇移动，蛇在吃到随机生成的食物后长度增加并累计分数，撞墙或碰到自身后游戏结束。 [juejin](https://juejin.cn/post/7055284056577015838)

### 技术选型
项目采用 Java Swing 构建图形用户界面，使用 `JFrame` 作为主窗口、`JPanel` 作为游戏绘图区域，通过 `KeyListener` 处理键盘事件，通过 `Timer` 周期性驱动游戏状态更新与界面刷新。 [cnblogs](https://www.cnblogs.com/XXZ-JAVA/articles/15313008.html)

### 系统功能
系统实现了游戏开始、蛇移动控制、食物随机生成、吃食物增长、分数统计、碰撞检测、游戏结束提示和重新开始等功能，这些也是 Java 贪吃蛇项目的核心功能组成。 [blog.csdn](https://blog.csdn.net/m0_74549522/article/details/131187612)

### 面向对象设计
本项目采用面向对象思想进行模块划分，将窗口控制、游戏逻辑和坐标数据分离处理。主窗口类负责创建界面，游戏面板类负责核心逻辑与绘制，节点类负责保存蛇身和食物坐标信息，从而提高了程序结构清晰度和可扩展性。 [comate.baidu](https://comate.baidu.com/zh/page/uc4v7o36mvo)

### 核心难点
项目的主要难点包括蛇的连续移动控制、方向切换限制、食物随机生成、防止食物与蛇身重叠、以及撞墙和撞自身的碰撞检测处理。 [youtube](https://www.youtube.com/watch?v=KtksjrI0pnc)

## 答辩时怎么讲

你答辩别泛泛而谈，直接按这个逻辑说最稳：

1. 先说这是一个基于 Java Swing 的 GUI 游戏项目，用来练习图形界面、事件监听和面向对象设计。 [developer.aliyun](https://developer.aliyun.com/article/1649867)
2. 再说项目核心驱动方式是 `Timer` 定时刷新，定时执行移动、碰撞检测和重绘。 [cnblogs](https://www.cnblogs.com/XXZ-JAVA/articles/15313008.html)
3. 然后讲方向控制依赖 `KeyListener`，为了避免逻辑错误，不能直接朝相反方向移动，比如向左时不能立即向右。 [juejin](https://juejin.cn/post/7055284056577015838)
4. 最后讲碰撞检测包括两类：撞墙判负、蛇头碰到身体判负，吃到食物则增长身体和增加分数。 [blog.csdn](https://blog.csdn.net/u012970287/article/details/148202567)

## 你可以继续升级的点

这版已经够交作业，但如果你想把它做得更像“像样的小项目”，可以继续加这些功能： [stackoverflow](https://stackoverflow.com/questions/72422160/how-to-restart-this-java-snake-game)

- 增加暂停/继续功能。
- 增加难度选择，不同难度对应不同刷新速度。
- 增加最高分记录。
- 增加开始界面和结束界面。
- 增加障碍物模式或穿墙模式。
