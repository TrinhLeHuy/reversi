package com.example.reversi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class RoomBotActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private Spinner difficultySpinner;
    private Button startGameButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_bot);

        usernameEditText = findViewById(R.id.username);
        difficultySpinner = findViewById(R.id.difficulty_spinner);
        startGameButton = findViewById(R.id.start_game_button);

        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String difficulty = difficultySpinner.getSelectedItem().toString(); // Lấy giá trị chuỗi từ Spinner

                if (!username.isEmpty()) {
                    Intent intent = new Intent(RoomBotActivity.this, GameWithBotActivity.class);
                    intent.putExtra("playerOneName", username);
                    intent.putExtra("playerTwoName", "Bot");
                    intent.putExtra("difficulty", difficulty); // Gửi mức độ khó dưới dạng chuỗi
                    startActivity(intent);
                } else {
                    usernameEditText.setError("Please enter your name");
                }
            }
        });
    }
}
