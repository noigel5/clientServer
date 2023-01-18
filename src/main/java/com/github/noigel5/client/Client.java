package com.github.noigel5.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import com.github.noigel5.model.Envelope;
import com.github.noigel5.utils.AES;
import com.google.gson.Gson;

public class Client {

    public static final Gson GSON = new Gson();
    private static String password;

    public static void main(String[] args) {
        String serverHost = args.length >= 1 ? args[0] : null;
        if (serverHost == null || "".equals(serverHost)) {
            throw new IllegalArgumentException("First argument required as server host address");
        }

        String port = args.length >= 2 ? args[1] : null;
        if (port == null || "".equals(port)) {
            throw new IllegalArgumentException("Second argument required a port");
        }

        password = args.length >= 3 ? args[2] : null;
        if (password == null || "".equals(password)) {
            throw new IllegalArgumentException("third argument required as encryption password");
        }

        final BufferedReader in;
        final PrintWriter out;

        try {
            final Socket clientSocket = new Socket(serverHost, Integer.parseInt(port));
            out = new PrintWriter(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

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
                            envelope.setRecipientHashCode(Integer.parseInt(split[1]));
                            envelope.setText(AES.encrypt(split[2], password));
                            out.println(GSON.toJson(envelope));
                        }
                        case "/all" -> {
                            split = line.split(" ", 2);
                            envelope.setCommand(split[0]);
                            envelope.setText(AES.encrypt(split[1], password));
                            out.println(GSON.toJson(envelope));
                        }
                        case "/name" -> {
                            split = line.split(" ", 2);
                            envelope.setSenderName(split[1]);
                            envelope.setCommand(split[0]);
                            out.println(GSON.toJson(envelope));
                        }
                        case "/clients" -> {
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
                    try {
                        String msg = in.readLine();

//                        System.out.println("                                                             DBG: " + msg);

                        Envelope envelope = GSON.fromJson(msg, Envelope.class);

                        switch (envelope.getCommand()) {
                            case ("/msg"), ("/all") -> {
                                // Brieföffner ;-)
                                String text = envelope.getText() == null ? "" : envelope.getText();
                                text = envelope.getSenderHashCode() == 0
                                        ? text
                                        : AES.decrypt(text, password);
                                text = text.replaceAll("[\\r\\n]+", System.lineSeparator() + "    ");
                                text = text.replaceAll("\\\\n", System.lineSeparator() + "    ");

                                System.out.printf("Msg from %d (%s):%n    %s%n", envelope.getSenderHashCode(), envelope.getSenderName(), text);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            receive.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

