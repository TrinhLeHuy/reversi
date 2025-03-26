package com.example.reversi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Random;
import com.example.myapplication.R;

public class GameWithBotActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 8;
    private static final int EMPTY = 0;
    private static final int PLAYER_ONE = 1; // Quân đen
    private static final int PLAYER_TWO = 2; // Quân trắng (Bot)

    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private ImageView[][] cells = new ImageView[BOARD_SIZE][BOARD_SIZE];

    private GridLayout gridLayout;
    private TextView playerOneTextView;
    private TextView playerTwoTextView;
    private String playerOneName;
    private String difficulty;
    private int currentPlayer = PLAYER_ONE;

    // 8 hướng lật cờ: trên, dưới, trái, phải, 4 đường chéo
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {1, 1}, {-1, 1}, {1, -1}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_with_bot);

        playerOneTextView = findViewById(R.id.PlayerOneName);
        playerTwoTextView = findViewById(R.id.PlayerxBot);

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        playerOneName = intent.getStringExtra("playerOneName");
        difficulty = intent.getStringExtra("difficulty");

        playerOneTextView.setText(playerOneName);
        playerTwoTextView.setText("Bot (" + difficulty + ")");

        gridLayout = findViewById(R.id.gridLayout);

        // Khởi tạo bàn cờ
        setupGameBoard();
    }

    /**
     * Tạo bàn cờ bằng cách thêm các ô (ImageView) vào GridLayout
     */
    private void setupGameBoard() {
        // Tính toán kích thước mỗi ô (chia cho 8 để sát nhau hơn)
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cellSize = screenWidth / BOARD_SIZE;

        // Thiết lập số cột, số hàng cho GridLayout
        gridLayout.setColumnCount(BOARD_SIZE);
        gridLayout.setRowCount(BOARD_SIZE);

        // Khởi tạo mảng board
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }

        // Tạo ô cờ
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                ImageView cell = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                cell.setLayoutParams(params);

                // Tô nền checkered
                if ((i + j) % 2 == 0) {
                    cell.setBackgroundResource(R.drawable.banco1);
                } else {
                    cell.setBackgroundResource(R.drawable.banco2);
                }

                final int row = i, col = j;
                cell.setOnClickListener(v -> sendMove(row, col));
                cells[i][j] = cell;
                gridLayout.addView(cell);
            }
        }

        // Đặt 4 quân cờ ở trung tâm (theo luật Reversi)
        board[3][3] = PLAYER_TWO;
        board[3][4] = PLAYER_ONE;
        board[4][3] = PLAYER_ONE;
        board[4][4] = PLAYER_TWO;

        // Cập nhật giao diện lần đầu
        updateBoardUI();

        // Nếu lượt đầu là Bot
        if (currentPlayer == PLAYER_TWO) {
            makeBotMove();
        }
    }

    /**
     * Cập nhật hiển thị cho toàn bộ bàn cờ.
     * Nếu là lượt người chơi (PLAYER_ONE) thì hiển thị gợi ý nước đi.
     */
    private void updateBoardUI() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == PLAYER_ONE) {
                    cells[i][j].setImageResource(R.drawable.coden);
                } else if (board[i][j] == PLAYER_TWO) {
                    cells[i][j].setImageResource(R.drawable.cotrang);
                } else {
                    // Ô trống, xóa hình (giữ nền checkered)
                    cells[i][j].setImageResource(android.R.color.transparent);
                }
            }
        }
        // Hiển thị nước đi hợp lệ nếu là lượt người chơi
        if (currentPlayer == PLAYER_ONE) {
            showValidMoves();
        }
    }

    /**
     * Hiển thị gợi ý nước đi (highlight) cho những ô trống hợp lệ
     */
    private void showValidMoves() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY && isValidMove(i, j)) {
                    // Đặt ảnh highlight, ví dụ R.drawable.bantrong
                    cells[i][j].setImageResource(R.drawable.bantrong);
                }
            }
        }
    }

    /**
     * Xử lý khi người chơi nhấn vào ô (row, col)
     */
    private void sendMove(int row, int col) {
        if (currentPlayer == PLAYER_ONE) {
            if (isValidMove(row, col)) {
                board[row][col] = currentPlayer;
                flipPieces(row, col);
                currentPlayer = PLAYER_TWO;
                updateBoardUI();

                // Kiểm tra còn nước đi không
                if (!hasValidMove()) {
                    showGameOverDialog(getGameOverMessage());
                } else {
                    // Đến lượt Bot
                    makeBotMove();
                }
            } else {
                Toast.makeText(this, "Nước đi không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Kiểm tra nước đi (row, col) có hợp lệ hay không
     */
    private boolean isValidMove(int row, int col) {
        if (board[row][col] != EMPTY) {
            return false;
        }
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;
            // Dò theo hướng dir[] đến khi gặp quân đối thủ
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == getOpponent()) {
                r += dir[0];
                c += dir[1];
                foundOpponent = true;
            }
            // Nếu đã gặp quân đối thủ, và cuối chuỗi là quân mình -> nước đi hợp lệ
            if (foundOpponent && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == currentPlayer) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lật các quân cờ theo luật
     */
    private void flipPieces(int row, int col) {
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;
            ArrayList<int[]> toFlip = new ArrayList<>();

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == getOpponent()) {
                toFlip.add(new int[]{r, c});
                r += dir[0];
                c += dir[1];
                foundOpponent = true;
            }

            // Nếu có quân đối thủ và cuối cùng gặp quân mình -> lật tất cả
            if (foundOpponent && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == currentPlayer) {
                for (int[] pos : toFlip) {
                    board[pos[0]][pos[1]] = currentPlayer;
                }
            }
        }
    }

    /**
     * Kiểm tra xem còn nước đi hợp lệ nào không
     */
    private boolean hasValidMove() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Trả về quân cờ của đối thủ
     */
    private int getOpponent() {
        return (currentPlayer == PLAYER_ONE) ? PLAYER_TWO : PLAYER_ONE;
    }

    /**
     * Xử lý lượt đi của Bot, bao gồm cả các chế độ: easy, medium, ai learn, hard
     */
    private void makeBotMove() {
        if (currentPlayer == PLAYER_TWO) {
            int[] bestMove;

            switch (difficulty.toLowerCase()) {
                case "easy":
                    bestMove = getRandomMove();
                    break;
                case "medium":
                    bestMove = getMaxFlipsMove();
                    break;
                case "ai learn":
                    bestMove = getReinforcementLearningMove();
                    break;
                case "hard":
                default:
                    bestMove = alphaBeta(3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
                    break;
            }

            if (bestMove != null) {
                int row = bestMove[0];
                int col = bestMove[1];
                board[row][col] = currentPlayer;
                flipPieces(row, col);
                currentPlayer = PLAYER_ONE;
                updateBoardUI();

                if (!hasValidMove()) {
                    showGameOverDialog(getGameOverMessage());
                }
            }
        }
    }

    /**
     * Bot - random move (dễ)
     */
    private int[] getRandomMove() {
        ArrayList<int[]> validMoves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j)) {
                    validMoves.add(new int[]{i, j});
                }
            }
        }
        if (!validMoves.isEmpty()) {
            return validMoves.get(new Random().nextInt(validMoves.size()));
        }
        return null;
    }

    /**
     * Bot - chọn nước đi lật được nhiều quân nhất (trung bình)
     */
    private int[] getMaxFlipsMove() {
        int[] bestMove = null;
        int maxFlips = -1;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j)) {
                    int flips = countFlips(i, j);
                    if (flips > maxFlips) {
                        maxFlips = flips;
                        bestMove = new int[]{i, j};
                    }
                }
            }
        }
        return bestMove;
    }

    /**
     * Đếm số quân sẽ bị lật nếu đi tại (row, col)
     */
    private int countFlips(int row, int col) {
        int flips = 0;
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;
            int count = 0;

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == getOpponent()) {
                r += dir[0];
                c += dir[1];
                foundOpponent = true;
                count++;
            }

            if (foundOpponent && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == currentPlayer) {
                flips += count;
            }
        }
        return flips;
    }

    /**
     * Bot - sử dụng Alpha-Beta (chế độ hard)
     */
    private int[] alphaBeta(int depth, int alpha, int beta, boolean maximizingPlayer) {
        int[] bestMove = null;
        int bestScore = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        // Nếu độ sâu = 0 hoặc không còn nước đi, trả về giá trị tạm
        if (depth == 0 || !hasValidMove()) {
            return new int[]{0, 0};
        }

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j)) {
                    int[][] boardCopy = copyBoard(board);
                    board[i][j] = maximizingPlayer ? PLAYER_TWO : PLAYER_ONE;
                    flipPieces(i, j);

                    int score = minimax(depth - 1, alpha, beta, !maximizingPlayer);
                    board = boardCopy;

                    if (maximizingPlayer) {
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = new int[]{i, j};
                        }
                        alpha = Math.max(alpha, score);
                    } else {
                        if (score < bestScore) {
                            bestScore = score;
                            bestMove = new int[]{i, j};
                        }
                        beta = Math.min(beta, score);
                    }

                    if (beta <= alpha) {
                        break;
                    }
                }
            }
        }
        return bestMove;
    }

    private int minimax(int depth, int alpha, int beta, boolean maximizingPlayer) {
        int bestScore = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        if (depth == 0 || !hasValidMove()) {
            return evaluateBoard();
        }

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j)) {
                    int[][] boardCopy = copyBoard(board);
                    board[i][j] = maximizingPlayer ? PLAYER_TWO : PLAYER_ONE;
                    flipPieces(i, j);

                    int score = minimax(depth - 1, alpha, beta, !maximizingPlayer);
                    board = boardCopy;

                    if (maximizingPlayer) {
                        bestScore = Math.max(bestScore, score);
                        alpha = Math.max(alpha, score);
                    } else {
                        bestScore = Math.min(bestScore, score);
                        beta = Math.min(beta, score);
                    }
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
        }
        return bestScore;
    }

    /**
     * Sao chép mảng board
     */
    private int[][] copyBoard(int[][] original) {
        int[][] copy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, BOARD_SIZE);
        }
        return copy;
    }

    /**
     * Hàm đánh giá (score) = số quân BOT - số quân PLAYER
     */
    private int evaluateBoard() {
        int playerOneScore = 0, playerTwoScore = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == PLAYER_ONE) {
                    playerOneScore++;
                } else if (board[i][j] == PLAYER_TWO) {
                    playerTwoScore++;
                }
            }
        }
        return playerTwoScore - playerOneScore;
    }

    /**
     * Phương thức AI Learn sử dụng Reinforcement Learning (placeholder)
     * Ở đây ta mô phỏng bằng cách kết hợp chiến thuật lật nhiều quân nhất và Alpha-Beta.
     */
    private int[] getReinforcementLearningMove() {
        // Giả sử ta lấy nước đi theo chiến thuật "lật nhiều quân nhất"
        int[] maxFlipsMove = getMaxFlipsMove();
        // Lấy nước đi theo chiến thuật Alpha-Beta với độ sâu 3
        int[] alphaBetaMove = alphaBeta(3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);

        // Tính điểm cho mỗi nước đi (ở đây điểm được giả định là số quân lật được)
        int flipsMax = (maxFlipsMove != null) ? countFlips(maxFlipsMove[0], maxFlipsMove[1]) : -1;
        int flipsAlpha = (alphaBetaMove != null) ? countFlips(alphaBetaMove[0], alphaBetaMove[1]) : -1;

        // Giả lập quá trình "học" bằng cách so sánh và chọn nước đi cho kết quả tốt nhất
        if (flipsMax >= flipsAlpha) {
            return maxFlipsMove;
        } else {
            return alphaBetaMove;
        }
    }

    /**
     * Hiển thị dialog kết thúc trò chơi
     */
    private void showGameOverDialog(String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Kết thúc trò chơi")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    /**
     * Xác định kết quả cuối cùng
     */
    private String getGameOverMessage() {
        int playerOneScore = 0, playerTwoScore = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == PLAYER_ONE) {
                    playerOneScore++;
                } else if (board[i][j] == PLAYER_TWO) {
                    playerTwoScore++;
                }
            }
        }
        if (playerOneScore > playerTwoScore) {
            return "Người chơi thắng!";
        } else if (playerOneScore < playerTwoScore) {
            return "Bot thắng!";
        } else {
            return "Hòa!";
        }
    }
}
