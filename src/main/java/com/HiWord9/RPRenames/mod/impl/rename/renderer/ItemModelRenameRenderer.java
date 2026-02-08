package com.HiWord9.RPRenames.mod.impl.rename.renderer;

import com.HiWord9.RPRenames.api.rename.renderer.RenameRenderer;
import com.HiWord9.RPRenames.api.rename.renderer.SimpleRenameRenderer;
import com.HiWord9.RPRenames.mod.gui.Graphics;
import com.HiWord9.RPRenames.mod.gui.tooltip_component.MultiItemTooltipComponent;
import com.HiWord9.RPRenames.mod.gui.tooltip_component.preview.ItemPreviewTooltipComponent;
import com.HiWord9.RPRenames.mod.gui.tooltip_component.preview.PlayerPreviewTooltipComponent;
import com.HiWord9.RPRenames.mod.gui.widget.RPRWidget;
import com.HiWord9.RPRenames.mod.impl.rename.ItemModelRename;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.HiWord9.RPRenames.mod.gui.Graphics.tooltipOf;
import static com.HiWord9.RPRenames.mod.util.RenameRendererHelper.*;
import static com.HiWord9.RPRenames.mod.util.Util.*;

public class ItemModelRenameRenderer extends SimpleRenameRenderer<ItemModelRename> implements RenameRenderer.Preview {
    private static final MutableText playerPreviewHintShift = Text.translatable(
            "rprenames.gui.tooltipHint.playerPreview.holdShift",
            Text.translatable("rprenames.key.shift").formatted(Formatting.GRAY)
    ).formatted(Formatting.DARK_GRAY);

    private static final MutableText playerPreviewHintF = Text.translatable(
            "rprenames.gui.tooltipHint.playerPreview.pressF",
            Text.translatable("rprenames.key.f").formatted(Formatting.GRAY)
    ).formatted(Formatting.DARK_GRAY);

    private static final MutableText favoriteHintAdd = Text.translatable(
            "rprenames.gui.tooltipHint.favorite.add",
            Text.translatable("rprenames.key.rmb").formatted(Formatting.GRAY)
    ).formatted(Formatting.DARK_GRAY);

    private static final MutableText favoriteHintRemove = Text.translatable(
            "rprenames.gui.tooltipHint.favorite.remove",
            Text.translatable("rprenames.key.rmb").formatted(Formatting.GRAY)
    ).formatted(Formatting.DARK_GRAY);

    private static final MutableText nextHintN = Text.translatable(
            "rprenames.gui.tooltipHint.nextHint.pressN",
            Text.translatable("rprenames.key.n").formatted(Formatting.GRAY)
    ).formatted(Formatting.DARK_GRAY);

    private static final MutableText disableHint = Text.translatable(
            "rprenames.gui.tooltipHint.disable",
            Text.translatable("rprenames.gui.tooltipHint.disable.command").formatted(Formatting.RED)
    ).formatted(Formatting.DARK_RED);

    RPRWidget rprWidget;
    Supplier<Boolean> favoriteSupplier;

    ItemPreviewTooltipComponent itemPreviewTooltipComponent;
    PlayerPreviewTooltipComponent playerPreviewTooltipComponent;

    public ItemModelRenameRenderer(ItemModelRename rename, RPRWidget rprWidget, Supplier<Boolean> favoriteSupplier) {
        super(rename);
        this.rprWidget = rprWidget;
        this.favoriteSupplier = favoriteSupplier;

        int width = Graphics.DEFAULT_PREVIEW_WIDTH;
        int height = Graphics.DEFAULT_PREVIEW_HEIGHT;

        assert player() != null;

        int playerSize = (int) (Graphics.DEFAULT_PREVIEW_SIZE_ENTITY * config().scaleFactorEntity);
        int playerWidth = (int) (width + playerSize * player().getWidth() - 1);
        int playerHeight = (int) (height + playerSize * player().getHeight() - 1);

        playerPreviewTooltipComponent = new PlayerPreviewTooltipComponent(
                player(), stack,
                playerWidth, playerHeight,
                playerSize,
                config().spinPlayerPreview
        );

        double scaleFactorItem = config().scaleFactorItem;
        int itemSize = (int) (Graphics.DEFAULT_PREVIEW_SIZE_ITEM * scaleFactorItem);
        int itemWidth = (int) ((double) width / 2 * scaleFactorItem);
        int itemHeight = (int) ((double) height / 2 * scaleFactorItem);

        itemPreviewTooltipComponent = new ItemPreviewTooltipComponent(
                stack,
                itemWidth, itemHeight,
                itemSize
        );

        addTooltips();
    }

    protected void addTooltips() {
        if (!rprWidget.getCurrentTab().forCraftItemOnly) {
            MultiItemTooltipComponent component = multiItemTooltipComponent(rprWidget, rename);
            tooltipComponents.add(component);
        }
    }

    @Override
    public void onRenderTooltip(DrawContext context, int mouseX, int mouseY, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        ArrayList<TooltipComponent> tooltipAddition = new ArrayList<>();

        if (config().enablePreview) {
            boolean shiftDown = isShiftDown();

            if (!shiftDown && !config().playerPreviewByDefault) {
                if (!config().disableTooltipHints) tooltipAddition.add(tooltipOf(playerPreviewHintShift));
            } else if (shiftDown != config().playerPreviewByDefault) {
                if (!config().disableTooltipHints) tooltipAddition.add(tooltipOf(playerPreviewHintF));

                if (currentScreen() != null) currentScreen().setFocused(null);
            }
        }

        if (!config().disableTooltipHints) {
            tooltipAddition.add(tooltipOf(favoriteSupplier.get() ? favoriteHintRemove : favoriteHintAdd));
            tooltipAddition.add(tooltipOf(nextHintN));
            tooltipAddition.add(tooltipOf(disableHint));
        }

        tooltipComponents.addAll(tooltipAddition);

        updateRenameIndex();

        List<TooltipComponent> snapshot = new ArrayList<>(tooltipComponents);
        Graphics.drawTooltip(
                context,
                textRenderer(),
                snapshot,
                mouseX, mouseY,
                HoveredTooltipPositioner.INSTANCE
        );
        if (config().enablePreview) {
            drawPreview(context, mouseX, mouseY, snapshot);
        }

        tooltipComponents.removeAll(tooltipAddition);
    }

    @Override
    public void drawPreview(DrawContext context, int mouseX, int mouseY, List<TooltipComponent> mainTooltip) {
        boolean shouldPreviewPlayer = isShiftDown() != config().playerPreviewByDefault;
        TooltipPositioner positioner = new PreviewTooltipPositioner(mainTooltip);

        if (shouldPreviewPlayer) {
            playerPreview(context, mouseX, mouseY, positioner);
        } else {
            itemPreview(context, mouseX, mouseY, positioner);
        }
    }

    private void playerPreview(DrawContext context, int mouseX, int mouseY, TooltipPositioner positioner) {
        if (isFKeyJustPressed()) {
            playerPreviewTooltipComponent.cycleSlots(config().alwaysAllowPlayerPreviewHead);
        }

        Graphics.drawTooltipWithFixedBorders(
                context,
                textRenderer(),
                playerPreviewTooltipComponent,
                mouseX, mouseY,
                positioner,
                favoriteSupplier.get()
        );
    }

    private void itemPreview(DrawContext context, int mouseX, int mouseY, TooltipPositioner positioner) {
        Graphics.drawTooltipWithFixedBorders(
                context,
                textRenderer(),
                itemPreviewTooltipComponent,
                mouseX, mouseY,
                positioner,
                favoriteSupplier.get()
        );
    }

    private boolean fPressFuse = false;

    private boolean isFKeyJustPressed() {
        if (InputUtil.isKeyPressed(client().getWindow(), GLFW.GLFW_KEY_F)) {
            if (!fPressFuse) {
                fPressFuse = true;
                return true;
            }
        } else {
            fPressFuse = false;
        }
        return false;
    }
}
