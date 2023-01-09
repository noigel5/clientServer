package com.github.noigel5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Receiver implements Runnable {
    final Socket clientSocket;
    final BufferedReader in;
    final PrintWriter out;

    String msg;

    Receiver(Socket clientSocket, BufferedReader in, PrintWriter out) {
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            msg = in.readLine();
            while (msg != null) {
                System.out.println("Server: " + msg);
                msg = in.readLine();
            }
            System.out.println("Server out of service");
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
