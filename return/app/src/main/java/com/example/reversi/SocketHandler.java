package com.example.reversi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketHandler {
    private static SocketHandler instance;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private int playerId;

    private SocketHandler() {}

    public static synchronized SocketHandler getInstance() {
        if (instance == null) {
            instance = new SocketHandler();
        }
        return instance;
    }

    public void connect(String ip, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(ip, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int id) {
        playerId = id;
    }
}
