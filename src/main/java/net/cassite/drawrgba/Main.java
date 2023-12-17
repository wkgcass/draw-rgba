package net.cassite.drawrgba;

import io.vproxy.base.util.LogType;
import io.vproxy.base.util.Logger;
import io.vproxy.vfx.control.globalscreen.GlobalScreenUtils;
import javafx.application.Application;

public class Main {
    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        if (args.length >= 1 && args[0].equals("version")) {
            System.out.println(VERSION);
            return;
        }
        {
            var dllPath = "/net/cassite/drawrgba/JNativeHook_x64.dll";
            var dllStream = Main.class.getResourceAsStream(dllPath);
            if (dllStream == null) {
                Logger.error(LogType.SYS_ERROR, STR."\{dllPath} not found, program might not work");
            } else {
                GlobalScreenUtils.releaseJNativeHookNativeToLibraryPath(dllStream);
            }
        }
        Application.launch(FXMain.class);
    }
}
