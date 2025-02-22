package com.example.reversi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

public class MenuActivity extends AppCompatActivity {

    private Button startGameButtonWithBot, startGameButtonOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu); // Đảm bảo layout là activity_menu

        initViews();
        setupListeners();
    }

    private void initViews() {
        startGameButtonWithBot = findViewById(R.id.startGameButtonxBot);
        startGameButtonOnline = findViewById(R.id.startGameButtOnline);
    }

    private void setupListeners() {
        startGameButtonWithBot.setOnClickListener(v -> {
            startActivity(new Intent(this, RoomBotActivity.class));
            finish(); // Đóng MenuActivity nếu không cần quay lại
        });

        startGameButtonOnline.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
