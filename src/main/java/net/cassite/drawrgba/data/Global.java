package net.cassite.drawrgba.data;

import javafx.beans.property.ReadOnlyBooleanProperty;

import java.io.File;

public class Global {
    private Global() {
    }

    public static ReadOnlyBooleanProperty windowIsFocused;
    public static File directoryOfTheLastUsedFile;
    public static String lastedOpenedFileName;
}
