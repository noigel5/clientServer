package com.github.noigel5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static java.lang.Integer.parseInt;

public class Server {
    class Client {
        Socket socket;
        String name;

        public Client(Socket socket) {
            this.socket = socket;
        }
    }

    ServerSocket serverSocket;
    HashMap<Integer, Client> clients = new HashMap<>();

    public static void main(String[] args) {
        new Server().doSomething();
    }

    public void doSomething() {
//        Scanner scanner = new Scanner(System.in);

        try {
            serverSocket = new ServerSocket(5000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("client " + clientSocket.hashCode() + " connected");
                clients.put(clientSocket.hashCode(), new Client(clientSocket));
                new Thread(new ClientSocketHandler(clientSocket)).start();
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

    class ClientSocketHandler implements Runnable {

        final Socket clientSocket;
        String input;
        String finalMsg;

        ClientSocketHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();
                    String[] msg = input.split(" ");
                    switch (msg[0]) {
                        case "/clients" -> {
                            for (Client client : clients.values()) {
                                PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                                if (client.socket.hashCode() == clientSocket.hashCode()) {
                                    printWriter.println(client.socket.hashCode() + "(" + client.name + "): YOU");
                                } else {
                                    printWriter.println(client.socket.hashCode() + "(" + client.name + ")");
                                }
                                printWriter.flush();
                            }
                        }
                        case "/msg" -> {
                            finalMsg = null;
                            for (int i = 2; i < msg.length; i++) {
                                if (finalMsg == null) {
                                    finalMsg = msg[2];
                                } else {
                                    finalMsg = "%s %s".formatted(finalMsg, msg[i]);
                                }
                            }
                            System.out.println(clientSocket.hashCode() + " to " + msg[1] + ": " + finalMsg);
                            Client recepient = clients.get(parseInt(msg[1]));
                            Client sender = clients.get(clientSocket.hashCode());
                            PrintWriter printWriter = new PrintWriter(recepient.socket.getOutputStream());
                            printWriter.println(clientSocket.hashCode() + "(" + sender.name + "): " + finalMsg);
                            printWriter.flush();
                        }
                        case "/all" -> {
                            finalMsg = null;
                            for (int i = 1; i < msg.length; i++) {
                                if (finalMsg == null) {
                                    finalMsg = msg[1];
                                } else {
                                    finalMsg = "%s %s".formatted(finalMsg, msg[i]);
                                }
                            }
                            System.out.println(clientSocket.hashCode() + " to all: " + finalMsg);
                            for (Client client : clients.values()) {
                                if (client.socket.hashCode() != clientSocket.hashCode()) {
                                    PrintWriter printWriter = new PrintWriter(client.socket.getOutputStream());
                                    printWriter.println(clientSocket.hashCode() + "(" + client.name + "): " + finalMsg);
                                    printWriter.flush();
                                }
                            }
                        }
                        case "/name" -> {
                            finalMsg = null;
                            for (int i = 1; i < msg.length; i++) {
                                if (finalMsg == null) {
                                    finalMsg = msg[1];
                                } else {
                                    finalMsg = "%s %s".formatted(finalMsg, msg[i]);
                                }
                            }
                            System.out.println(clientSocket.hashCode() + " set name: " + finalMsg);
                            int hash = clientSocket.hashCode();
                            Client c = clients.get(hash);
                            c.name = finalMsg;
                        }
                        default -> {
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                            printWriter.println("ERROR: choose a command");
                            printWriter.flush();
                        }
                    }
                } catch (IOException e) {
                    int clientId = this.clientSocket.hashCode();
                    clients.remove(clientId);
                    System.out.println("client " + clientId + " disconnected.");
                    return;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println(Arrays.toString(e.getStackTrace()));
                }
            }
        }
    }
}