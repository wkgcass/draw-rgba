package net.cassite.drawrgba.ui;

import io.vproxy.vfx.ui.layout.HPadding;
import io.vproxy.vfx.ui.layout.VPadding;
import io.vproxy.vfx.ui.stage.VStage;
import io.vproxy.vfx.util.FXUtils;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.cassite.drawrgba.data.Shared;

public class ReferenceStage extends VStage {
    private final Shared newShared;

    public ReferenceStage(Shared shared) {
        setTitle("Reference");
        getStage().setWidth(900);
        getStage().setHeight(900);

        getInitialScene().enableAutoContentWidthHeight();
        var content = getInitialScene().getContentPane();

        newShared = new Shared(shared);
        var canvas = new MainCanvas(newShared);
        FXUtils.observeWidthHeight(content, canvas.getNode(), -20, -20);

        content.getChildren().add(
            new VBox(
                new VPadding(10),
                new HBox(new HPadding(10), canvas.getNode())
            ));

        newShared.mainCanvas = canvas;
    }

    public Shared getShared() {
        return newShared;
    }

    @Override
    public void close() {
        super.close();
        newShared.subStages.remove(this);
    }
}
