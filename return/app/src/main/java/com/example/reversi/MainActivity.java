package com.example.reversi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String SERVER_IP = "192.168.31.54";
    private static final int SERVER_PORT = 5000;

    private EditText roomIdInput;
    private Button createRoomButton, joinRoomButton, startGameButton, listRoomButton;
    private TextView statusText, roomListText;
    private boolean isRoomCreator = false;

    private Thread networkThread;
    private volatile boolean stopReading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        roomIdInput = findViewById(R.id.roomIdInput);
        createRoomButton = findViewById(R.id.createRoomButton);
        joinRoomButton = findViewById(R.id.joinRoomButton);
        startGameButton = findViewById(R.id.startGameButton);
        listRoomButton = findViewById(R.id.listRoomButton);
        statusText = findViewById(R.id.statusText);
        roomListText = findViewById(R.id.roomListText);

        networkThread = new Thread(() -> {
            while (!stopReading) {
                try {
                    // Thử kết nối với server
                    SocketHandler.getInstance().connect(SERVER_IP, SERVER_PORT);
                    runOnUiThread(() -> statusText.setText("Kết nối thành công!"));

                    // Lắng nghe phản hồi từ server
                    listenServerResponse();
                } catch (IOException e) {
                    runOnUiThread(() -> statusText.setText("Mất kết nối, thử kết nối lại sau 3 giây..."));
                    // Chờ 3 giây trước khi thử kết nối lại
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        networkThread.start();


        createRoomButton.setOnClickListener(v -> {
            String roomId = roomIdInput.getText().toString().trim();
            if (!roomId.isEmpty()) {
                sendCommand("CREATE:" + roomId);
            }
        });

        joinRoomButton.setOnClickListener(v -> {
            String roomId = roomIdInput.getText().toString().trim();
            if (!roomId.isEmpty()) {
                sendCommand("JOIN:" + roomId);
            }
        });

        startGameButton.setOnClickListener(v -> sendCommand("START"));

        listRoomButton.setOnClickListener(v -> sendCommand("LIST"));
    }

    private void listenServerResponse() {
        try {
            String line;
            while (!stopReading && (line = SocketHandler.getInstance().getReader().readLine()) != null) {
                final String msg = line.trim();
                runOnUiThread(() -> handleServerMessage(msg));
            }
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Mất kết nối server!", Toast.LENGTH_SHORT).show());
        }
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith("ROOM_CREATED")) {
            String room = msg.split(":")[1];
            isRoomCreator = true;
            statusText.setText("Phòng tạo thành công: " + room);
        } else if (msg.startsWith("JOINED")) {
            String room = msg.split(":")[1];
            isRoomCreator = false;
            statusText.setText("Đã tham gia phòng: " + room);
        } else if (msg.startsWith("ASSIGN")) {
            int assignedId = Integer.parseInt(msg.split(":")[1]);
            SocketHandler.getInstance().setPlayerId(assignedId);
            statusText.setText("Bạn được gán là Player " + assignedId);
        } else if (msg.startsWith("READY")) {
            statusText.setText("Phòng đủ 2 người. " +
                    (isRoomCreator ? "Nhấn 'Start' để bắt đầu" : "Chờ chủ phòng Start"));
            if (isRoomCreator) {
                startGameButton.setVisibility(Button.VISIBLE);
            }
        } else if (msg.equals("START")) {
            stopReading = true;
            startActivity(new Intent(MainActivity.this, GameActivity.class));
        } else if (msg.startsWith("ROOM_LIST:")) {
            String list = msg.substring("ROOM_LIST:".length());
            roomListText.setText("Phòng hiện có: " + list);
        } else if (msg.startsWith("ERROR")) {
            statusText.setText(msg);
        } else {
            statusText.setText(msg);
        }
    }

    private void sendCommand(String command) {
        new Thread(() -> {
            if (SocketHandler.getInstance().getWriter() != null) {
                SocketHandler.getInstance().getWriter().println(command);
            }
        }).start();
    }
}
