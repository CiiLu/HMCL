/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.ui;

import com.jfoenix.controls.*;
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.WeakListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.*;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventType;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.glavo.png.PNGType;
import org.glavo.png.PNGWriter;
import org.glavo.png.javafx.PNGJavaFXUtils;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.animation.AnimationUtils;
import org.jackhuang.hmcl.util.*;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.NetworkUtils;
import org.jackhuang.hmcl.util.javafx.ExtendedProperties;
import org.jackhuang.hmcl.util.javafx.SafeStringConverter;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.SystemUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.jackhuang.hmcl.util.Lang.thread;
import static org.jackhuang.hmcl.util.Lang.tryCast;
import static org.jackhuang.hmcl.util.logging.Logger.LOG;
import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

public final class FXUtils {
    private FXUtils() {
    }

    public static final int JAVAFX_MAJOR_VERSION;

    /// @see Platform.Preferences
    public static final @Nullable ObservableMap<String, Object> PREFERENCES;
    public static final @Nullable ObservableBooleanValue DARK_MODE;
    public static final @Nullable Boolean REDUCED_MOTION;

    static {
        String jfxVersion = System.getProperty("javafx.version");
        int majorVersion = -1;
        if (jfxVersion != null) {
            Matcher matcher = Pattern.compile("^(?<version>[0-9]+)").matcher(jfxVersion);
            if (matcher.find()) {
                majorVersion = Lang.parseInt(matcher.group(), -1);
            }
        }
        JAVAFX_MAJOR_VERSION = majorVersion;

        ObservableMap<String, Object> preferences = null;
        ObservableBooleanValue darkMode = null;
        Boolean reducedMotion = null;
        if (JAVAFX_MAJOR_VERSION >= 22) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> preferencesClass = Class.forName("javafx.application.Platform$Preferences");
                @SuppressWarnings("unchecked")
                var preferences0 = (ObservableMap<String, Object>) lookup.findStatic(Platform.class, "getPreferences", MethodType.methodType(preferencesClass))
                        .invoke();
                preferences = preferences0;

                @SuppressWarnings("unchecked")
                var colorSchemeProperty =
                        (ReadOnlyObjectProperty<? extends Enum<?>>)
                                lookup.findVirtual(preferencesClass, "colorSchemeProperty", MethodType.methodType(ReadOnlyObjectProperty.class))
                                        .invoke(preferences);

                darkMode = Bindings.createBooleanBinding(() ->
                        "DARK".equals(colorSchemeProperty.get().name()), colorSchemeProperty);

                if (JAVAFX_MAJOR_VERSION >= 24) {
                    reducedMotion = (boolean)
                            lookup.findVirtual(preferencesClass, "isReducedMotion", MethodType.methodType(boolean.class))
                                    .invoke(preferences);
                }
            } catch (Throwable e) {
                LOG.warning("Failed to get preferences", e);
            }
        }
        PREFERENCES = preferences;
        DARK_MODE = darkMode;
        REDUCED_MOTION = reducedMotion;
    }

    public static final String DEFAULT_MONOSPACE_FONT = OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? "Consolas" : "Monospace";

    public static final List<String> IMAGE_EXTENSIONS = Lang.immutableListOf(
            "png", "jpg", "jpeg", "bmp", "gif", "webp"
    );

    private static final Map<String, Image> builtinImageCache = new ConcurrentHashMap<>();
    private static final Map<String, Path> remoteImageCache = new ConcurrentHashMap<>();

    public static void shutdown() {
        for (Map.Entry<String, Path> entry : remoteImageCache.entrySet()) {
            try {
                Files.deleteIfExists(entry.getValue());
            } catch (IOException e) {
                LOG.warning(String.format("Failed to delete cache file %s.", entry.getValue()), e);
            }
            remoteImageCache.remove(entry.getKey());
        }

        builtinImageCache.clear();
    }

    public static void runInFX(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    public static void checkFxUserThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Not on FX application thread; currentThread = "
                    + Thread.currentThread().getName());
        }
    }

    public static InvalidationListener onInvalidating(Runnable action) {
        return arg -> action.run();
    }

    public static <T> void onChange(ObservableValue<T> value, Consumer<T> consumer) {
        value.addListener((a, b, c) -> consumer.accept(c));
    }

    public static <T> ChangeListener<T> onWeakChange(ObservableValue<T> value, Consumer<T> consumer) {
        ChangeListener<T> listener = (a, b, c) -> consumer.accept(c);
        value.addListener(new WeakChangeListener<>(listener));
        return listener;
    }

    public static <T> void onChangeAndOperate(ObservableValue<T> value, Consumer<T> consumer) {
        consumer.accept(value.getValue());
        onChange(value, consumer);
    }

    public static <T> ChangeListener<T> onWeakChangeAndOperate(ObservableValue<T> value, Consumer<T> consumer) {
        consumer.accept(value.getValue());
        return onWeakChange(value, consumer);
    }

    public static InvalidationListener observeWeak(Runnable runnable, Observable... observables) {
        InvalidationListener originalListener = observable -> runnable.run();
        WeakInvalidationListener listener = new WeakInvalidationListener(originalListener);
        for (Observable observable : observables) {
            observable.addListener(listener);
        }
        runnable.run();
        return originalListener;
    }

    public static void runLaterIf(BooleanSupplier condition, Runnable runnable) {
        if (condition.getAsBoolean()) Platform.runLater(() -> runLaterIf(condition, runnable));
        else runnable.run();
    }

    public static void limitSize(ImageView imageView, double maxWidth, double maxHeight) {
        imageView.setPreserveRatio(true);
        onChangeAndOperate(imageView.imageProperty(), image -> {
            if (image != null && (image.getWidth() > maxWidth || image.getHeight() > maxHeight)) {
                imageView.setFitHeight(maxHeight);
                imageView.setFitWidth(maxWidth);
            } else {
                imageView.setFitHeight(-1);
                imageView.setFitWidth(-1);
            }
        });
    }

    private static class ListenerPair<T> {
        private final ObservableValue<T> value;
        private final ChangeListener<? super T> listener;

        ListenerPair(ObservableValue<T> value, ChangeListener<? super T> listener) {
            this.value = value;
            this.listener = listener;
        }

        void bind() {
            value.addListener(listener);
        }

        void unbind() {
            value.removeListener(listener);
        }
    }

    public static <T> void addListener(Node node, String key, ObservableValue<T> value, Consumer<? super T> callback) {
        ListenerPair<T> pair = new ListenerPair<>(value, (a, b, newValue) -> callback.accept(newValue));
        node.getProperties().put(key, pair);
        pair.bind();
    }

    public static void removeListener(Node node, String key) {
        tryCast(node.getProperties().get(key), ListenerPair.class)
                .ifPresent(info -> {
                    info.unbind();
                    node.getProperties().remove(key);
                });
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> void ignoreEvent(Node node, EventType<T> type, Predicate<? super T> filter) {
        EventDispatcher oldDispatcher = node.getEventDispatcher();
        node.setEventDispatcher((event, tail) -> {
            EventType<?> t = event.getEventType();
            while (t != null && t != type)
                t = t.getSuperType();
            if (t == type && filter.test((T) event)) {
                return tail.dispatchEvent(event);
            } else {
                return oldDispatcher.dispatchEvent(event, tail);
            }
        });
    }

    public static void setValidateWhileTextChanged(Node field, boolean validate) {
        if (field instanceof JFXTextField) {
            if (validate) {
                addListener(field, "FXUtils.validation", ((JFXTextField) field).textProperty(), o -> ((JFXTextField) field).validate());
            } else {
                removeListener(field, "FXUtils.validation");
            }
            ((JFXTextField) field).validate();
        } else if (field instanceof JFXPasswordField) {
            if (validate) {
                addListener(field, "FXUtils.validation", ((JFXPasswordField) field).textProperty(), o -> ((JFXPasswordField) field).validate());
            } else {
                removeListener(field, "FXUtils.validation");
            }
            ((JFXPasswordField) field).validate();
        } else
            throw new IllegalArgumentException("Only JFXTextField and JFXPasswordField allowed");
    }

    public static boolean getValidateWhileTextChanged(Node field) {
        return field.getProperties().containsKey("FXUtils.validation");
    }

    public static Rectangle setOverflowHidden(Region region) {
        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(region.widthProperty());
        rectangle.heightProperty().bind(region.heightProperty());
        region.setClip(rectangle);
        return rectangle;
    }

    public static Rectangle setOverflowHidden(Region region, double arc) {
        Rectangle rectangle = setOverflowHidden(region);
        rectangle.setArcWidth(arc);
        rectangle.setArcHeight(arc);
        return rectangle;
    }

    public static void setLimitWidth(Region region, double width) {
        region.setMaxWidth(width);
        region.setMinWidth(width);
        region.setPrefWidth(width);
    }

    public static double getLimitWidth(Region region) {
        return region.getMaxWidth();
    }

    public static void setLimitHeight(Region region, double height) {
        region.setMaxHeight(height);
        region.setMinHeight(height);
        region.setPrefHeight(height);
    }

    public static double getLimitHeight(Region region) {
        return region.getMaxHeight();
    }

    public static Node limitingSize(Node node, double width, double height) {
        StackPane pane = new StackPane(node);
        pane.setAlignment(Pos.CENTER);
        FXUtils.setLimitWidth(pane, width);
        FXUtils.setLimitHeight(pane, height);
        return pane;
    }

    public static void smoothScrolling(ScrollPane scrollPane) {
        if (AnimationUtils.isAnimationEnabled())
            ScrollUtils.addSmoothScrolling(scrollPane);
    }

    private static final Duration TOOLTIP_FAST_SHOW_DELAY = Duration.millis(50);
    private static final Duration TOOLTIP_SLOW_SHOW_DELAY = Duration.millis(500);
    private static final Duration TOOLTIP_SHOW_DURATION = Duration.millis(5000);

    public static void installTooltip(Node node, Duration showDelay, Duration showDuration, Duration hideDelay, Tooltip tooltip) {
        tooltip.setShowDelay(showDelay);
        tooltip.setShowDuration(showDuration);
        tooltip.setHideDelay(hideDelay);
        Tooltip.install(node, tooltip);
    }

    public static void installFastTooltip(Node node, Tooltip tooltip) {
        runInFX(() -> installTooltip(node, TOOLTIP_FAST_SHOW_DELAY, TOOLTIP_SHOW_DURATION, Duration.ZERO, tooltip));
    }

    public static void installFastTooltip(Node node, String tooltip) {
        installFastTooltip(node, new Tooltip(tooltip));
    }

    public static void installSlowTooltip(Node node, Tooltip tooltip) {
        runInFX(() -> installTooltip(node, TOOLTIP_SLOW_SHOW_DELAY, TOOLTIP_SHOW_DURATION, Duration.ZERO, tooltip));
    }

    public static void installSlowTooltip(Node node, String tooltip) {
        installSlowTooltip(node, new Tooltip(tooltip));
    }

    public static void playAnimation(Node node, String animationKey, Timeline timeline) {
        animationKey = "FXUTILS.ANIMATION." + animationKey;
        Object oldTimeline = node.getProperties().get(animationKey);
//        if (oldTimeline instanceof Timeline) ((Timeline) oldTimeline).stop();
        if (timeline != null) timeline.play();
        node.getProperties().put(animationKey, timeline);
    }

    public static <T> Animation playAnimation(Node node, String animationKey, Duration duration, WritableValue<T> property, T from, T to, Interpolator interpolator) {
        if (from == null) from = property.getValue();
        if (duration == null || Objects.equals(duration, Duration.ZERO) || Objects.equals(from, to)) {
            playAnimation(node, animationKey, null);
            property.setValue(to);
            return null;
        } else {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(property, from, interpolator)),
                    new KeyFrame(duration, new KeyValue(property, to, interpolator))
            );
            playAnimation(node, animationKey, timeline);
            return timeline;
        }
    }

    public static void openFolder(File file) {
        if (!FileUtils.makeDirectory(file)) {
            LOG.error("Unable to make directory " + file);
            return;
        }

        String path = file.getAbsolutePath();

        String openCommand;
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
            openCommand = "explorer.exe";
        else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
            openCommand = "/usr/bin/open";
        else if (OperatingSystem.CURRENT_OS.isLinuxOrBSD() && new File("/usr/bin/xdg-open").exists())
            openCommand = "/usr/bin/xdg-open";
        else
            openCommand = null;

        thread(() -> {
            if (openCommand != null) {
                try {
                    int exitCode = SystemUtils.callExternalProcess(openCommand, path);

                    // explorer.exe always return 1
                    if (exitCode == 0 || (exitCode == 1 && OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS))
                        return;
                    else
                        LOG.warning("Open " + path + " failed with code " + exitCode);
                } catch (Throwable e) {
                    LOG.warning("Unable to open " + path + " by executing " + openCommand, e);
                }
            }

            // Fallback to java.awt.Desktop::open
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Throwable e) {
                LOG.error("Unable to open " + path + " by java.awt.Desktop.getDesktop()::open", e);
            }
        });
    }

    public static void showFileInExplorer(Path file) {
        String path = file.toAbsolutePath().toString();

        String[] openCommands;
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
            openCommands = new String[]{"explorer.exe", "/select,", path};
        else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
            openCommands = new String[]{"/usr/bin/open", "-R", path};
        else if (OperatingSystem.CURRENT_OS.isLinuxOrBSD() && SystemUtils.which("dbus-send") != null)
            openCommands = new String[]{
                    "dbus-send",
                    "--print-reply",
                    "--dest=org.freedesktop.FileManager1",
                    "/org/freedesktop/FileManager1",
                    "org.freedesktop.FileManager1.ShowItems",
                    "array:string:" + file.toAbsolutePath().toUri(),
                    "string:"
            };
        else
            openCommands = null;

        if (openCommands != null) {
            thread(() -> {
                try {
                    int exitCode = SystemUtils.callExternalProcess(openCommands);

                    // explorer.exe always return 1
                    if (exitCode == 0 || (exitCode == 1 && OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS))
                        return;
                    else
                        LOG.warning("Show " + path + " in explorer failed with code " + exitCode);
                } catch (Throwable e) {
                    LOG.warning("Unable to show " + path + " in explorer", e);
                }

                // Fallback to open folder
                openFolder(file.getParent().toFile());
            });
        } else {
            // We do not have a universal method to show file in file manager.
            openFolder(file.getParent().toFile());
        }
    }

    private static final String[] linuxBrowsers = {
            "xdg-open",
            "google-chrome",
            "firefox",
            "microsoft-edge",
            "opera",
            "konqueror",
            "mozilla"
    };

    /**
     * Open URL in browser
     *
     * @param link null is allowed but will be ignored
     */
    public static void openLink(String link) {
        if (link == null)
            return;

        thread(() -> {
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
                try {
                    Runtime.getRuntime().exec(new String[]{"rundll32.exe", "url.dll,FileProtocolHandler", link});
                    return;
                } catch (Throwable e) {
                    LOG.warning("An exception occurred while calling rundll32", e);
                }
            }
            if (OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                for (String browser : linuxBrowsers) {
                    Path path = SystemUtils.which(browser);
                    if (path != null) {
                        try {
                            Runtime.getRuntime().exec(new String[]{path.toString(), link});
                            return;
                        } catch (Throwable ignored) {
                        }
                    }
                    LOG.warning("No known browser found");
                }
            }
            try {
                java.awt.Desktop.getDesktop().browse(new URI(link));
            } catch (Throwable e) {
                if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", link});
                    } catch (IOException ex) {
                        LOG.warning("Unable to open link: " + link, ex);
                    }
                LOG.warning("Failed to open link: " + link, e);
            }
        });
    }

    public static <T> void bind(JFXTextField textField, Property<T> property, StringConverter<T> converter) {
        TextFieldBinding<T> binding = new TextFieldBinding<>(textField, property, converter);
        binding.updateTextField();
        textField.getProperties().put("FXUtils.bind.binding", binding);
        textField.focusedProperty().addListener(binding.focusedListener);
        textField.sceneProperty().addListener(binding.sceneListener);
        property.addListener(binding.propertyListener);
    }

    public static void bindInt(JFXTextField textField, Property<Number> property) {
        bind(textField, property, SafeStringConverter.fromInteger());
    }

    public static void bindString(JFXTextField textField, Property<String> property) {
        bind(textField, property, null);
    }

    public static void unbind(JFXTextField textField, Property<?> property) {
        TextFieldBinding<?> binding = (TextFieldBinding<?>) textField.getProperties().remove("FXUtils.bind.binding");
        if (binding != null) {
            textField.focusedProperty().removeListener(binding.focusedListener);
            textField.sceneProperty().removeListener(binding.sceneListener);
            property.removeListener(binding.propertyListener);
        }
    }

    private static final class TextFieldBinding<T> {
        private final JFXTextField textField;
        private final Property<T> property;
        private final StringConverter<T> converter;

        public final ChangeListener<Boolean> focusedListener;
        public final ChangeListener<Scene> sceneListener;
        public final InvalidationListener propertyListener;

        public TextFieldBinding(JFXTextField textField, Property<T> property, StringConverter<T> converter) {
            this.textField = textField;
            this.property = property;
            this.converter = converter;

            focusedListener = (observable, oldFocused, newFocused) -> {
                if (oldFocused && !newFocused) {
                    if (textField.validate()) {
                        updateProperty();
                    } else {
                        // Rollback to old value
                        updateTextField();
                    }
                }
            };

            sceneListener = (observable, oldScene, newScene) -> {
                if (oldScene != null && newScene == null) {
                    // Component is being removed from scene
                    if (textField.validate()) {
                        updateProperty();
                    }
                }
            };

            propertyListener = observable -> {
                updateTextField();
            };
        }

        public void updateProperty() {
            String newText = textField.getText();
            @SuppressWarnings("unchecked")
            T newValue = converter == null ? (T) newText : converter.fromString(newText);

            if (!Objects.equals(newValue, property.getValue())) {
                property.setValue(newValue);
            }
        }

        public void updateTextField() {
            T value = property.getValue();
            textField.setText(converter == null ? (String) value : converter.toString(value));
        }
    }

    private static final class EnumBidirectionalBinding<E extends Enum<E>> implements InvalidationListener, WeakListener {
        private final WeakReference<JFXComboBox<E>> comboBoxRef;
        private final WeakReference<Property<E>> propertyRef;
        private final int hashCode;

        private boolean updating = false;

        private EnumBidirectionalBinding(JFXComboBox<E> comboBox, Property<E> property) {
            this.comboBoxRef = new WeakReference<>(comboBox);
            this.propertyRef = new WeakReference<>(property);
            this.hashCode = System.identityHashCode(comboBox) ^ System.identityHashCode(property);
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final JFXComboBox<E> comboBox = comboBoxRef.get();
                final Property<E> property = propertyRef.get();

                if (comboBox == null || property == null) {
                    if (comboBox != null) {
                        comboBox.getSelectionModel().selectedItemProperty().removeListener(this);
                    }

                    if (property != null) {
                        property.removeListener(this);
                    }
                } else {
                    updating = true;
                    try {
                        if (property == sourceProperty) {
                            E newValue = property.getValue();
                            comboBox.getSelectionModel().select(newValue);
                        } else {
                            E newValue = comboBox.getSelectionModel().getSelectedItem();
                            property.setValue(newValue);
                        }
                    } finally {
                        updating = false;
                    }
                }
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return comboBoxRef.get() == null || propertyRef.get() == null;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof EnumBidirectionalBinding))
                return false;

            EnumBidirectionalBinding<?> that = (EnumBidirectionalBinding<?>) o;

            final JFXComboBox<E> comboBox = this.comboBoxRef.get();
            final Property<E> property = this.propertyRef.get();

            final JFXComboBox<?> thatComboBox = that.comboBoxRef.get();
            final Property<?> thatProperty = that.propertyRef.get();

            if (comboBox == null || property == null || thatComboBox == null || thatProperty == null)
                return false;

            return comboBox == thatComboBox && property == thatProperty;
        }
    }

    /**
     * Bind combo box selection with given enum property bidirectionally.
     * You should <b>only and always</b> use {@code bindEnum} as well as {@code unbindEnum} at the same time.
     *
     * @param comboBox the combo box being bound with {@code property}.
     * @param property the property being bound with {@code combo box}.
     * @see #unbindEnum(JFXComboBox, Property)
     * @see ExtendedProperties#selectedItemPropertyFor(ComboBox)
     */
    public static <T extends Enum<T>> void bindEnum(JFXComboBox<T> comboBox, Property<T> property) {
        EnumBidirectionalBinding<T> binding = new EnumBidirectionalBinding<>(comboBox, property);

        comboBox.getSelectionModel().selectedItemProperty().removeListener(binding);
        property.removeListener(binding);

        comboBox.getSelectionModel().select(property.getValue());
        comboBox.getSelectionModel().selectedItemProperty().addListener(binding);
        property.addListener(binding);
    }

    /**
     * Unbind combo box selection with given enum property bidirectionally.
     * You should <b>only and always</b> use {@code bindEnum} as well as {@code unbindEnum} at the same time.
     *
     * @param comboBox the combo box being bound with the property which can be inferred by {@code bindEnum}.
     * @see #bindEnum(JFXComboBox, Property)
     * @see ExtendedProperties#selectedItemPropertyFor(ComboBox)
     */
    public static <T extends Enum<T>> void unbindEnum(JFXComboBox<T> comboBox, Property<T> property) {
        EnumBidirectionalBinding<T> binding = new EnumBidirectionalBinding<>(comboBox, property);
        comboBox.getSelectionModel().selectedItemProperty().removeListener(binding);
        property.removeListener(binding);
    }

    public static void bindAllEnabled(BooleanProperty allEnabled, BooleanProperty... children) {
        int itemCount = children.length;
        int childSelectedCount = 0;
        for (BooleanProperty child : children) {
            if (child.get())
                childSelectedCount++;
        }

        allEnabled.set(childSelectedCount == itemCount);

        class Listener implements InvalidationListener {
            private int childSelectedCount;
            private boolean updating = false;

            public Listener(int childSelectedCount) {
                this.childSelectedCount = childSelectedCount;
            }

            @Override
            public void invalidated(Observable observable) {
                if (updating)
                    return;

                updating = true;
                try {
                    boolean value = ((BooleanProperty) observable).get();

                    if (observable == allEnabled) {
                        for (BooleanProperty child : children) {
                            child.setValue(value);
                        }
                        childSelectedCount = value ? itemCount : 0;
                    } else {
                        if (value)
                            childSelectedCount++;
                        else
                            childSelectedCount--;

                        allEnabled.set(childSelectedCount == itemCount);
                    }
                } finally {
                    updating = false;
                }
            }
        }

        InvalidationListener listener = new Listener(childSelectedCount);

        WeakInvalidationListener weakListener = new WeakInvalidationListener(listener);
        allEnabled.addListener(listener);
        for (BooleanProperty child : children) {
            child.addListener(weakListener);
        }
    }

    public static void setIcon(Stage stage) {
        String icon;
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            icon = "/assets/img/icon.png";
        } else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS) {
            icon = "/assets/img/icon-mac.png";
        } else {
            icon = "/assets/img/icon@4x.png";
        }
        stage.getIcons().add(newBuiltinImage(icon));
    }

    private static Image loadWebPImage(InputStream input) throws IOException {
        WebPImageReaderSpi spi = new WebPImageReaderSpi();
        ImageReader reader = spi.createReaderInstance(null);

        try (ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            reader.setInput(imageInput, true, true);
            return SwingFXUtils.toFXImage(reader.read(0, reader.getDefaultReadParam()), null);
        } finally {
            reader.dispose();
        }
    }

    public static Image loadImage(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path)) {
            if ("webp".equalsIgnoreCase(FileUtils.getExtension(path)))
                return loadWebPImage(input);
            else {
                Image image = new Image(input);
                if (image.isError())
                    throw image.getException();
                return image;
            }
        }
    }

    public static Image loadImage(URL url) throws Exception {
        URLConnection connection = NetworkUtils.createConnection(url);
        if (connection instanceof HttpURLConnection) {
            connection = NetworkUtils.resolveConnection((HttpURLConnection) connection);
        }

        try (InputStream input = connection.getInputStream()) {
            String path = url.getPath();
            if (path != null && "webp".equalsIgnoreCase(StringUtils.substringAfterLast(path, '.')))
                return loadWebPImage(input);
            else {
                Image image = new Image(input);
                if (image.isError())
                    throw image.getException();
                return image;
            }
        }
    }

    /**
     * Suppress IllegalArgumentException since the url is supposed to be correct definitely.
     *
     * @param url the url of image. The image resource should be a file within the jar.
     * @return the image resource within the jar.
     * @see org.jackhuang.hmcl.util.CrashReporter
     * @see ResourceNotFoundError
     */
    public static Image newBuiltinImage(String url) {
        try {
            return builtinImageCache.computeIfAbsent(url, Image::new);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundError("Cannot access image: " + url, e);
        }
    }

    /**
     * Suppress IllegalArgumentException since the url is supposed to be correct definitely.
     *
     * @param url             the url of image. The image resource should be a file within the jar.
     * @param requestedWidth  the image's bounding box width
     * @param requestedHeight the image's bounding box height
     * @param preserveRatio   indicates whether to preserve the aspect ratio of
     *                        the original image when scaling to fit the image within the
     *                        specified bounding box
     * @param smooth          indicates whether to use a better quality filtering
     *                        algorithm or a faster one when scaling this image to fit within
     *                        the specified bounding box
     * @return the image resource within the jar.
     * @see org.jackhuang.hmcl.util.CrashReporter
     * @see ResourceNotFoundError
     */
    public static Image newBuiltinImage(String url, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth) {
        try {
            return new Image(url, requestedWidth, requestedHeight, preserveRatio, smooth);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundError("Cannot access image: " + url, e);
        }
    }

    /**
     * Load image from the internet. It will cache the data of images for the further usage.
     * The cached data will be deleted when HMCL is closed or hidden.
     *
     * @param url the url of image. The image resource should be a file on the internet.
     * @return the image resource within the jar.
     */
    public static Image newRemoteImage(String url) {
        return newRemoteImage(url, 0, 0, false, false, false);
    }

    /**
     * Load image from the internet. It will cache the data of images for the further usage.
     * The cached data will be deleted when HMCL is closed or hidden.
     *
     * @param url             the url of image. The image resource should be a file on the internet.
     * @param requestedWidth  the image's bounding box width
     * @param requestedHeight the image's bounding box height
     * @param preserveRatio   indicates whether to preserve the aspect ratio of
     *                        the original image when scaling to fit the image within the
     *                        specified bounding box
     * @param smooth          indicates whether to use a better quality filtering
     *                        algorithm or a faster one when scaling this image to fit within
     *                        the specified bounding box
     * @return the image resource within the jar.
     */
    public static Image newRemoteImage(String url, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth, boolean backgroundLoading) {
        Path currentPath = remoteImageCache.get(url);
        if (currentPath != null) {
            if (Files.isReadable(currentPath)) {
                try (InputStream inputStream = Files.newInputStream(currentPath)) {
                    return new Image(inputStream, requestedWidth, requestedHeight, preserveRatio, smooth);
                } catch (IOException e) {
                    LOG.warning("An exception encountered while reading data from cached image file.", e);
                }
            }

            // The file is unavailable or unreadable.
            remoteImageCache.remove(url);

            try {
                Files.deleteIfExists(currentPath);
            } catch (IOException e) {
                LOG.warning("An exception encountered while deleting broken cached image file.", e);
            }
        }

        Image image = new Image(url, requestedWidth, requestedHeight, preserveRatio, smooth, backgroundLoading);
        image.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1.0 && !image.isError() && image.getPixelReader() != null && image.getWidth() > 0.0 && image.getHeight() > 0.0) {
                Task.runAsync(() -> {
                    Path newPath = Files.createTempFile("hmcl-net-resource-cache-", ".cache");
                    try ( // Make sure the file is released from JVM before we put the path into remoteImageCache.
                          OutputStream outputStream = Files.newOutputStream(newPath);
                          PNGWriter writer = new PNGWriter(outputStream, PNGType.RGBA, PNGWriter.DEFAULT_COMPRESS_LEVEL)
                    ) {
                        writer.write(PNGJavaFXUtils.asArgbImage(image));
                    } catch (IOException e) {
                        try {
                            Files.delete(newPath);
                        } catch (IOException e2) {
                            e2.addSuppressed(e);
                            throw e2;
                        }
                        throw e;
                    }
                    if (remoteImageCache.putIfAbsent(url, newPath) != null) {
                        Files.delete(newPath); // The image has been loaded in another task. Delete the image here in order not to pollute the tmp folder.
                    }
                }).start();
            }
        });
        return image;
    }

    public static JFXButton newRaisedButton(String text) {
        JFXButton button = new JFXButton(text);
        button.getStyleClass().add("jfx-button-raised");
        button.setButtonType(JFXButton.ButtonType.RAISED);
        return button;
    }

    public static JFXButton newBorderButton(String text) {
        JFXButton button = new JFXButton(text);
        button.getStyleClass().add("jfx-button-border");
        button.setButtonType(JFXButton.ButtonType.RAISED);
        return button;
    }

    public static Label truncatedLabel(String text, int limit) {
        Label label = new Label();
        if (text.length() <= limit) {
            label.setText(text);
        } else {
            label.setText(StringUtils.truncate(text, limit));
            installFastTooltip(label, text);
        }
        return label;
    }

    public static void applyDragListener(Node node, FileFilter filter, Consumer<List<File>> callback) {
        applyDragListener(node, filter, callback, null);
    }

    public static void applyDragListener(Node node, FileFilter filter, Consumer<List<File>> callback, Runnable dragDropped) {
        node.setOnDragOver(event -> {
            if (event.getGestureSource() != node && event.getDragboard().hasFiles()) {
                if (event.getDragboard().getFiles().stream().anyMatch(filter::accept))
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        node.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            if (files != null) {
                List<File> acceptFiles = files.stream().filter(filter::accept).collect(Collectors.toList());
                if (!acceptFiles.isEmpty()) {
                    callback.accept(acceptFiles);
                    event.setDropCompleted(true);
                }
            }
            if (dragDropped != null)
                dragDropped.run();
            event.consume();
        });
    }

    public static <T> StringConverter<T> stringConverter(Function<T, String> func) {
        return new StringConverter<T>() {

            @Override
            public String toString(T object) {
                return object == null ? "" : func.apply(object);
            }

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <T> Callback<ListView<T>, ListCell<T>> jfxListCellFactory(Function<T, Node> graphicBuilder) {
        Holder<Object> lastCell = new Holder<>();
        return view -> new JFXListCell<T>() {
            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                // https://mail.openjdk.org/pipermail/openjfx-dev/2022-July/034764.html
                if (this == lastCell.value && !isVisible())
                    return;
                lastCell.value = this;

                if (!empty) {
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(graphicBuilder.apply(item));
                }
            }
        };
    }

    public static ColumnConstraints getColumnFillingWidth() {
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setFillWidth(true);
        return constraint;
    }

    public static ColumnConstraints getColumnHgrowing() {
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setFillWidth(true);
        constraint.setHgrow(Priority.ALWAYS);
        return constraint;
    }

    public static final Interpolator SINE = new Interpolator() {
        @Override
        protected double curve(double t) {
            return Math.sin(t * Math.PI / 2);
        }

        @Override
        public String toString() {
            return "Interpolator.SINE";
        }
    };

    public static final Interpolator EASE = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

    public static void onEscPressed(Node node, Runnable action) {
        node.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                action.run();
                e.consume();
            }
        });
    }

    public static void onClicked(Node node, Runnable action) {
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                action.run();
                e.consume();
            }
        });
    }

    public static void copyOnDoubleClick(Labeled label) {
        label.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                String text = label.getText();
                if (text != null && !text.isEmpty()) {
                    copyText(label.getText());
                    e.consume();
                }
            }
        });
    }

    public static void copyText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);

        if (!Controllers.isStopped()) {
            Controllers.showToast(i18n("message.copied"));
        }
    }

    public static List<Node> parseSegment(String segment, Consumer<String> hyperlinkAction) {
        if (segment.indexOf('<') < 0)
            return Collections.singletonList(new Text(segment));

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader("<body>" + segment + "</body>")));
            Element r = doc.getDocumentElement();

            NodeList children = r.getChildNodes();
            List<Node> texts = new ArrayList<>();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node node = children.item(i);

                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("a".equals(element.getTagName())) {
                        String href = element.getAttribute("href");
                        Text text = new Text(element.getTextContent());
                        onClicked(text, () -> {
                            String link = href;
                            try {
                                link = new URI(href).toASCIIString();
                            } catch (URISyntaxException ignored) {
                            }
                            hyperlinkAction.accept(link);
                        });
                        text.setCursor(Cursor.HAND);
                        text.setFill(Color.web("#0070E0"));
                        text.setUnderline(true);
                        texts.add(text);
                    } else if ("b".equals(element.getTagName())) {
                        Text text = new Text(element.getTextContent());
                        text.getStyleClass().add("bold");
                        texts.add(text);
                    } else if ("br".equals(element.getTagName())) {
                        texts.add(new Text("\n"));
                    } else {
                        throw new IllegalArgumentException("unsupported tag " + element.getTagName());
                    }
                } else {
                    texts.add(new Text(node.getTextContent()));
                }
            }
            return texts;
        } catch (SAXException | ParserConfigurationException | IOException e) {
            LOG.warning("Failed to parse xml", e);
            return Collections.singletonList(new Text(segment));
        }
    }

    public static TextFlow segmentToTextFlow(final String segment, Consumer<String> hyperlinkAction) {
        TextFlow tf = new TextFlow();
        tf.getChildren().setAll(parseSegment(segment, hyperlinkAction));
        return tf;
    }

    public static String toWeb(Color color) {
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);

        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static FileChooser.ExtensionFilter getImageExtensionFilter() {
        return new FileChooser.ExtensionFilter(i18n("extension.png"),
                IMAGE_EXTENSIONS.stream().map(ext -> "*." + ext).toArray(String[]::new));
    }
}
