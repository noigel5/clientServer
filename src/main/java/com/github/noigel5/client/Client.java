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
            final Socket serverSocket = new Socket(serverHost, Integer.parseInt(port));
            out = new PrintWriter(serverSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            Envelope envelope = new Envelope();

            Thread sender = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                String line;
                String[] split;

                System.out.println("type (r) for register or (l) for login: ");
                String loginRegister = scanner.nextLine();
                while (true) {
                    if (!envelope.isWaiting()) {
                        if (!envelope.isLogedIn()) {
                            if (loginRegister.equals("r")) {
                                System.out.println("Username: ");
                                envelope.setUsername(scanner.nextLine());
                                while (true) {
                                    System.out.println("Password: ");
                                    String password1 = scanner.nextLine();
                                    System.out.println("repeat Password: ");
                                    String password2 = scanner.nextLine();
                                    if (password1.equals(password2)) {
                                        envelope.setPassword(AES.encrypt(password1, password));
                                        envelope.setWaiting(true);
                                        out.println(GSON.toJson(envelope));
                                        out.flush();
                                        break;
                                    } else {
                                        System.out.println("passwords don't match, try again.");
                                    }
                                }
                            } else if (loginRegister.equals("l")) {
                                System.out.println("Username: ");
                                envelope.setUsername(scanner.nextLine());
                                System.out.println("Password: ");
                                envelope.setPassword(AES.encrypt(scanner.nextLine(), password));
                                envelope.setWaiting(true);
                                envelope.setLogin(true);
                                out.println(GSON.toJson(envelope));
                                out.flush();
                            } else {
                                System.out.println("type (r) or (l)");
                                loginRegister = scanner.nextLine();
                            }
                        } else {
                            System.out.println("you can write now: ");
                            // Ich schreibe die nachrichen von der commandline an den server
                            while (scanner.hasNext()) {
                                line = scanner.nextLine();
                                split = line.split(" ", 3);

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
                                        envelope.setUsername(split[1]);
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
                        }
                    } else {
                        System.out.println("waiting for server response...");
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            sender.start();

            // und ich höre darauf, was der server mit schickt und drucke es auf der commandline aus
            Thread receive = new Thread(() -> {
                while (true) {
                    try {
                        Envelope inputEnvelope = GSON.fromJson(in.readLine(), Envelope.class);

                        if (inputEnvelope.isLogedIn()) {
                            switch (inputEnvelope.getCommand()) {
                                case ("/msg"), ("/all") -> {
                                    // Brieföffner ;-)
                                    String text = inputEnvelope.getText() == null ? "" : inputEnvelope.getText();
                                    text = inputEnvelope.getSenderHashCode() == 0
                                            ? text
                                            : AES.decrypt(text, password);
                                    text = text.replaceAll("[\\r\\n]+", System.lineSeparator() + "    ");
                                    text = text.replaceAll("\\\\n", System.lineSeparator() + "    ");

                                    System.out.printf("Msg from %d (%s):%n    %s%n", inputEnvelope.getSenderHashCode(), inputEnvelope.getUsername(), text);
                                }
                                case ("buffer") -> {
                                    envelope.setLogedIn(true);
                                    envelope.setWaiting(false);
                                }
                            }
                        } else {
                            envelope.setLogedIn(false);
                            envelope.setWaiting(false);
                            System.out.println(inputEnvelope.getText());
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

