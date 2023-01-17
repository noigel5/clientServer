package com.github.noigel5.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import com.github.noigel5.dto.Envelope;
import com.google.gson.Gson;

public class Client {

    public static final Gson GSON = new Gson();
    private static String password;

    public static void main(String[] args) {
        String serverHost = args.length >= 1 ? args[0] : null;
        if (serverHost == null || "".equals(serverHost)) {
            throw new IllegalArgumentException("First argument required as server host address");
        }

        password = args.length >= 2 ? args[1] : null;
        if (password == null || "".equals(password)) {
            throw new IllegalArgumentException("Second argument required as encryption password");
        }

        final String in;
        final PrintWriter out;

        try {
            final Socket clientSocket = new Socket(serverHost, 5000);
            out = new PrintWriter(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();

            // Ich schreibe die nachrichen von der commandline an den server
            Thread sender = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (scanner.hasNext()) {

                    String line = scanner.nextLine();
                    String[] split = line.split(" ", 3);
                    Envelope envelope = new Envelope();

                    switch (split[0]) {
                        case "/msg" -> {
                            envelope.setCommand(split[0]);
                            envelope.setHashCode(Integer.parseInt(split[1]));
                            envelope.setText(AES.encrypt(split[2], password));
                            out.println(GSON.toJson(envelope));
                        } case "/all" -> {
                            split = line.split(" ", 2);
                            envelope.setCommand(split[0]);
                            envelope.setText(AES.encrypt(split[1], password));
                            out.printf(GSON.toJson(envelope));
                        } case "/name" -> {
                            split = line.split(" ", 2);
                            envelope.setName(split[1]);
                            envelope.setCommand(split[0]);
                            out.println(GSON.toJson(envelope));
                        } case "/clients" -> {
                            envelope.setCommand(split[0]);
                            out.println(GSON.toJson(envelope));
                        }
                        default -> System.out.println("ERROR: Command must be /clients, /all, /msg, /name");
                    }
                    out.flush();
                }
            });
            sender.start();

            // und ich höre darauf, was der server mit schickt und drucke es auf der commandline aus
            Thread receive = new Thread(() -> {
                while (true) {
                    Envelope envelope = GSON.fromJson(in, Envelope.class);

                    switch (envelope.getCommand()) {
                        case ("/msg"), ("/all") -> System.out.printf("%d(%s): %s%n", envelope.getHashCode(), envelope.getName(), AES.decrypt(envelope.getText(), password));
                        case ("/name") -> System.out.printf("name set to: %s%n", envelope.getName());
                        case ("/clients") -> System.out.printf("%d(%s)".formatted(envelope.getHashCode(), envelope.getName()));
                    }
                }
            });
            receive.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

