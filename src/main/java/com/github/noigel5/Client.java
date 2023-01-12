package com.github.noigel5;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        final BufferedReader in;
        final PrintWriter out;

        try {
            final Socket clientSocket = new Socket("127.0.0.1", 5000);
            out = new PrintWriter(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Ich schreibe die nachrichen von der commandline an den server
            Thread sender = new Thread(() -> {
                final Scanner scanner = new Scanner(System.in);
                while (scanner.hasNext()) {
                    out.println(scanner.nextLine());
                    out.flush();
                }
            });
            sender.start();

            // und ich höre darauf, was der server mit schickt und drucke es auf der commandline aus
            Thread receive = new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readLine();
                        System.out.println("Server: " + msg);
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

