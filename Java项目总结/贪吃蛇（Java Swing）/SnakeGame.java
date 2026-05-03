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