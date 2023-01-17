package com.github.noigel5.server;

import com.github.noigel5.dto.Envelope;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

import static java.lang.Integer.parseInt;

class ClientSocketHandler implements Runnable {

    public static final Gson GSON = new Gson();
    private final Server server;
    final Socket clientSocket;
    String input;

    ClientSocketHandler(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();
                Envelope envelope = GSON.fromJson(input, Envelope.class);
                switch (envelope.getCommand()) {
                    case "/clients" -> {
                        for (Client ignored : server.clients.values()) {
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                            printWriter.println(envelope);
                            printWriter.flush();
                        }
                    }
                    case "/msg" -> {
                        System.out.printf("%d to %s: %s%n", clientSocket.hashCode(), envelope.getHashCode(), envelope.getText());
                        PrintWriter printWriter = new PrintWriter(server.clients.get(envelope.getHashCode()).socket.getOutputStream());
                        printWriter.printf(GSON.toJson(envelope));
                        printWriter.flush();
                    }
                    case "/all" -> {
                        System.out.printf("%d to all: %s%n", clientSocket.hashCode(), envelope.getText());
                        for (Client client : server.clients.values()) {
                            if (client.socket.hashCode() != clientSocket.hashCode()) {
                                PrintWriter printWriter = new PrintWriter(client.socket.getOutputStream());
                                printWriter.printf(GSON.toJson(envelope));
                                printWriter.flush();
                            }
                        }
                    }
                    case "/name" -> {
                        System.out.printf("%d set name to:(%s)%n", clientSocket.hashCode(), envelope.getName());
                        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                        printWriter.printf(GSON.toJson(envelope));
                        printWriter.flush();
                    }
                }
            } catch (IOException e) {
                int clientId = this.clientSocket.hashCode();
                server.clients.remove(clientId);
                System.out.println("client " + clientId + " disconnected.");
                return;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }
    }
}
