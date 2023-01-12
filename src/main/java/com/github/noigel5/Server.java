package com.github.noigel5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    ServerSocket serverSocket;

    ArrayList<Socket> clientSockets = new ArrayList<>();

    public static void main(String[] args) {
        new Server().doSomething();
    }

    public void doSomething() {
//        Scanner scanner = new Scanner(System.in);

        try {
            serverSocket = new ServerSocket(5000);
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("client connected");
                    clientSockets.add(clientSocket);
                    new Thread(new ClientSocketHandler(clientSocket)).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // hören und bei Nachricht broadcasten
        //wenn ClientConnection eine Nachricht empfangen hat
        // --> dann
        // 1. gebe diese auf der Console aus
        // 2. loope durch den clientSockets array und schreibe die Nachricht auf jeden client kanal
    }

    class ClientSocketHandler implements Runnable {

        final Socket clientSocket;
        String msg;

        ClientSocketHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    msg = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();
                    System.out.println(msg);
                    clientSockets.forEach(socket -> {
                        try {
                            if (socket.hashCode() != clientSocket.hashCode()) {
                                PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
                                printWriter.println(clientSocket.hashCode() + ": " + msg);
                                printWriter.flush();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}