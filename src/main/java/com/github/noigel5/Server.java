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
    static class Client {
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

        ClientSocketHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();
                    String[] msg = input.split(" ", 3);
                    switch (msg[0]) {
                        case "/clients" -> {
                            for (Client client : clients.values()) {
                                PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                                if (client.socket.hashCode() == clientSocket.hashCode()) {
                                    printWriter.printf("%d(%s):(YOU)%n", client.socket.hashCode(), client.name);
                                } else {
                                    printWriter.printf("%d(%s)%n", client.socket.hashCode(), client.name);
                                }
                                printWriter.flush();
                            }
                        }
                        case "/msg" -> {
                            System.out.printf("%d to %s: %s%n", clientSocket.hashCode(), msg[1], msg[2]);
                            Client sender = clients.get(clientSocket.hashCode());
                            PrintWriter printWriter = new PrintWriter(clients.get(parseInt(msg[1])).socket.getOutputStream());
                            printWriter.printf("%d(%s): %s%n", clientSocket.hashCode(), sender.name, msg[2]);
                            printWriter.flush();
                        }
                        case "/all" -> {
                            System.out.printf("%d to all: %s%n", clientSocket.hashCode(), msg[1]);
                            for (Client client : clients.values()) {
                                if (client.socket.hashCode() != clientSocket.hashCode()) {
                                    PrintWriter printWriter = new PrintWriter(client.socket.getOutputStream());
                                    printWriter.printf("%d(%s): %s%n", clientSocket.hashCode(), client.name, msg[1]);
                                    printWriter.flush();
                                }
                            }
                        }
                        case "/name" -> {
                            System.out.printf("%d set name to:(%s%n)", clientSocket.hashCode(), msg[1]);
                            clients.get(clientSocket.hashCode()).name = msg[1];
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                            printWriter.printf("name set to:(%s)%n", msg[1]);
                            printWriter.flush();
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