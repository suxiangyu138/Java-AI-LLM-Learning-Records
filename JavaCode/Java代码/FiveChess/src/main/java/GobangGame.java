import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 五子棋游戏核心类
 * 基于Swing实现图形化界面，支持人机对弈、落子校验、胜负判断
 */
public class GobangGame extends JFrame {
    // 棋盘相关常量
    private static final int BOARD_SIZE = 15; // 棋盘大小（15x15）
    private static final int CELL_SIZE = 40; // 每个格子的像素大小
    private static final int MARGIN = 30;    // 棋盘边距
    private static final int BOARD_PIXEL_SIZE = MARGIN * 2 + CELL_SIZE * (BOARD_SIZE - 1);

    // 棋子状态：0-空，1-玩家（黑棋），2-电脑（白棋）
    private int[][] chessBoard = new int[BOARD_SIZE][BOARD_SIZE];
    private boolean isGameOver = false;
    private boolean isPlayerTurn = true; // 玩家先下

    public GobangGame() {
        // 初始化窗口
        setTitle("五子棋游戏");
        setSize(BOARD_PIXEL_SIZE, BOARD_PIXEL_SIZE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 窗口居中
        setResizable(false);

        // 添加棋盘面板
        ChessBoardPanel boardPanel = new ChessBoardPanel();
        add(boardPanel);

        // 添加鼠标事件监听
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isGameOver || !isPlayerTurn) {
                    return; // 游戏结束或非玩家回合，不响应
                }

                // 计算点击位置对应的棋盘坐标
                int x = (e.getX() - MARGIN + CELL_SIZE / 2) / CELL_SIZE;
                int y = (e.getY() - MARGIN + CELL_SIZE / 2) / CELL_SIZE;

                // 校验落子位置是否合法
                if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE && chessBoard[x][y] == 0) {
                    chessBoard[x][y] = 1; // 玩家下黑棋
                    repaint();

                    // 检查玩家是否获胜
                    if (checkWin(x, y, 1)) {
                        JOptionPane.showMessageDialog(null, "恭喜你！你赢了！");
                        isGameOver = true;
                        return;
                    }

                    // 电脑落子
                    isPlayerTurn = false;
                    computerMove();
                    repaint();

                    // 检查电脑是否获胜
                    if (checkWin(computerX, computerY, 2)) {
                        JOptionPane.showMessageDialog(null, "很遗憾，电脑赢了！");
                        isGameOver = true;
                        return;
                    }
                }
            }
        });
    }

    // 电脑落子（简单随机落子，可优化为AI算法）
    private int computerX, computerY;
    private void computerMove() {
        // 随机找空位置落子
        do {
            computerX = (int) (Math.random() * BOARD_SIZE);
            computerY = (int) (Math.random() * BOARD_SIZE);
        } while (chessBoard[computerX][computerY] != 0);

        chessBoard[computerX][computerY] = 2; // 电脑下白棋
        isPlayerTurn = true; // 回合交还给玩家
    }

    /**
     * 检查是否获胜（横向、纵向、两个对角线）
     * @param x 落子x坐标
     * @param y 落子y坐标
     * @param player 玩家标识（1-玩家，2-电脑）
     * @return 是否获胜
     */
    private boolean checkWin(int x, int y, int player) {
        // 1. 横向检查
        int count = 1;
        // 向左
        for (int i = x - 1; i >= 0 && chessBoard[i][y] == player; i--) {
            count++;
        }
        // 向右
        for (int i = x + 1; i < BOARD_SIZE && chessBoard[i][y] == player; i++) {
            count++;
        }
        if (count >= 5) return true;

        // 2. 纵向检查
        count = 1;
        // 向上
        for (int i = y - 1; i >= 0 && chessBoard[x][i] == player; i--) {
            count++;
        }
        // 向下
        for (int i = y + 1; i < BOARD_SIZE && chessBoard[x][i] == player; i++) {
            count++;
        }
        if (count >= 5) return true;

        // 3. 对角线1（左上到右下）
        count = 1;
        // 左上
        for (int i = x - 1, j = y - 1; i >= 0 && j >= 0 && chessBoard[i][j] == player; i--, j--) {
            count++;
        }
        // 右下
        for (int i = x + 1, j = y + 1; i < BOARD_SIZE && j < BOARD_SIZE && chessBoard[i][j] == player; i++, j++) {
            count++;
        }
        if (count >= 5) return true;

        // 4. 对角线2（右上到左下）
        count = 1;
        // 右上
        for (int i = x + 1, j = y - 1; i < BOARD_SIZE && j >= 0 && chessBoard[i][j] == player; i++, j--) {
            count++;
        }
        // 左下
        for (int i = x - 1, j = y + 1; i >= 0 && j < BOARD_SIZE && chessBoard[i][j] == player; i--, j++) {
            count++;
        }
        return count >= 5;
    }

    /**
     * 棋盘绘制面板
     */
    private class ChessBoardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制棋盘网格
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < BOARD_SIZE; i++) {
                // 横线
                g2d.drawLine(MARGIN, MARGIN + i * CELL_SIZE,
                        MARGIN + (BOARD_SIZE - 1) * CELL_SIZE, MARGIN + i * CELL_SIZE);
                // 竖线
                g2d.drawLine(MARGIN + i * CELL_SIZE, MARGIN,
                        MARGIN + i * CELL_SIZE, MARGIN + (BOARD_SIZE - 1) * CELL_SIZE);
            }

            // 绘制棋子
            for (int x = 0; x < BOARD_SIZE; x++) {
                for (int y = 0; y < BOARD_SIZE; y++) {
                    int pixelX = MARGIN + x * CELL_SIZE;
                    int pixelY = MARGIN + y * CELL_SIZE;

                    if (chessBoard[x][y] == 1) { // 黑棋
                        g2d.setColor(Color.BLACK);
                        g2d.fillOval(pixelX - CELL_SIZE / 2, pixelY - CELL_SIZE / 2, CELL_SIZE, CELL_SIZE);
                    } else if (chessBoard[x][y] == 2) { // 白棋
                        g2d.setColor(Color.WHITE);
                        g2d.fillOval(pixelX - CELL_SIZE / 2, pixelY - CELL_SIZE / 2, CELL_SIZE, CELL_SIZE);
                        // 白棋画边框，区分黑棋
                        g2d.setColor(Color.BLACK);
                        g2d.drawOval(pixelX - CELL_SIZE / 2, pixelY - CELL_SIZE / 2, CELL_SIZE, CELL_SIZE);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        // 启动游戏（Swing需在事件调度线程中运行）
        SwingUtilities.invokeLater(() -> {
            new GobangGame().setVisible(true);
        });
    }
}