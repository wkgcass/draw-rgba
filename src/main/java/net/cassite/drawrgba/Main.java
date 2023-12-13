package net.cassite.drawrgba;

import javafx.application.Application;

public class Main {
    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        if (args.length >= 1 && args[0].equals("version")) {
            System.out.println(VERSION);
            return;
        }
        Application.launch(FXMain.class);
    }
}
