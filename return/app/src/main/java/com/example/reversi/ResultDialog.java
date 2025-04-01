package com.example.reversi;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import com.example.myapplication.R;

public class ResultDialog extends DialogFragment {

    private String message;

    public static ResultDialog newInstance() {
        return new ResultDialog();
    }

    public void setGameOverMessage(String message) {
        this.message = message;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(message);

        // Nếu thông báo chứa từ khóa chỉ ra đối thủ đã thoát (có thể tuỳ chỉnh theo thông điệp bạn sử dụng)
        if (message.toLowerCase().contains("đối thủ") || message.toLowerCase().contains("left") || message.toLowerCase().contains("disconnected")) {
            // Chỉ hiển thị nút Return Menu
            builder.setPositiveButton("Return Menu", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    GameActivity activity = (GameActivity) getActivity();
                    if (activity != null) {
                        activity. returnToMenuRequest();
                    }
                }
            });
        } else {
            // Hiển thị cả 2 nút Start Again và Return Menu
            builder.setPositiveButton("Start Again", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GameActivity activity = (GameActivity) getActivity();
                            if (activity != null) {
                                activity.startGameAgain();
                            }
                        }
                    })
                    .setNegativeButton("Return Menu", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GameActivity activity = (GameActivity) getActivity();
                            if (activity != null) {
                                activity.returnToMenu();
                            }
                        }
                    });
        }
        return builder.create();
    }
}
