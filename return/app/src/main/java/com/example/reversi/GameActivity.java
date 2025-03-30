package com.example.reversi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.myapplication.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;


public class GameActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private TextView roleText, turnText;
    private ImageView[][] cells = new ImageView[8][8];
    private int[][] board = new int[8][8];

    private int playerId = 0;      // Player 1 (Đen) hoặc Player 2 (Trắng)
    private int currentTurn = 1;   // Ai đang đi (1 hoặc 2)

    private BufferedReader reader;
    private PrintWriter writer;
    private Thread readingThread;
    private volatile boolean stopReading = false;
    private boolean inGame = false;

    // Thêm mảng hằng số các hướng lật cờ (8 hướng)
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},     // Trên, Dưới, Trái, Phải
            {-1, -1}, {1, 1}, {-1, 1}, {1, -1}    // 4 đường chéo
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gridLayout = findViewById(R.id.gridLayout);
        roleText = findViewById(R.id.roleText);
        turnText = findViewById(R.id.turnText);

        setupGameBoard();

        reader = SocketHandler.getInstance().getReader();
        writer = SocketHandler.getInstance().getWriter();

        // Lấy playerId do server gán
        playerId = SocketHandler.getInstance().getPlayerId();
        if (playerId == 1) {
            roleText.setText("Bạn là Player 1 (Đen)");
        } else if (playerId == 2) {
            roleText.setText("Bạn là Player 2 (Trắng)");
        } else {
            roleText.setText("Chưa xác định playerId!");
        }
        turnText.setText("Cờ đen đi trước, cờ đen đi trò chơi sẽ bắt đầu !");

        // Bắt đầu luồng lắng nghe server
        readingThread = new Thread(this::listenForServerUpdates);
        readingThread.start();
    }

    private void setupGameBoard() {
        // Điều chỉnh cellSize để các ô bớt thưa. Bạn có thể chia cho 8 thay vì 9
        int cellSize = getResources().getDisplayMetrics().widthPixels / 9;
        gridLayout.setColumnCount(8);
        gridLayout.setRowCount(8);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ImageView cell = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                cell.setLayoutParams(params);

                // Tô nền kiểu checkered
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

        // Đặt trạng thái khởi đầu cho 4 ô trung tâm (thường server cũng làm)
        board[3][3] = 2;
        board[3][4] = 1;
        board[4][3] = 1;
        board[4][4] = 2;

        updateBoardUI();
    }

    /**
     * Lắng nghe phản hồi từ server
     */
    private void listenForServerUpdates() {
        try {
            String line;
            while (!stopReading && (line = reader.readLine()) != null) {
                final String msg = line.trim();
                runOnUiThread(() -> handleServerMessage(msg));
            }
        } catch (IOException e) {
            runOnUiThread(() ->
                    Toast.makeText(GameActivity.this, "Mất kết nối server!", Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * Xử lý thông điệp từ server
     */
    private void handleServerMessage(String msg) {
        String[] parts = msg.split(":");
        String key = parts[0];
        switch (key) {
            case "TURN":
                if (parts.length >= 2) {
                    currentTurn = Integer.parseInt(parts[1]);
                    turnText.setText("Lượt chơi: " + (currentTurn == playerId ? "Của bạn" : "Đối thủ"));
                }
                break;
            case "BOARD_STATE":
                if (msg.contains(":")) {
                    String boardData = msg.substring(msg.indexOf(":") + 1);
                    updateBoardFromState(boardData);
                }
                break;
            case "WIN":
                String winMsg = (parts.length >= 2) ? parts[1] : "Game Over";
                showResultDialog("Kết thúc trò chơi: " + winMsg);
                break;
            case "PLAY_AGAIN_REQUEST":
                showPlayAgainDialog();
                break;
            case "NEW_GAME":
                Toast.makeText(this, "Trò chơi mới bắt đầu!", Toast.LENGTH_SHORT).show();
                break;
            case "ERROR":
                if (parts.length >= 2) {
                    Toast.makeText(this, parts[1], Toast.LENGTH_SHORT).show();
                }
                break;
            case "ASSIGN":
                if (parts.length >= 2) {
                    playerId = Integer.parseInt(parts[1]);
                    roleText.setText("Bạn là Player " + playerId + (playerId == 1 ? " (Đen)" : " (Trắng)"));
                }
                break;
            case "EXIT_TO_MENU":
                Toast.makeText(this, "Đối thủ đã từ chối chơi lại. Trở về menu...", Toast.LENGTH_SHORT).show();
                returnToMenu(); // Hàm xử lý quay về menu chính
                break;
            default:
                break;
        }
    }

    /**
     * Cập nhật board[][] từ chuỗi server gửi về
     */
    private void updateBoardFromState(String state) {
        String[] rows = state.split(";");
        for (int i = 0; i < 8 && i < rows.length; i++) {
            String[] cols = rows[i].split(",");
            for (int j = 0; j < 8 && j < cols.length; j++) {
                board[i][j] = Integer.parseInt(cols[j]);
            }
        }
        updateBoardUI();
    }

    /**
     * Cập nhật giao diện bàn cờS
     * Sau khi vẽ quân cờ xong, nếu là lượt của người chơi (playerId == currentTurn)
     * thì hiển thị các ô hợp lệ.
     */
    private void updateBoardUI() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == 1) {
                    cells[i][j].setImageResource(R.drawable.coden);
                } else if (board[i][j] == 2) {
                    cells[i][j].setImageResource(R.drawable.cotrang);
                } else {
                    cells[i][j].setImageResource(android.R.color.transparent);
                }
            }
        }

        // Nếu đến lượt mình, tô highlight nước đi hợp lệ
        if (playerId == currentTurn) {
            showValidMoves();
        }
    }

    /**
     * Gợi ý nước đi cho người chơi bằng cách highlight các ô hợp lệ
     * (ô trống và có thể lật cờ).
     */
    private void showValidMoves() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                // Nếu ô đang trống và là nước đi hợp lệ -> highlight
                if (board[i][j] == 0 && isValidMove(i, j, currentTurn)) {
                    // Bạn có thể dùng R.drawable.bantrong để đánh dấu
                    cells[i][j].setImageResource(R.drawable.bantrong);
                }
            }
        }
    }

    /**
     * Kiểm tra (row, col) có phải là nước đi hợp lệ cho player hay không
     */
    private boolean isValidMove(int row, int col, int player) {
        if (board[row][col] != 0) {
            return false;
        }
        int opponent = getOpponent(player);

        // Kiểm tra 8 hướng
        for (int[] dir : DIRECTIONS) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;

            // Duyệt theo hướng dir[] đến khi gặp quân đối thủ
            while (r >= 0 && r < 8 && c >= 0 && c < 8 && board[r][c] == opponent) {
                r += dir[0];
                c += dir[1];
                foundOpponent = true;
            }

            // Nếu có quân đối thủ và điểm dừng là quân mình => hợp lệ
            if (foundOpponent &&
                    r >= 0 && r < 8 && c >= 0 && c < 8 &&
                    board[r][c] == player) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trả về quân đối thủ
     */
    private int getOpponent(int player) {
        return (player == 1) ? 2 : 1;
    }

    /**
     * Gửi nước đi (row, col) lên server
     */
    private void sendMove(int row, int col) {
        if (playerId != currentTurn) {
            Toast.makeText(this, "Chưa tới lượt của bạn!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (board[row][col] != 0) {
            Toast.makeText(this, "Ô này đã có quân!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gửi lệnh MOVE:row:col
        new Thread(() -> {
            if (writer != null) {
                writer.println("MOVE:" + row + ":" + col);
            }
        }).start();
    }

    private void sendCommand(String cmd) {
        new Thread(() -> {
            if (writer != null) {
                writer.println(cmd);
            }
        }).start();
    }

    public void startGameAgain() {
        // Kiểm tra cờ inGame
        if (!inGame) {
            Toast.makeText(this,
                    "Không thể gửi PLAY_AGAIN. Phòng đã đóng hoặc đối thủ thoát!",
                    Toast.LENGTH_SHORT).show();

            // Trở về menu (hoặc đóng Activity)
            returnToMenu();
            return;
        }

        try {
            // Gửi lệnh PLAY_AGAIN
            sendCommand("PLAY_AGAIN");
            Toast.makeText(this, "Yêu cầu chơi lại đã được gửi", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Nếu có lỗi (VD socket đã đóng)
            Toast.makeText(this,
                    "Không thể gửi yêu cầu chơi lại. " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();

            // Trở về menu (hoặc đóng Activity)
            returnToMenu();
        }
    }


    private void showResultDialog(String message) {
        ResultDialog dialog = ResultDialog.newInstance();
        dialog.setGameOverMessage(message);
        dialog.show(getSupportFragmentManager(), "ResultDialog");
    }

    private void showPlayAgainDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chơi lại?")
                .setMessage("Đối thủ muốn chơi lại. Bạn có muốn không?")
                .setPositiveButton("Có", (dialog, which) -> sendCommand("PLAY_AGAIN"))
                .setNegativeButton("Không", (dialog, which) -> {
                    sendCommand("EXIT");
                    Intent intent = new Intent(GameActivity.this, MenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
    private void returnToMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
        finish(); // Đóng màn hình hiện tại
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopReading = true;
    }
}
