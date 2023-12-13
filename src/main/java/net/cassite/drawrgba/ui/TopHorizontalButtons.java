package net.cassite.drawrgba.ui;

import io.vproxy.vfx.manager.font.FontManager;
import io.vproxy.vfx.ui.alert.SimpleAlert;
import io.vproxy.vfx.ui.alert.StackTraceAlert;
import io.vproxy.vfx.ui.button.FusionButton;
import io.vproxy.vfx.ui.pane.FusionPane;
import io.vproxy.vfx.ui.wrapper.FusionW;
import io.vproxy.vfx.ui.wrapper.ThemeLabel;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.cassite.drawrgba.data.Global;
import net.cassite.drawrgba.data.Shared;
import net.cassite.drawrgba.util.Utils;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Arrays;

public class TopHorizontalButtons extends FusionPane {
    public static final int PREF_HEIGHT = 120;
    private static final int ITEM_PREF_HEIGHT = (PREF_HEIGHT - FusionPane.PADDING_V * 2 - 10) / 2;

    public TopHorizontalButtons(Shared shared) {
        getNode().setPrefHeight(PREF_HEIGHT);

        var openBtn = new FusionButton("Open") {{
            FontManager.get().setFont(getTextNode());
            setPrefWidth(100);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnAction(_ -> {
                var chooser = new FileChooser();
                chooser.setTitle("Open");
                chooser.setInitialDirectory(Global.directoryOfTheLastUsedFile);
                chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("PNG", "*.png"));
                var f = chooser.showOpenDialog(null);
                if (f == null) {
                    return;
                }
                if (!f.getName().toLowerCase().endsWith(".png")) {
                    SimpleAlert.showAndWait(Alert.AlertType.WARNING, "You should open a .png file");
                    return;
                }
                try {
                    shared.openFile(f);
                } catch (Exception e) {
                    StackTraceAlert.showAndWait(e);
                    return;
                }
                Global.directoryOfTheLastUsedFile = f.getParentFile();
                Global.lastedOpenedFileName = f.getName();
            });
        }};
        var saveBtn = new FusionButton("Save") {{
            FontManager.get().setFont(getTextNode());
            setPrefWidth(100);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnAction(_ -> {
                var img = shared.getImage();
                if (img == null)
                    return;
                var chooser = new FileChooser();
                chooser.setTitle("Save");
                chooser.setInitialDirectory(Global.directoryOfTheLastUsedFile);
                chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("PNG", "*.png"));
                chooser.setInitialFileName(Global.lastedOpenedFileName);
                var f = chooser.showSaveDialog(null);
                if (f == null) {
                    return;
                }
                if (!f.getName().toLowerCase().endsWith(".png")) {
                    SimpleAlert.showAndWait(Alert.AlertType.WARNING, "You should save the file with .png extension");
                    return;
                }
                try {
                    ImageIO.write(img, "PNG", f);
                } catch (IOException e) {
                    StackTraceAlert.showAndWait(e);
                    return;
                }
                Global.directoryOfTheLastUsedFile = f.getParentFile();
            });
        }};
        var openRefBtn = new FusionButton("Open Reference") {{
            FontManager.get().setFont(getTextNode());
            setPrefWidth(180);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnAction(_ -> {
                var chooser = new FileChooser();
                chooser.setTitle("Open Reference");
                chooser.setInitialDirectory(Global.directoryOfTheLastUsedFile);
                chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("PNG", "*.png"));
                var f = chooser.showOpenDialog(null);
                if (f == null) {
                    return;
                }
                if (!f.getName().toLowerCase().endsWith(".png")) {
                    SimpleAlert.showAndWait(Alert.AlertType.WARNING, "You should open a .png file");
                    return;
                }
                var stage = new ReferenceStage(shared);
                try {
                    stage.getShared().openFile(f);
                } catch (Exception e) {
                    StackTraceAlert.showAndWait(e);
                    return;
                }
                shared.subStages.add(stage);
                stage.show();
                Global.directoryOfTheLastUsedFile = f.getParentFile();
            });
        }};
        var pasteRegionBtn = new FusionButton("Paste As Selection") {{
            FontManager.get().setFont(getTextNode());
            setPrefWidth(180);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnAction(_ -> shared.pasteForSelection());
        }};
        var toolLabel = new ThemeLabel("Tool") {{
            FontManager.get().setFont(this);
            setPrefHeight(ITEM_PREF_HEIGHT);
        }};
        var toolSelection = new ComboBox<Tool>() {{
            setPrefWidth(100);
            setItems(FXCollections.observableList(Arrays.asList(Tool.values())));
            getSelectionModel().select(shared.currentTool.getValue());

            getSelectionModel().selectedItemProperty().addListener((_, _, now) -> {
                if (now == null)
                    return;
                shared.currentTool.setValue(now);
            });
            setOnScroll(Event::consume); // forbid scrolling
            shared.currentTool.addListener((_, _, now) -> {
                if (now == null)
                    return;
                getSelectionModel().select(now);
            });
        }};
        var radiusLabel = new ThemeLabel("Radius") {{
            FontManager.get().setFont(this);
            setPrefHeight(ITEM_PREF_HEIGHT);
        }};
        var radiusInput = new TextField(String.valueOf(shared.radius.get())) {{
            FontManager.get().setFont(this);
            setPrefWidth(100);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnMouseExited(_ -> {
                var text = getText();
                if (Utils.isValidNum(text, 1, 200)) {
                    shared.radius.set(Integer.parseInt(text));
                } else {
                    setText(String.valueOf(shared.radius.get()));
                }
                shared.mainCanvasRequestFocus();
            });
            shared.radius.addListener((_, _, now) -> setText(String.valueOf(now)));
        }};

        var redLabel = new ThemeLabel("R:") {{
            FontManager.get().setFont(this);
            setPrefHeight(ITEM_PREF_HEIGHT);
        }};
        var redInput = new TextField(String.valueOf(shared.red.get())) {{
            FontManager.get().setFont(this);
            setPrefWidth(80);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnMouseExited(_ -> {
                var text = getText();
                if (Utils.isValid255(text)) {
                    shared.red.set(Integer.parseInt(text));
                } else {
                    setText(String.valueOf(shared.red.get()));
                }
                shared.mainCanvasRequestFocus();
            });
            shared.red.addListener((_, _, now) -> setText(String.valueOf(now)));
        }};
        var greenLabel = new ThemeLabel("G:") {{
            FontManager.get().setFont(this);
            setPrefHeight(ITEM_PREF_HEIGHT);
        }};
        var greenInput = new TextField(String.valueOf(shared.green.get())) {{
            FontManager.get().setFont(this);
            setPrefWidth(80);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnMouseExited(_ -> {
                var text = getText();
                if (Utils.isValid255(text)) {
                    shared.green.set(Integer.parseInt(text));
                } else {
                    setText(String.valueOf(shared.green.get()));
                }
                shared.mainCanvasRequestFocus();
            });
            shared.green.addListener((_, _, now) -> setText(String.valueOf(now)));
        }};
        var blueLabel = new ThemeLabel("B:") {{
            FontManager.get().setFont(this);
            setPrefHeight(ITEM_PREF_HEIGHT);
        }};
        var blueInput = new TextField(String.valueOf(shared.blue.get())) {{
            FontManager.get().setFont(this);
            setPrefWidth(80);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnMouseExited(_ -> {
                var text = getText();
                if (Utils.isValid255(text)) {
                    shared.blue.set(Integer.parseInt(text));
                } else {
                    setText(String.valueOf(shared.blue.get()));
                }
                shared.mainCanvasRequestFocus();
            });
            shared.blue.addListener((_, _, now) -> setText(String.valueOf(now)));
        }};
        var alphaLabel = new ThemeLabel("A:") {{
            FontManager.get().setFont(this);
            setPrefHeight(ITEM_PREF_HEIGHT);
        }};
        var alphaInput = new TextField(String.valueOf(shared.alpha.get())) {{
            FontManager.get().setFont(this);
            setPrefWidth(80);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnMouseExited(_ -> {
                var text = getText();
                if (Utils.isValid255(text)) {
                    shared.alpha.set(Integer.parseInt(text));
                } else {
                    setText(String.valueOf(shared.alpha.get()));
                }
                shared.mainCanvasRequestFocus();
            });
            shared.alpha.addListener((_, _, now) -> setText(String.valueOf(now)));
        }};
        var fillBtn = new FusionButton("Fill") {{
            FontManager.get().setFont(getTextNode());
            setPrefWidth(100);
            setPrefHeight(ITEM_PREF_HEIGHT);
            setOnAction(_ -> shared.fillColor());
        }};

        var hbox1 = new HBox(
            openBtn,
            saveBtn,
            new Separator(Orientation.VERTICAL) {{
                setOpacity(0.25);
            }},
            pasteRegionBtn,
            new Separator(Orientation.VERTICAL) {{
                setOpacity(0.25);
            }},
            toolLabel,
            new FusionW(toolSelection) {{
                FontManager.get().setFont(getLabel());
                getLabel().setAlignment(Pos.CENTER);
            }},
            new Separator(Orientation.VERTICAL) {{
                setOpacity(0.25);
            }},
            radiusLabel,
            new FusionW(radiusInput) {{
                FontManager.get().setFont(getLabel());
                getLabel().setAlignment(Pos.CENTER);
            }}
        ) {{
            setSpacing(10);
        }};
        var hbox2 = new HBox(
            openRefBtn,
            new Separator(Orientation.VERTICAL) {{
                setOpacity(0.25);
            }},
            redLabel,
            new FusionW(redInput) {{
                FontManager.get().setFont(getLabel());
            }},
            greenLabel,
            new FusionW(greenInput) {{
                FontManager.get().setFont(getLabel());
            }},
            blueLabel,
            new FusionW(blueInput) {{
                FontManager.get().setFont(getLabel());
            }},
            alphaLabel,
            new FusionW(alphaInput) {{
                FontManager.get().setFont(getLabel());
            }},
            fillBtn
        ) {{
            setSpacing(10);
        }};

        getContentPane().getChildren().add(new VBox(
            hbox1,
            hbox2
        ) {{
            setSpacing(10);
        }});
    }
}
