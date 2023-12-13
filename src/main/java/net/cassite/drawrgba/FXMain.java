package net.cassite.drawrgba;

import io.vproxy.vfx.control.globalscreen.GlobalScreenUtils;
import io.vproxy.vfx.ui.layout.VPadding;
import io.vproxy.vfx.ui.stage.VStage;
import io.vproxy.vfx.util.FXUtils;
import javafx.application.Application;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.cassite.drawrgba.data.Global;
import net.cassite.drawrgba.data.Shared;
import net.cassite.drawrgba.ui.MainCanvas;
import net.cassite.drawrgba.ui.TopHorizontalButtons;

import javax.imageio.ImageIO;

public class FXMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        var shared = new Shared();
        var stage = new VStage(primaryStage) {
            @Override
            public void close() {
                super.close();
                terminate(shared);
            }
        };
        stage.setTitle("Draw RGBA");
        stage.getStage().setWidth(900);
        stage.getStage().setHeight(900 + 10 + TopHorizontalButtons.PREF_HEIGHT);
        stage.getInitialScene().enableAutoContentWidthHeight();

        GlobalScreenUtils.enable(FXMain.class);
        Global.windowIsFocused = stage.getStage().focusedProperty();

        var content = stage.getInitialScene().getContentPane();

        var top = new TopHorizontalButtons(shared);
        FXUtils.observeWidth(content, top.getNode(), -20);
        var canvas = new MainCanvas(shared);
        FXUtils.observeWidth(content, canvas.getNode(), -20);
        FXUtils.observeHeight(content, canvas.getNode(),
            -TopHorizontalButtons.PREF_HEIGHT - 10 /*vbox spacing*/ - 20/*window margin*/);

        shared.mainCanvas = canvas;

        var layout = new VBox();
        layout.getChildren().addAll(
            top.getNode(),
            new VPadding(10),
            canvas.getNode()
        );
        layout.setLayoutX(10);
        layout.setLayoutY(10);
        content.getChildren().add(layout);

        //noinspection DataFlowIssue
        var img = ImageIO.read(FXMain.class.getResourceAsStream("/net/cassite/drawrgba/sample.png"));
        canvas.setImage(img);

        stage.show();
    }

    private void terminate(Shared shared) {
        GlobalScreenUtils.disable(FXMain.class);
        for (var s : shared.subStages) {
            s.close();
        }
    }
}
