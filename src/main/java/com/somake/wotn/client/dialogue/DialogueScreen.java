package com.somake.wotn.client.dialogue;

import com.somake.wotn.network.CloseDialogueRequestPayload;
import com.somake.wotn.network.SelectDialogueResponsePayload;
import com.somake.wotn.network.ShowDialoguePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class DialogueScreen extends Screen {
    private static final float TEXT_SCALE = 1.2F;
    private UUID sessionId;
    private Component speaker;
    private Component role;
    private Component dialogueText;
    private List<ResponseView> responses;
    private int visibleCharacters;
    private int revealTicker;
    private int selectedResponse;
    private boolean serverClosing;
    private boolean awaitingServer;

    public DialogueScreen(ShowDialoguePayload payload) {
        super(Component.translatable("screen.wotn.dialogue"));
        this.sessionId = payload.sessionId();
        this.speaker = Component.translatable(payload.speaker());
        this.role = payload.role().isBlank() ? Component.empty() : Component.translatable(payload.role());
        this.dialogueText = Component.translatable(payload.text());
        this.responses = toViews(payload.responses());
    }

    public void update(ShowDialoguePayload payload) {
        if (!this.sessionId.equals(payload.sessionId())) return;
        this.speaker = Component.translatable(payload.speaker());
        this.role = payload.role().isBlank() ? Component.empty() : Component.translatable(payload.role());
        this.dialogueText = Component.translatable(payload.text());
        this.responses = toViews(payload.responses());
        this.visibleCharacters = 0;
        this.revealTicker = 0;
        this.selectedResponse = firstAvailable();
        this.awaitingServer = false;
    }

    public UUID sessionId() {
        return this.sessionId;
    }

    public void closeFromServer() {
        this.serverClosing = true;
        this.onClose();
    }

    @Override
    protected void init() {
        this.selectedResponse = firstAvailable();
    }

    @Override
    public void tick() {
        super.tick();
        int total = codePointCount(this.dialogueText);
        if (this.visibleCharacters < total && ++this.revealTicker >= 1) {
            this.revealTicker = 0;
            this.visibleCharacters += 2;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x66081419);
        graphics.fillGradient(0, height / 2, width, height, 0x00101D23, 0xB508151A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Panel panel = panel();
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), 0xE30A151A);
        graphics.fillGradient(panel.x, panel.y, panel.right(), panel.y + 26, 0xD1264B56, 0x60111F24);
        graphics.outline(panel.x, panel.y, panel.width, panel.height, 0xFF4E8795);
        graphics.horizontalLine(panel.x + 10, panel.right() - 10, panel.y + 27, 0x8072D5EE);

        drawScaledText(graphics, this.speaker, panel.x + 13, panel.y + 9, 0xFFDDFBFF, true);
        if (!this.role.getString().isBlank()) {
            drawScaledText(graphics, this.role,
                    panel.right() - scaledTextWidth(this.role) - 13,
                    panel.y + 9, 0xFFD9A85F, false);
        }

        Component visible = visibleText();
        drawScaledWrappedText(graphics, visible, panel.x + 14, panel.y + 38,
                panel.width - 28, 0xFFBED4DA);

        if (!this.responses.isEmpty()) {
            int separatorY = panel.y + 57 + scaledWrappedHeight(this.dialogueText, panel.width - 28);
            graphics.horizontalLine(panel.x + 1, panel.right() - 1, separatorY, 0xA050838F);
        }

        if (isFullyRevealed()) drawResponses(graphics, panel, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawResponses(GuiGraphicsExtractor graphics, Panel panel, int mouseX, int mouseY) {
        List<ResponseBounds> bounds = responseBounds(panel);
        for (int i = 0; i < this.responses.size(); i++) {
            ResponseView response = this.responses.get(i);
            ResponseBounds bound = bounds.get(i);
            boolean hovered = bound.contains(mouseX, mouseY);
            boolean selected = i == this.selectedResponse;
            int color = !response.available ? 0xFF576A70
                    : selected || hovered ? 0xFFDDFBFF : 0xFF88AAB3;
            Component line = Component.literal((i + 1) + ". ").append(response.text);
            if (selected && response.available) {
                int markerHeight = scaledWrappedHeight(line, bound.width);
                graphics.fill(bound.x - 8, bound.y, bound.x - 5,
                        bound.y + markerHeight, 0xFFD9A85F);
            }
            drawScaledWrappedText(graphics, line, bound.x, bound.y, bound.width, color);
            if (!response.available && !response.requirement.getString().isBlank()) {
                drawScaledText(graphics,
                        Component.literal("[ ").append(response.requirement).append(" ]"),
                        bound.x, bound.y + bound.height - scaledLineHeight(), 0xFF8A6762, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (!isFullyRevealed()) {
            revealAll();
            return true;
        }
        List<ResponseBounds> bounds = responseBounds(panel());
        for (int i = 0; i < bounds.size(); i++) {
            if (bounds.get(i).contains(event.x(), event.y())) {
                choose(i);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFullyRevealed() && (event.isConfirmation() || event.isSelection())) {
            revealAll();
            return true;
        }
        int digit = event.getDigit();
        if (!isFullyRevealed() && digit >= 0) {
            revealAll();
            return true;
        }
        if (digit >= 1 && digit <= this.responses.size()) {
            choose(digit - 1);
            return true;
        }
        if (event.isUp()) {
            moveSelection(-1);
            return true;
        }
        if (event.isDown()) {
            moveSelection(1);
            return true;
        }
        if (event.isConfirmation() && this.selectedResponse >= 0) {
            choose(this.selectedResponse);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D && isFullyRevealed()) {
            moveSelection(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void choose(int index) {
        if (this.awaitingServer || index < 0 || index >= this.responses.size()
                || !this.responses.get(index).available) return;
        this.awaitingServer = true;
        this.selectedResponse = index;
        if (minecraft.player != null) minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.85F);
        ClientPacketDistributor.sendToServer(new SelectDialogueResponsePayload(
                this.sessionId, this.responses.get(index).id));
    }

    private void moveSelection(int direction) {
        if (this.responses.isEmpty()) return;
        int cursor = this.selectedResponse < 0 ? 0 : this.selectedResponse;
        for (int i = 0; i < this.responses.size(); i++) {
            cursor = Math.floorMod(cursor + direction, this.responses.size());
            if (this.responses.get(cursor).available) {
                this.selectedResponse = cursor;
                return;
            }
        }
    }

    private int firstAvailable() {
        for (int i = 0; i < this.responses.size(); i++) if (this.responses.get(i).available) return i;
        return -1;
    }

    @Override
    public void onClose() {
        if (!this.serverClosing) {
            ClientPacketDistributor.sendToServer(new CloseDialogueRequestPayload(this.sessionId));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private Panel panel() {
        int panelWidth = Math.min(width - 20, Math.max(300, width * 13 / 25));
        int panelX = (width - panelWidth) / 2;
        int responseWidth = panelWidth - 42;
        int dialogueHeight = scaledWrappedHeight(this.dialogueText, panelWidth - 28);
        int responseHeight = 0;
        for (int i = 0; i < this.responses.size(); i++) {
            responseHeight += responseHeight(i, this.responses.get(i), responseWidth);
        }
        int panelHeight = Math.min(height - 18, 88 + dialogueHeight + responseHeight);
        return new Panel(panelX, height - panelHeight - 14, panelWidth, panelHeight);
    }

    private List<ResponseBounds> responseBounds(Panel panel) {
        List<ResponseBounds> bounds = new ArrayList<>(this.responses.size());
        int y = panel.y + 76 + scaledWrappedHeight(this.dialogueText, panel.width - 28);
        int x = panel.x + 22;
        int responseWidth = panel.width - 42;
        for (int i = 0; i < this.responses.size(); i++) {
            ResponseView response = this.responses.get(i);
            int height = responseHeight(i, response, responseWidth);
            bounds.add(new ResponseBounds(x, y, responseWidth, height));
            y += height;
        }
        return bounds;
    }

    private int responseHeight(int index, ResponseView response, int width) {
        Component line = Component.literal((index + 1) + ". ").append(response.text);
        int textHeight = scaledWrappedHeight(line, width);
        return textHeight + (response.available || response.requirement.getString().isBlank()
                ? 8 : scaledLineHeight() + 7);
    }

    private int scaledTextWidth(Component text) {
        return (int) Math.ceil(font.width(text) * TEXT_SCALE);
    }

    private int scaledLineHeight() {
        return (int) Math.ceil(font.lineHeight * TEXT_SCALE);
    }

    private int scaledWrappedHeight(Component text, int width) {
        int unscaledWidth = Math.max(1, (int) Math.floor(width / TEXT_SCALE));
        return (int) Math.ceil(font.wordWrapHeight(text, unscaledWidth) * TEXT_SCALE);
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, Component text,
            int x, int y, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        graphics.text(font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    private void drawScaledWrappedText(GuiGraphicsExtractor graphics, Component text,
            int x, int y, int width, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        graphics.textWithWordWrap(font, text, 0, 0,
                Math.max(1, (int) Math.floor(width / TEXT_SCALE)), color, false);
        graphics.pose().popMatrix();
    }

    private Component visibleText() {
        String full = this.dialogueText.getString();
        int count = Math.min(this.visibleCharacters, full.codePointCount(0, full.length()));
        return Component.literal(full.substring(0, full.offsetByCodePoints(0, count)));
    }

    private boolean isFullyRevealed() {
        return this.visibleCharacters >= codePointCount(this.dialogueText);
    }

    private void revealAll() {
        this.visibleCharacters = codePointCount(this.dialogueText);
    }

    private static int codePointCount(Component component) {
        String text = component.getString();
        return text.codePointCount(0, text.length());
    }

    private static List<ResponseView> toViews(List<ShowDialoguePayload.Response> responses) {
        return responses.stream().map(response -> new ResponseView(response.id(),
                Component.translatable(response.text()), response.available(),
                response.requirement().isBlank() ? Component.empty()
                        : Component.translatable(response.requirement())))
                .toList();
    }

    private record ResponseView(String id, Component text, boolean available, Component requirement) {
    }

    private record Panel(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }

    private record ResponseBounds(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
