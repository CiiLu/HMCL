/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.ui.patchpack;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jackhuang.hmcl.game.GameInstanceID;
import org.jackhuang.hmcl.game.HMCLGameInstance;
import org.jackhuang.hmcl.game.HMCLGameRepository;
import org.jackhuang.hmcl.modpack.ModpackConfiguration;
import org.jackhuang.hmcl.patchpack.PatchPackHelper;
import org.jackhuang.hmcl.patchpack.PatchPackInfo;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.ui.construct.ComponentList;
import org.jackhuang.hmcl.ui.construct.LineTextPane;
import org.jackhuang.hmcl.ui.construct.MessageDialogPane;
import org.jackhuang.hmcl.ui.construct.SpinnerPane;
import org.jackhuang.hmcl.ui.wizard.WizardController;
import org.jackhuang.hmcl.ui.wizard.WizardPage;
import org.jackhuang.hmcl.util.SettingsMap;
import org.jackhuang.hmcl.util.StringUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;
import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The page showing the content of a patch pack archive and starting its installation.
///
/// The archive is read asynchronously, and the install button stays disabled until its information
/// file has been parsed successfully.
@NotNullByDefault
public final class LocalPatchPackPage extends SpinnerPane implements WizardPage {

    /// The key of the charset used to decode the archive entry names in the wizard settings.
    static final SettingsMap.Key<Charset> PATCH_PACK_CHARSET = new SettingsMap.Key<>("PATCH_PACK_CHARSET");

    private final WizardController controller;

    /// Creates the patch pack information page for the archive stored in the wizard settings.
    ///
    /// @param controller the wizard controller
    public LocalPatchPackPage(WizardController controller) {
        this.controller = controller;

        JFXButton btnInstall = FXUtils.newRaisedButton(i18n("button.install"));
        btnInstall.setOnAction(e -> onInstall());
        btnInstall.setDisable(true);

        JFXButton btnURL = FXUtils.newBorderButton(i18n("patchpack.url"));
        btnURL.setVisible(false);
        btnURL.setManaged(false);

        ComponentList componentList = new ComponentList();

        VBox pane = new VBox();
        pane.setAlignment(Pos.CENTER);
        FXUtils.setLimitWidth(pane, 500);

        BorderPane buttons = new BorderPane();
        buttons.setLeft(btnURL);
        buttons.setRight(btnInstall);

        Path file = controller.getSettings().get(PatchPackInstallWizardProvider.PATCH_PACK_FILE);
        if (file == null) {
            // This page is only reachable through the selection page, so a missing archive means
            // that the wizard was navigated to incorrectly.
            Controllers.dialog(i18n("patchpack.failed"), i18n("message.error"), MessageDialogPane.MessageType.ERROR);
            Platform.runLater(controller::onEnd);
            pane.getChildren().setAll(componentList);
            setContent(pane);
            return;
        }

        showSpinner();
        Task.supplyAsync(() -> PatchPackHelper.findSuitableEncoding(file))
                .thenApplyAsync(encoding -> new LoadedPatchPack(encoding, PatchPackHelper.readPatchPackInfo(file, encoding)))
                .whenComplete(Schedulers.javafx(), (loaded, exception) -> {
                    hideSpinner();

                    if (exception != null || loaded == null) {
                        LOG.warning("Failed to read patch pack " + file, exception);
                        Controllers.dialog(i18n("patchpack.failed"), i18n("message.error"), MessageDialogPane.MessageType.ERROR);
                        Platform.runLater(controller::onEnd);
                        return;
                    }

                    controller.getSettings().put(PATCH_PACK_CHARSET, loaded.charset());
                    controller.getSettings().put(PatchPackInstallWizardProvider.PATCH_PACK_INFO, loaded.info());

                    PatchPackInfo info = loaded.info();
                    componentList.getContent().add(createTextPane(i18n("patchpack.name"), info.name()));
                    if (StringUtils.isNotBlank(info.description()))
                        componentList.getContent().add(createTextPane(i18n("patchpack.description"), info.description()));
                    if (info.authors() != null && !info.authors().isEmpty())
                        componentList.getContent().add(createTextPane(i18n("archive.author"), String.join(", ", info.authors())));

                    @Nullable LineTextPane versionPane = createVersionPane(info, readInstanceVersion(controller));
                    if (versionPane != null)
                        componentList.getContent().add(versionPane);

                    componentList.getContent().add(buttons);

                    if (StringUtils.isNotBlank(info.url())) {
                        btnURL.setVisible(true);
                        btnURL.setManaged(true);
                        btnURL.setOnAction(e -> FXUtils.openLink(info.url()));
                    }

                    btnInstall.setDisable(false);
                })
                .start();

        pane.getChildren().setAll(componentList);
        setContent(pane);
    }

    /// Creates a read-only text pane.
    ///
    /// @param title the title of the pane
    /// @param text  the text shown on the right of the pane
    /// @return the created pane
    private static LineTextPane createTextPane(String title, String text) {
        LineTextPane pane = new LineTextPane();
        pane.setTitle(title);
        pane.setText(text);
        return pane;
    }

    /// Creates the pane describing the modpack versions this patch pack supports, and whether the
    /// version of the target instance is one of them.
    ///
    /// The installation is never blocked by this check: a patch pack may still be useful for a
    /// modpack version the launcher cannot determine, or for a version outside the declared range.
    ///
    /// @param info           the patch pack information
    /// @param instanceVersion the version of the target modpack, or `null` if it is unknown
    /// @return the created pane, or `null` if this patch pack declares no version range
    private static @Nullable LineTextPane createVersionPane(PatchPackInfo info, @Nullable String instanceVersion) {
        if (StringUtils.isBlank(info.modpackVersionRange())) {
            return null;
        }

        LineTextPane pane = new LineTextPane();
        pane.setTitle(i18n("patchpack.version_range"));

        if (info.parsedModpackVersionRange() == null) {
            // The range is malformed, so it cannot be validated: show it as declared.
            pane.setText(info.modpackVersionRange());
            return pane;
        }

        if (instanceVersion == null) {
            pane.setText(i18n("patchpack.version.unknown", info.modpackVersionRange()));
            return pane;
        }

        if (info.isOutOfRange(instanceVersion)) {
            // The declared range and the instance version are both shown, so that the user can
            // decide whether this patch pack is applicable.
            pane.setText(i18n("patchpack.version.out_of_range", instanceVersion, info.modpackVersionRange()));
        } else {
            pane.setText(i18n("patchpack.version.matched", info.modpackVersionRange()));
        }
        return pane;
    }

    /// Reads the version of the modpack installed in the instance targeted by this wizard.
    ///
    /// @param controller the wizard controller
    /// @return the modpack version, or `null` if it is unknown
    private static @Nullable String readInstanceVersion(WizardController controller) {
        @Nullable GameInstanceID instanceId = controller.getSettings().get(PatchPackInstallWizardProvider.INSTANCE_ID);
        HMCLGameRepository repository = controller.getSettings().get(PatchPackInstallWizardProvider.REPOSITORY);
        if (instanceId == null || repository == null)
            return null;

        @Nullable HMCLGameInstance instance = repository.findInstance(instanceId);
        if (instance == null)
            return null;

        try {
            @Nullable ModpackConfiguration<?> configuration = instance.readModpackConfiguration();
            return configuration != null ? configuration.getVersion() : null;
        } catch (IOException e) {
            LOG.warning("Failed to read modpack configuration of " + instanceId, e);
            return null;
        }
    }

    /// Starts the installation of the patch pack read by this page.
    private void onInstall() {
        if (controller.getSettings().get(PatchPackInstallWizardProvider.PATCH_PACK_INFO) == null)
            return;

        controller.onFinish();
    }

    @Override
    public String getTitle() {
        return i18n("patchpack.task.install");
    }

    /// A patch pack information file together with the charset used to read its archive.
    ///
    /// @param charset the charset detected for the archive entry names
    /// @param info    the parsed patch pack information
    private record LoadedPatchPack(Charset charset, PatchPackInfo info) {
    }
}
