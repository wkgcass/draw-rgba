package net.cassite.drawrgba.ui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import io.vproxy.base.util.Logger;
import io.vproxy.base.util.callback.Callback;
import io.vproxy.vfx.animation.AnimationGraph;
import io.vproxy.vfx.animation.AnimationGraphBuilder;
import io.vproxy.vfx.animation.AnimationNode;
import io.vproxy.vfx.control.drag.DragHandler;
import io.vproxy.vfx.ui.alert.SimpleAlert;
import io.vproxy.vfx.ui.alert.StackTraceAlert;
import io.vproxy.vfx.ui.pane.FusionPane;
import io.vproxy.vfx.util.FXUtils;
import io.vproxy.vfx.util.algebradata.DoubleData;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import net.cassite.drawrgba.data.Global;
import net.cassite.drawrgba.data.Shared;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public class MainCanvas extends FusionPane {
    private final Shared shared;

    private final Group transformGroup = new Group();
    private final Circle cursor = new Circle();
    private final Rectangle removeRect = new Rectangle();
    private final Scale removeRectScale = new Scale(1, 1);
    private final Group canvasGroup = new Group();
    private final Group imageGroup = new Group();
    private final Scale scale = new Scale(1, 1, 0, 0);

    private int width;
    private int height;
    private Arena arena;
    private MemorySegment seg;
    private PixelBuffer<ByteBuffer> pixelBuffer;// [x + y*w : x + y*w + 3] = [b, g, r, a];
    private Canvas canvas;

    @SuppressWarnings("FieldCanBeLocal") private final ChangeListener<Number> __mousePosX;
    @SuppressWarnings("FieldCanBeLocal") private final ChangeListener<Number> __mousePosY;

    public MainCanvas(Shared shared) {
        this.shared = shared;

        cursor.setRadius(shared.radius.get());
        cursor.setVisible(true);
        cursor.setStroke(Color.WHITE);
        cursor.setStrokeWidth(2);
        cursor.setFill(new Color(1, 1, 1, 0.6));

        removeRect.setVisible(false);
        removeRect.setStroke(Color.WHITE);
        removeRect.setStrokeWidth(3);
        removeRect.setFill(new Color(1, 1, 1, 0.6));
        removeRect.getTransforms().add(removeRectScale);

        var canvasBlendColor = new ColorAdjust();
        canvasGroup.setEffect(canvasBlendColor);
        canvasGroup.setOpacity(0.8);
        var canvasGroupOpacityAnimationNode1 = new AnimationNode<>("node1", new DoubleData(0.3));
        var canvasGroupOpacityAnimationNode2 = new AnimationNode<>("node2", new DoubleData(0.5));
        var canvasGroupOpacityAnimation = AnimationGraphBuilder.simpleTwoNodeGraph(
                canvasGroupOpacityAnimationNode1,
                canvasGroupOpacityAnimationNode2,
                4_000)
            .setApply((_, _, data) -> {
                canvasBlendColor.setHue(data.value);
                canvasBlendColor.setSaturation(1);
            })
            .build(canvasGroupOpacityAnimationNode1);
        playCanvasOpacityAnimation(canvasGroupOpacityAnimation, canvasGroupOpacityAnimationNode1, canvasGroupOpacityAnimationNode2);

        transformGroup.getChildren().addAll(imageGroup, canvasGroup, cursor, removeRect);
        transformGroup.getTransforms().add(scale);
        getContentPane().getChildren().add(transformGroup);

        shared.radius.addListener((_, _, now) -> {
            if (now == null)
                return;
            cursor.setRadius(now.doubleValue());
        });
        shared.currentTool.addListener((_, old, now) -> {
            if (now == null)
                return;
            cursor.setVisible(now == Tool.MOVE || now == Tool.SELECT);
            if ((old == Tool.SELECT || old == Tool.REMOVE) && now != Tool.SELECT && now != Tool.REMOVE) {
                if (canvas != null) {
                    canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                }
            }
        });
        __mousePosX = (_, _, now) -> {
            if (now == null)
                return;
            cursor.setLayoutX(now.doubleValue());
        };
        shared.mousePosX.addListener(new WeakChangeListener<>(__mousePosX));
        __mousePosY = (_, _, now) -> {
            if (now == null)
                return;
            cursor.setLayoutY(now.doubleValue());
        };
        shared.mousePosY.addListener(new WeakChangeListener<>(__mousePosY));

        initCursor();
        initKeyboardEvents();
        initMoveAndDraw();
        initScale();
        initPick();
    }

    private void playCanvasOpacityAnimation(AnimationGraph<DoubleData> animation,
                                            AnimationNode<DoubleData> node1,
                                            AnimationNode<DoubleData> node2) {
        animation.play(node2, new Callback<>() {
            @Override
            protected void onSucceeded(Void unused) {
            }

            @Override
            protected void onFailed(Exception e) {
                Logger.shouldNotHappen("canvas opacity animation failed", e);
            }

            @Override
            protected void doFinally() {
                Platform.runLater(() ->
                    playCanvasOpacityAnimation(animation, node2, node1));
            }
        });
    }

    private void initCursor() {
        setCursor();
        shared.currentTool.addListener(_ -> setCursor());
    }

    private void setCursor() {
        if (isMove) {
            getNode().setCursor(Cursor.MOVE);
            return;
        }
        var mode = shared.currentTool.getValue();
        var cursor = switch (mode) {
            case MOVE -> Cursor.MOVE;
            case SELECT -> Cursor.NONE;
            case REMOVE -> Cursor.SE_RESIZE;
            case null -> Cursor.DEFAULT;
        };
        getNode().setCursor(cursor);
    }

    private void initKeyboardEvents() {
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (!Global.windowIsFocused.get()) {
                    return;
                }
                if (e.getKeyCode() == NativeKeyEvent.VC_SPACE) {
                    enterTemporaryMoveMode();
                } else if (e.getKeyCode() == NativeKeyEvent.VC_OPEN_BRACKET) {
                    FXUtils.runOnFX(() -> decreaseRadius());
                } else if (e.getKeyCode() == NativeKeyEvent.VC_CLOSE_BRACKET) {
                    FXUtils.runOnFX(() -> increaseRadius());
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_SPACE) {
                    leaveTemporaryMoveMode();
                }
            }
        });
    }

    private boolean isMove = false;

    private void enterTemporaryMoveMode() {
        isMove = true;
        setCursor();
    }

    private void leaveTemporaryMoveMode() {
        isMove = false;
        setCursor();
    }

    private static final double radiusIncDecRatio = 1.2;

    private void increaseRadius() {
        var old = shared.radius.get();
        var v = (int) (old * radiusIncDecRatio);
        if (v == old) {
            v += 1;
        }
        if (v >= 200) {
            v = 200;
        }
        shared.radius.set(v);
    }

    private void decreaseRadius() {
        var v = (int) (shared.radius.get() / radiusIncDecRatio);
        if (v <= 1) {
            v = 1;
        }
        shared.radius.set(v);
    }

    private void initMoveAndDraw() {
        var moveHelper = new DragHandler() {
            @Override
            protected void set(double x, double y) {
                transformGroup.setLayoutX(x);
                transformGroup.setLayoutY(y);
            }

            @Override
            protected double[] get() {
                return new double[]{
                    transformGroup.getLayoutX(),
                    transformGroup.getLayoutY()
                };
            }
        };
        transformGroup.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            moveHelper.handle(e);
            if (isMove) {
                return;
            }
            if (shared.currentTool.getValue() == Tool.SELECT) {
                draw(e.getX(), e.getY());
            } else if (shared.currentTool.getValue() == Tool.REMOVE) {
                removeBegin(e.getX(), e.getY());
            }
        });
        transformGroup.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (isMove) {
                return;
            }
            if (shared.currentTool.getValue() == Tool.SELECT) {
                drawEnd(e.getX(), e.getY());
            } else {
                removeEnd(e.getX(), e.getY());
            }
        });
        transformGroup.setOnMouseDragged(e -> {
            shared.mousePosX.set(e.getX());
            shared.mousePosY.set(e.getY());
            if (!e.isPrimaryButtonDown()) {
                return;
            }
            if (shared.currentTool.getValue() == Tool.MOVE || isMove) {
                moveHelper.handle(e);
            } else if (shared.currentTool.getValue() == Tool.SELECT) {
                draw(e.getX(), e.getY());
            } else if (shared.currentTool.getValue() == Tool.REMOVE) {
                removeUpdate(e.getX(), e.getY());
            }
        });
        transformGroup.setOnMouseMoved(e -> {
            shared.mousePosX.set(e.getX());
            shared.mousePosY.set(e.getY());
        });
    }

    private void initScale() {
        transformGroup.setOnScroll(e -> {
            double zoomFactor = 0.1;
            if (e.getDeltaY() < 0) {
                zoomFactor = -zoomFactor;
            }
            double oldRatio = scale.getX();
            double ratio = oldRatio + zoomFactor;
            if (ratio < 0.1) {
                ratio = 0.1;
            } else if (ratio > 10) {
                ratio = 10;
            }
            scale.setX(ratio);
            scale.setY(ratio);

            var imageX = e.getX();
            var imageY = e.getY();
            var realOldX = imageX * oldRatio;
            var realOldY = imageY * oldRatio;
            var realNewX = imageX * ratio;
            var realNewY = imageY * ratio;

            transformGroup.setLayoutX(transformGroup.getLayoutX() + realOldX - realNewX);
            transformGroup.setLayoutY(transformGroup.getLayoutY() + realOldY - realNewY);
        });
    }

    private void initPick() {
        transformGroup.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.SECONDARY)
                return;

            var x = (int) e.getX();
            var y = (int) e.getY();
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return;
            }
            var offset = (x + y * width) * 4;
            var b = seg.get(ValueLayout.JAVA_BYTE, offset) & 0xff;
            var g = seg.get(ValueLayout.JAVA_BYTE, offset + 1) & 0xff;
            var r = seg.get(ValueLayout.JAVA_BYTE, offset + 2) & 0xff;
            var a = seg.get(ValueLayout.JAVA_BYTE, offset + 3) & 0xff;

            shared.red.set(r);
            shared.green.set(g);
            shared.blue.set(b);
            shared.alpha.set(a);
        });
    }

    private double drawLastX = -1;
    private double drawLastY = -1;
    private static final int selectColorA = 255;
    private static final int selectColorR = 255;
    private static final int selectColorG = 255;
    private static final int selectColorB = 255;
    private static final int selectColorARGB =
        (selectColorA << 24) |
        (selectColorR << 16) |
        (selectColorG << 8) |
        (selectColorB);
    private static final Color selectColor = new Color(selectColorR / 255d, selectColorG / 255d, selectColorB / 255d, selectColorA / 255d);
    private static double lastTrigger = -1;

    private void draw(double x, double y) {
        if (x < 0 || y < 0) {
            return;
        }
        if (canvas == null) {
            return;
        }
        if (System.currentTimeMillis() - lastTrigger < 50) {
            return;
        }
        lastTrigger = System.currentTimeMillis();

        var g2d = canvas.getGraphicsContext2D();
        g2d.setFill(selectColor);
        var radius = shared.radius.get();

        if (drawLastX < 0 || drawLastY < 0) {
            drawLastX = x;
            drawLastY = y;

            g2d.setLineWidth(0);
            g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            return;
        }

        drawLine(x, y, g2d, radius);

        drawLastX = x;
        drawLastY = y;
    }

    private void drawEnd(double x, double y) {
        if (canvas != null) {
            var g2d = canvas.getGraphicsContext2D();
            var radius = shared.radius.get();

            drawLine(x, y, g2d, radius);
        }

        drawLastX = -1;
        drawLastY = -1;
    }

    private void drawLine(double x, double y, GraphicsContext g2d, int radius) {
        if (drawLastX != x || drawLastY != y) {
            g2d.setLineWidth(0);
            var angle = Math.atan2(y - drawLastY, x - drawLastX);
            var degree = angle * 180 / Math.PI;
            var len = Math.sqrt((x - drawLastX) * (x - drawLastX) + (y - drawLastY) * (y - drawLastY));
            g2d.save();
            g2d.translate(drawLastX, drawLastY);
            g2d.rotate(degree);
            g2d.fillRect(0, -radius, len, radius * 2);
            g2d.restore();
        }

        g2d.setLineWidth(0);
        g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    private double removeBeginX;
    private double removeBeginY;

    private void removeBegin(double x, double y) {
        removeBeginX = x;
        removeBeginY = y;
        removeRect.setVisible(true);
        removeRect.setLayoutX(x);
        removeRect.setLayoutY(y);
        removeRect.setWidth(0);
        removeRect.setHeight(0);
    }

    private void removeUpdate(double ex, double ey) {
        var bx = removeBeginX;
        var by = removeBeginY;
        removeRectScale.setX(bx > ex ? -1 : 1);
        removeRectScale.setY(by > ey ? -1 : 1);
        removeRect.setWidth(Math.abs(ex - bx));
        removeRect.setHeight(Math.abs(ey - by));
    }

    private void removeEnd(double ex, double ey) {
        if (canvas == null) {
            return;
        }

        var bx = removeBeginX;
        var by = removeBeginY;
        if (bx > ex) {
            var foo = ex;
            ex = bx;
            bx = foo;
        }
        if (by > ey) {
            var foo = ey;
            ey = by;
            by = foo;
        }
        canvas.getGraphicsContext2D().clearRect(bx, by, ex - bx, ey - by);

        removeRect.setVisible(false);
    }

    public void setImage(File f) throws Exception {
        var image = ImageIO.read(f);
        setImage(image);
    }

    public void setImage(java.awt.image.BufferedImage awtImage) {
        if (arena != null) {
            seg = null;
            pixelBuffer = null;
            imageGroup.getChildren().clear();
            final var arena = this.arena;
            Platform.runLater(arena::close);
        }

        var w = awtImage.getWidth();
        var h = awtImage.getHeight();
        width = w;
        height = h;
        arena = Arena.ofConfined();
        seg = arena.allocate(w * h * 4L);
        var buf = seg.asByteBuffer();
        pixelBuffer = new PixelBuffer<>(w, h, buf, PixelFormat.getByteBgraPreInstance());

        for (var y = 0; y < h; ++y) {
            for (var x = 0; x < w; ++x) {
                var pixel = awtImage.getRGB(x, y);
                byte alpha = (byte) ((pixel >> 24) & 0xff);
                byte red = (byte) ((pixel >> 16) & 0xff);
                byte green = (byte) ((pixel >> 8) & 0xff);
                byte blue = (byte) (pixel & 0xff);
                setPixel(x, y, red, green, blue, alpha);
            }
        }
        updatePixelBuffer();

        var image = new WritableImage(pixelBuffer);
        imageGroup.getChildren().clear();
        imageGroup.getChildren().add(new ImageView(image) {{
            setPickOnBounds(true);
        }});

        if (canvas == null || canvas.getWidth() < width || canvas.getHeight() < height) {
            canvas = new Canvas(width, height);
            canvas.getGraphicsContext2D().setLineWidth(0);
        }
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvasGroup.getChildren().clear();
        canvasGroup.getChildren().add(canvas);
    }

    private void setPixel(int x, int y, int red, int green, int blue, int alpha) {
        var offset = (x + y * width) * 4;
        seg.set(ValueLayout.JAVA_BYTE, offset, (byte) blue);
        seg.set(ValueLayout.JAVA_BYTE, offset + 1, (byte) green);
        seg.set(ValueLayout.JAVA_BYTE, offset + 2, (byte) red);
        seg.set(ValueLayout.JAVA_BYTE, offset + 3, (byte) alpha);
        pixelBuffer.updateBuffer(_ -> null);
    }

    private int getPixel(int x, int y) {
        var offset = (x + y * width) * 4;
        var blue = seg.get(ValueLayout.JAVA_BYTE, offset) & 0xff;
        var green = seg.get(ValueLayout.JAVA_BYTE, offset + 1) & 0xff;
        var red = seg.get(ValueLayout.JAVA_BYTE, offset + 2) & 0xff;
        var alpha = seg.get(ValueLayout.JAVA_BYTE, offset + 3) & 0xff;
        return (alpha << 24) | (red << 16) | (green << 8) | (blue);
    }

    private void updatePixelBuffer() {
        if (pixelBuffer != null)
            pixelBuffer.updateBuffer(_ -> null);
    }

    public void pasteAsSelection() {
        var t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (t == null || !t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return;
        }
        BufferedImage img;
        try {
            var data = t.getTransferData(DataFlavor.imageFlavor);
            Logger.alert(STR."retrieved data is \{data}@\{data.getClass()}");
            if (data instanceof MultiResolutionImage mri) {
                var variants = mri.getResolutionVariants();
                if (variants.isEmpty()) {
                    SimpleAlert.showAndWait(Alert.AlertType.WARNING, "no multi-resolution variants available");
                    return;
                }
                Logger.alert(STR."variants: \{variants}");
                img = (BufferedImage) mri.getResolutionVariants().getFirst();
            } else if (data instanceof BufferedImage bi) {
                img = bi;
            } else {
                SimpleAlert.showAndWait(Alert.AlertType.WARNING, STR."the image type is not supported: \{data.getClass()}");
                return;
            }
        } catch (Exception e) {
            StackTraceAlert.showAndWait(e);
            return;
        }

        var w = img.getWidth();
        var h = img.getHeight();
        Logger.alert(STR."clipboard image info: w=\{w}, h=\{h}");

        var writer = canvas.getGraphicsContext2D().getPixelWriter();
        boolean hasValidPixel = false;
        for (var x = 0; x < w && x < width; ++x) {
            for (var y = 0; y < h && y < height; ++y) {
                var argb = img.getRGB(x, y);
                if (!pixelCanBeUsed(argb)) {
                    continue;
                }
                hasValidPixel = true;
                writer.setArgb(x, y, selectColorARGB);
            }
        }

        if (hasValidPixel) {
            shared.currentTool.setValue(Tool.SELECT);
        }
    }

    private boolean pixelCanBeUsed(int argb) {
        if (argb == 0xffffffff)
            return false;
        if (argb == 0)
            return false;
        var a = (argb >> 24) & 0xff;
        return a != 0;
    }

    public void fillColor() {
        var a = shared.alpha.get();
        var r = shared.red.get();
        var g = shared.green.get();
        var b = shared.blue.get();

        if (canvas == null)
            return;
        var img = canvas.snapshot(null, null);
        var reader = img.getPixelReader();
        for (var x = 0; x < width; ++x) {
            for (var y = 0; y < height; ++y) {
                var c = reader.getArgb(x, y);
                if (c == 0)
                    continue;
                setPixel(x, y, r, g, b, a);
            }
        }
    }

    public BufferedImage getImage() {
        if (pixelBuffer == null) {
            return null;
        }
        var img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (var x = 0; x < width; ++x) {
            for (var y = 0; y < height; ++y) {
                img.setRGB(x, y, getPixel(x, y));
            }
        }
        return img;
    }

    public void requestFocus() {
        imageGroup.requestFocus();
    }
}
