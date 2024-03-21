package net.totobirdcreations.iapi

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.item.ItemStack
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.awt.Color
import kotlin.math.max


internal class ImportScreen(
                addr  : String,
    private val stack : ItemStack
) : Screen(Text.translatable("${Main.ID}.screen.title")) {

    init {
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
    }
    private val itemRenderer = MinecraftClient.getInstance().itemRenderer;

    private val label : TextWidget = TextWidget(
        Text.translatable("${Main.ID}.screen.label"),
        this.textRenderer
    );
    private val addr : TextWidget = TextWidget(
        Text.literal(addr).setStyle(Style.EMPTY
            .withColor(Formatting.GRAY)
            .withUnderline(true)
        ),
        this.textRenderer
    );

    private val approve : ButtonWidget = ButtonWidget.builder(
        Text.translatable("${Main.ID}.screen.approve"),
        { _ -> this.approveImport(); }
    ).build();

    private val reject : ButtonWidget = ButtonWidget.builder(
        Text.translatable("${Main.ID}.screen.reject"),
        { _ -> this.close(); }
    ).build();



    override fun init() {
        val leftWidth = max(max(max(
            this.textRenderer.getWidth(this.label   .message),
            this.textRenderer.getWidth(this.addr    .message)
        ),  this.textRenderer.getWidth(this.approve .message) + 8
        ),  this.textRenderer.getWidth(this.reject  .message) + 8);

        this.label   .width = leftWidth;
        this.addr    .width = leftWidth;
        this.approve .width = leftWidth / 2 - 1;
        this.reject  .width = leftWidth / 2 - 1;

        this.label   .x = this.width  / 2 - leftWidth - 16;
        this.addr    .x = this.width  / 2 - leftWidth - 16;
        this.approve .x = this.width  / 2 - leftWidth - 16;
        this.reject  .x = this.approve.x + this.approve.width + 2;
        this.label   .y = this.height / 2 - this.label .height * 2;
        this.addr    .y = this.height / 2 - this.addr  .height;
        this.approve .y = this.height / 2 + this.label .height;
        this.reject  .y = this.approve.y;

        this.addDrawableChild(this.label   );
        this.addDrawableChild(this.addr    );
        this.addDrawableChild(this.approve );
        this.addDrawableChild(this.reject  );

    }


    override fun render(context : DrawContext, mouseX : Int, mouseY : Int, delta : Float) {
        super.render(context, mouseX, mouseY, delta);

        context.fill(
            this.width  / 2 - 9,
            this.height / 2 - 9,
            this.width  / 2 + 9,
            this.height / 2 + 9,
            Color(1.0f, 1.0f, 1.0f, 0.25f).rgb
        );
        context.drawItem(
            this.stack,
            this.width  / 2 - 8,
            this.height / 2 - 8
        );
        context.drawTooltip(
            this.textRenderer,
            this.stack.getTooltip(MinecraftClient.getInstance().player, TooltipContext.Default.ADVANCED),
            this.width  / 2 + 8,
            this.height / 2 + 7
        );
    }


    override fun close() {
        super.close();
        Server.resetOccupied();
    }


    private fun approveImport() {
        Server.approveImport(this.stack);
        this.close();
    }

}