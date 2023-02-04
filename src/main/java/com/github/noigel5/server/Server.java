package com.github.noigel5.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

@Slf4j
public class Server {
    ServerSocket serverSocket;
    HashMap<Integer, ClientRef> clients = new HashMap<>();

    public static void main(String[] args) {
        String port = args[0];
        new Server().doSomething(port);
    }

    public void doSomething(String port) {
//        Scanner scanner = new Scanner(System.in);

        try {
            serverSocket = new ServerSocket(Integer.parseInt(port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                log.debug("client %d initialized".formatted(clientSocket.hashCode()));
                clients.put(clientSocket.hashCode(), new ClientRef(clientSocket));
                new Thread(new ClientSocketHandler(this, clientSocket)).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        // hören und bei Nachricht broadcasten
        //wenn ClientConnection eine Nachricht empfangen hat
        // --> dann
        // 1. gebe diese auf der Console aus
        // 2. loope durch den clientSockets array und schreibe die Nachricht auf jeden client kanal
    }

}