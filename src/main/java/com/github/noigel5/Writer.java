package com.github.noigel5;

import java.io.PrintWriter;
import java.util.Scanner;

public class Writer implements Runnable {
    final Scanner scanner  = new Scanner(System.in);
    final PrintWriter out;

    public Writer(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void run() {
        out.println(scanner.nextLine());
        out.flush();
    }
}
