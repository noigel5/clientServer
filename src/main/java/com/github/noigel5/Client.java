package com.github.noigel5;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

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

        final BufferedReader in;
        final PrintWriter out;

        try {
            final Socket clientSocket = new Socket(serverHost, 5000);
            out = new PrintWriter(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Ich schreibe die nachrichen von der commandline an den server
            Thread sender = new Thread(() -> {
            final Scanner scanner = new Scanner(System.in);
                while (scanner.hasNext()) {

                    String line = scanner.nextLine();
                    String[] split = line.split(" ", 3);

                    if (split[0].equals("/msg")) {
                        out.printf("%s %s %s%n", split[0], split[1], AES.encrypt(split[2], password));
                    } else if (split[0].equals("/all")) {
                        split = line.split(" ", 2);
                        out.printf("%s %s%n", split[0], AES.encrypt(split[1], password));
                    } else {
                        out.println(line);
                    }
                    out.flush();
                }
            });
            sender.start();

            // und ich höre darauf, was der server mit schickt und drucke es auf der commandline aus
            Thread receive = new Thread(() -> {
                try {
                    while (true) {
                        String output = in.readLine();
                        String[] split = output.split(": ", 2);

                        if (split.length == 2) {
                            System.out.printf("%s: %s%n", split[0], AES.decrypt(split[1], password));
                        } else {
                            System.out.println(output);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            receive.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

