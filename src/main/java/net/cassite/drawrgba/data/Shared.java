package net.cassite.drawrgba.data;

import javafx.beans.property.*;
import net.cassite.drawrgba.ui.MainCanvas;
import net.cassite.drawrgba.ui.ReferenceStage;
import net.cassite.drawrgba.ui.Tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Shared {
    public File currentFile;
    public final IntegerProperty red = new SimpleIntegerProperty(255);
    public final IntegerProperty green = new SimpleIntegerProperty(255);
    public final IntegerProperty blue = new SimpleIntegerProperty(255);
    public final IntegerProperty alpha = new SimpleIntegerProperty(255);
    public final Property<Tool> currentTool = new SimpleObjectProperty<>(Tool.MOVE);
    public final IntegerProperty radius = new SimpleIntegerProperty(10);

    public final List<ReferenceStage> subStages = new ArrayList<>();
    public final DoubleProperty mousePosX;
    public final DoubleProperty mousePosY;

    public MainCanvas mainCanvas;

    public Shared() {
        mousePosX = new SimpleDoubleProperty(0);
        mousePosY = new SimpleDoubleProperty(0);
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public Shared(Shared shared) {
        this.mousePosX = shared.mousePosX;
        this.mousePosY = shared.mousePosY;
    }

    public void pasteForSelection() {
        mainCanvas.pasteAsSelection();
    }

    public void fillColor() {
        mainCanvas.fillColor();
    }

    public BufferedImage getImage() {
        return mainCanvas.getImage();
    }

    public void openFile(File f) throws Exception {
        mainCanvas.setImage(f);
    }

    public void mainCanvasRequestFocus() {
        mainCanvas.requestFocus();
    }
}
