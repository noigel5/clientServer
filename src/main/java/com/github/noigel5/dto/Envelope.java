package com.github.noigel5.dto;

import java.io.Serializable;

public class Envelope implements Serializable {
    private String command;
    private int hashCode;
    private String name;
    private String text;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getHashCode() {
        return hashCode;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }
    public void setText(String text) {
        this.text = text;
    }
}
