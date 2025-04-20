package com.example.reversi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // Q-learning parameters
    private Map<String, Double> qTable = new HashMap<>();
    private static final double ALPHA = 0.1;      // learning rate
    private static final double GAMMA = 0.9;      // discount factor
    private static final double EPSILON = 0.1;    // exploration rate
    private Random random = new Random();

    // 8 hướng lật cờ
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
        gridLayout = findViewById(R.id.gridLayout);

        Intent intent = getIntent();
        playerOneName = intent.getStringExtra("playerOneName");
        difficulty = intent.getStringExtra("difficulty");

        playerOneTextView.setText(playerOneName);
        playerTwoTextView.setText("Bot (" + difficulty + ")");

        setupGameBoard();
    }

    private void setupGameBoard() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cellSize = screenWidth / BOARD_SIZE;

        gridLayout.setColumnCount(BOARD_SIZE);
        gridLayout.setRowCount(BOARD_SIZE);

        // Khởi tạo board
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = EMPTY;
                ImageView cell = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                cell.setLayoutParams(params);
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

        // Đặt quân cờ ban đầu
        board[3][3] = PLAYER_TWO;
        board[3][4] = PLAYER_ONE;
        board[4][3] = PLAYER_ONE;
        board[4][4] = PLAYER_TWO;

        updateBoardUI();
        if (currentPlayer == PLAYER_TWO) makeBotMove();
    }

    private void updateBoardUI() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == PLAYER_ONE) {
                    cells[i][j].setImageResource(R.drawable.coden);
                } else if (board[i][j] == PLAYER_TWO) {
                    cells[i][j].setImageResource(R.drawable.cotrang);
                } else {
                    cells[i][j].setImageResource(android.R.color.transparent);
                }
            }
        }
        if (currentPlayer == PLAYER_ONE) showValidMoves();
    }

    private void showValidMoves() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY && isValidMove(i, j)) {
                    cells[i][j].setImageResource(R.drawable.bantrong);
                }
            }
        }
    }

    private void sendMove(int row, int col) {
        if (currentPlayer != PLAYER_ONE) return;
        if (!isValidMove(row, col)) {
            Toast.makeText(this, "Nước đi không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        board[row][col] = PLAYER_ONE;
        flipPieces(row, col);
        currentPlayer = PLAYER_TWO;
        updateBoardUI();
        if (!hasValidMove()) {
            showGameOverDialog(getGameOverMessage());
        } else {
            makeBotMove();
        }
    }

    private boolean isValidMove(int row, int col) {
        if (board[row][col] != EMPTY) return false;
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0], c = col + dir[1];
            boolean foundOpp = false;
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == getOpponent()) {
                foundOpp = true;
                r += dir[0]; c += dir[1];
            }
            if (foundOpp && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == currentPlayer) {
                return true;
            }
        }
        return false;
    }

    private void flipPieces(int row, int col) {
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0], c = col + dir[1];
            List<int[]> toFlip = new ArrayList<>();
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == getOpponent()) {
                toFlip.add(new int[]{r, c});
                r += dir[0]; c += dir[1];
            }
            if (!toFlip.isEmpty() && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == currentPlayer) {
                for (int[] p : toFlip) board[p[0]][p[1]] = currentPlayer;
            }
        }
    }

    private boolean hasValidMove() {
        for (int i = 0; i < BOARD_SIZE; i++) for (int j = 0; j < BOARD_SIZE; j++) if (isValidMove(i, j)) return true;
        return false;
    }

    private int getOpponent() {
        return (currentPlayer == PLAYER_ONE) ? PLAYER_TWO : PLAYER_ONE;
    }

    private void makeBotMove() {
        if (currentPlayer != PLAYER_TWO) return;
        int[] bestMove;
        switch (difficulty.toLowerCase()) {
            case "easy":
                bestMove = getRandomMove(); break;
            case "medium":
                bestMove = getMaxFlipsMove(); break;
            case "ai learn":
                bestMove = getReinforcementLearningMove(); break;
            case "hard":
            default:
                bestMove = alphaBeta(3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        }
        if (bestMove != null) {
            board[bestMove[0]][bestMove[1]] = PLAYER_TWO;
            flipPieces(bestMove[0], bestMove[1]);
            currentPlayer = PLAYER_ONE;
            updateBoardUI();
            if (!hasValidMove()) showGameOverDialog(getGameOverMessage());
        }
    }

    private int[] getRandomMove() {
        List<int[]> moves = getValidMovesList();
        return moves.isEmpty() ? null : moves.get(random.nextInt(moves.size()));
    }

    private int[] getMaxFlipsMove() {
        int[] best = null; int max = -1;
        for (int[] m : getValidMovesList()) {
            int f = countFlips(m[0], m[1]);
            if (f > max) { max = f; best = m; }
        }
        return best;
    }

    private int countFlips(int row, int col) {
        int flips = 0;
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0], c = col + dir[1], cnt = 0;
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == getOpponent()) {
                cnt++; r += dir[0]; c += dir[1];
            }
            if (cnt > 0 && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == currentPlayer) flips += cnt;
        }
        return flips;
    }

    private int[] alphaBeta(int depth, int alpha, int beta, boolean maxPlayer) {
        int[] bestMove = null;
        int bestScore = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        if (depth == 0 || !hasValidMove()) return bestMove;
        for (int[] m : getValidMovesList()) {
            int[][] copy = copyBoard(board);
            board[m[0]][m[1]] = maxPlayer ? PLAYER_TWO : PLAYER_ONE;
            flipPieces(m[0], m[1]);
            int score = minimax(depth - 1, alpha, beta, !maxPlayer);
            board = copy;
            if (maxPlayer) {
                if (score > bestScore) { bestScore = score; bestMove = m; }
                alpha = Math.max(alpha, score);
            } else {
                if (score < bestScore) { bestScore = score; bestMove = m; }
                beta = Math.min(beta, score);
            }
            if (beta <= alpha) break;
        }
        return bestMove;
    }

    private int minimax(int depth, int alpha, int beta, boolean maxPlayer) {
        if (depth == 0 || !hasValidMove()) return evaluateBoard();
        int bestScore = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int[] m : getValidMovesList()) {
            int[][] copy = copyBoard(board);
            board[m[0]][m[1]] = maxPlayer ? PLAYER_TWO : PLAYER_ONE;
            flipPieces(m[0], m[1]);
            int score = minimax(depth - 1, alpha, beta, !maxPlayer);
            board = copy;
            if (maxPlayer) {
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, score);
            } else {
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, score);
            }
            if (beta <= alpha) break;
        }
        return bestScore;
    }

    private int[][] copyBoard(int[][] orig) {
        int[][] c = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) System.arraycopy(orig[i], 0, c[i], 0, BOARD_SIZE);
        return c;
    }

    private int evaluateBoard() {
        int score = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == PLAYER_TWO) score++;
                else if (board[i][j] == PLAYER_ONE) score--;
            }
        }
        return score;
    }

    // Q-learning support methods
    private String stateToString() {
        StringBuilder sb = new StringBuilder();
        for (int[] row : board) for (int v : row) sb.append(v);
        return sb.toString();
    }

    private double getQValue(String state, String action) {
        String key = state + "_" + action;
        return qTable.getOrDefault(key, 0.0);
    }

    private void setQValue(String state, String action, double val) {
        String key = state + "_" + action;
        qTable.put(key, val);
    }

    private double getMaxFutureQ(String nextState, List<int[]> moves) {
        double maxQ = Double.NEGATIVE_INFINITY;
        for (int[] m : moves) {
            double q = getQValue(nextState, m[0] + "," + m[1]);
            maxQ = Math.max(maxQ, q);
        }
        return maxQ == Double.NEGATIVE_INFINITY ? 0.0 : maxQ;
    }

    private List<int[]> getValidMovesList() {
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) for (int j = 0; j < BOARD_SIZE; j++) if (isValidMove(i, j)) list.add(new int[]{i, j});
        return list;
    }

    private double getReward(int flips) {
        return flips; // reward đơn giản theo số quân lật
    }

    private int[] getReinforcementLearningMove() {
        String state = stateToString();
        List<int[]> moves = getValidMovesList();
        if (moves.isEmpty()) return null;

        // Epsilon-greedy selection
        int[] chosen;
        if (random.nextDouble() < EPSILON) {
            chosen = moves.get(random.nextInt(moves.size()));
        } else {
            double bestQ = Double.NEGATIVE_INFINITY;
            chosen = moves.get(0);
            for (int[] m : moves) {
                double q = getQValue(state, m[0] + "," + m[1]);
                if (q > bestQ) { bestQ = q; chosen = m; }
            }
        }

        // Execute and evaluate
        int flips = countFlips(chosen[0], chosen[1]);
        board[chosen[0]][chosen[1]] = PLAYER_TWO;
        flipPieces(chosen[0], chosen[1]);
        String nextState = stateToString();

        // Q-learning update
        double reward = getReward(flips);
        double oldQ = getQValue(state, chosen[0] + "," + chosen[1]);
        double maxFutureQ = getMaxFutureQ(nextState, getValidMovesList());
        double newQ = oldQ + ALPHA * (reward + GAMMA * maxFutureQ - oldQ);
        setQValue(state, chosen[0] + "," + chosen[1], newQ);

        Toast.makeText(this, "AI chọn: RL move (Q=" + String.format("%.2f", newQ) + ")", Toast.LENGTH_SHORT).show();
        return chosen;
    }

    private void showGameOverDialog(String msg) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Kết thúc trò chơi")
                .setMessage(msg)
                .setPositiveButton("OK", (d,w) -> finish())
                .show();
    }

    private String getGameOverMessage() {
        int one=0, two=0;
        for (int i=0;i<BOARD_SIZE;i++) for (int j=0;j<BOARD_SIZE;j++) {
            if (board[i][j]==PLAYER_ONE) one++; else if (board[i][j]==PLAYER_TWO) two++;
        }
        if (one>two) return "Người chơi thắng!";
        if (two>one) return "Bot thắng!";
        return "Hòa!";
    }
}
