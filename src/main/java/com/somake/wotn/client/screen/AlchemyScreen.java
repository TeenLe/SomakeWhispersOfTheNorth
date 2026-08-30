package com.somake.wotn.client.screen;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.alchemy.AlchemyRune;
import com.somake.wotn.alchemy.AlchemyPotionConfiguration;
import com.somake.wotn.client.renderer.state.ItemPreviewRenderState;
import com.somake.wotn.network.AlchemyActionPayload;
import com.somake.wotn.network.AlchemyFormulaSnapshot;
import com.somake.wotn.network.AlchemyRuneSnapshot;
import com.somake.wotn.network.CloseAlchemySessionPayload;
import com.somake.wotn.network.OpenAlchemyPayload;
import com.somake.wotn.network.SelectAlchemyFormulaPayload;
import com.somake.wotn.network.UpdateAlchemyPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2f;

public final class AlchemyScreen extends Screen {
    private final UUID sessionId;
    private List<AlchemyFormulaSnapshot> formulas;
    private List<AlchemyRuneSnapshot> runes;
    private String selectedFormulaId;
    private String activeAnalysisId;
    private String selectedStudyItemId = "";
    private String selectedRuneId = "";
    private RuneSelectionOrigin selectedRuneOrigin = RuneSelectionOrigin.NONE;
    private int selectedEquippedRuneIndex = -1;
    private int remainingTicks;
    private String messageKey;
    private Tab tab = Tab.LABORATORY;
    private int listScroll;
    private int detailScroll;
    private int age;
    private boolean closeSent;

    public AlchemyScreen(OpenAlchemyPayload payload) {
        super(Component.translatable("screen.wotn.alchemy.title"));
        sessionId = payload.sessionId();
        apply(payload);
    }

    public UUID sessionId() { return sessionId; }
    public void update(UpdateAlchemyPayload payload) {
        if (sessionId.equals(payload.snapshot().sessionId())) apply(payload.snapshot());
    }

    private void apply(OpenAlchemyPayload payload) {
        String previousFormulaId = selectedFormulaId;
        formulas = payload.formulas();
        runes = payload.runes();
        selectedFormulaId = payload.selectedFormulaId();
        activeAnalysisId = payload.activeResearchId();
        remainingTicks = payload.remainingTicks();
        messageKey = payload.messageKey();
        if (selected() == null || !isVisible(selected())) selectFirstVisible();
        if (previousFormulaId != null && !previousFormulaId.equals(selectedFormulaId)) {
            clearRuneSelection();
            detailScroll = 0;
        }
        else reconcileRuneSelection();
    }

    @Override public void tick() { age++; if (remainingTicks > 0) remainingTicks--; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB508151A);
        graphics.fillGradient(0, 0, width, height, 0x60366159, 0xC008151A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        drawFrame(g, l);
        drawHeader(g, l);
        drawFormulaList(g, l, mouseX, mouseY);
        AlchemyFormulaSnapshot formula = selected();
        clampDetailScroll(l, formula);
        if (formula == null || !isVisible(formula)) drawNoSelection(g, l);
        else if (tab == Tab.GRIMOIRE) drawGrimoireDetails(g, l, formula, mouseX, mouseY);
        else drawStudyDetails(g, l, formula, mouseX, mouseY);
        drawTabs(g, l, mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawFrame(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.x, l.y, l.right(), l.bottom(), 0xF00A1317);
        g.fillGradient(l.x, l.y, l.right(), l.y + 39, 0xEE31544E, 0x80203331);
        g.outline(l.x, l.y, l.width, l.height, 0xFF527F78);
        g.outline(l.x + 2, l.y + 2, l.width - 4, l.height - 4, 0x705FA99D);
        g.verticalLine(l.listRight, l.y + 39, l.tabY, 0x80618E87);
        g.horizontalLine(l.x, l.right(), l.tabY, 0x80618E87);
    }

    private void drawHeader(GuiGraphicsExtractor g, Layout l) {
        g.text(font, title, l.x + 14, l.y + 13, 0xFFE8FBF6, true);
        Component status = activeAnalysisId.isBlank()
                ? Component.translatable("screen.wotn.alchemy.study.status.empty")
                : Component.translatable("screen.wotn.alchemy.study.status.analyzing", formatTime(remainingTicks));
        g.text(font, status, l.right() - font.width(status) - 14, l.y + 13,
                activeAnalysisId.isBlank() ? 0xFF83A8A0 : 0xFFFFCD73, false);
        if (!messageKey.isBlank()) {
            Component message = Component.translatable(messageKey);
            g.centeredText(font, message, (l.x + l.right()) / 2, l.y + 27,
                    messageKey.contains("learned") || messageKey.contains("brewed") ? 0xFF8DDFBE : 0xFFE3B86B);
        }
    }

    private void drawTabs(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        int tabWidth = l.width / Tab.values().length;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab candidate = Tab.values()[i];
            int x0 = l.x + i * tabWidth;
            int x1 = i == Tab.values().length - 1 ? l.right() : x0 + tabWidth;
            boolean active = candidate == tab;
            boolean hover = mx >= x0 && mx <= x1 && my >= l.tabY && my <= l.bottom();
            g.fill(x0, l.tabY, x1, l.bottom(), active ? 0xD01A2F2D : hover ? 0x90203431 : 0xB00D191B);
            if (active) g.horizontalLine(x0 + 18, x1 - 18, l.tabY + 1, 0xFFFFC765);
            g.centeredText(font, Component.translatable(candidate.key), (x0 + x1) / 2,
                    l.tabY + 13, active ? 0xFFFFFFFF : 0xFF78938F);
        }
    }

    private void drawFormulaList(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        int x = l.x + 10;
        g.text(font, Component.translatable(tab == Tab.LABORATORY
                ? "screen.wotn.alchemy.study.list" : "screen.wotn.alchemy.grimoire.list"),
                x, l.y + 50, 0xFFD8C083, true);
        List<AlchemyFormulaSnapshot> visible = visibleFormulas();
        if (visible.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable(tab == Tab.LABORATORY
                    ? "screen.wotn.alchemy.study.empty" : "screen.wotn.alchemy.grimoire.empty"),
                    x, l.y + 75, l.listRight - x - 10, 0xFF6F8580, false);
            return;
        }
        clampScroll(l, visible.size(), 52);
        g.enableScissor(l.x + 5, viewportTop(l), l.listRight - 7, viewportBottom(l));
        for (int i = 0; i < visible.size(); i++) {
            AlchemyFormulaSnapshot formula = visible.get(i);
            Bounds b = new Bounds(l.x + 10, viewportTop(l) + i * 52 - listScroll,
                    l.listRight - l.x - 23, 45);
            boolean selected = formula.id().equals(selectedFormulaId);
            boolean hover = b.contains(mx, my);
            int color = stateColor(formula.state());
            g.fill(b.x, b.y, b.right(), b.bottom(), selected ? 0xC024403B : hover ? 0xA01C302D : 0x80101E1E);
            g.outline(b.x, b.y, b.width, b.height, selected ? 0xFFFFD17A : color);
            if (formula.state() == AlchemyFormulaSnapshot.State.LEARNED) {
                g.item(formula.result(), b.x + 8, b.y + 9);
                g.text(font, formula.result().getHoverName(), b.x + 31, b.y + 9, 0xFFE6F5F1, true);
                g.text(font, Component.translatable("screen.wotn.alchemy.tier", formula.tier()),
                        b.x + 31, b.y + 23, 0xFFD8B36B, false);
            } else {
                g.centeredText(font, Component.literal("?"), b.x + 16, b.y + 12, 0xFF879892);
                g.text(font, Component.translatable(formula.hiddenTitle()), b.x + 31, b.y + 9, 0xFFB7C8C3, true);
                g.text(font, Component.translatable("screen.wotn.alchemy.study.progress_short", percent(formula)),
                        b.x + 31, b.y + 23, color, false);
            }
            drawProgress(g, b.x + 8, b.bottom() - 6, b.width - 16, formula);
        }
        g.disableScissor();
        drawScrollbar(g, l, visible.size(), 52);
    }

    private void drawStudyDetails(GuiGraphicsExtractor g, Layout l, AlchemyFormulaSnapshot formula, int mx, int my) {
        int x = l.listRight + 12, right = l.right() - 12;
        g.enableScissor(l.listRight + 1, detailViewportTop(l), l.right() - 3, detailViewportBottom(l));
        g.text(font, Component.translatable(formula.hiddenTitle()), x, detailY(l, 53), 0xFFE5F4F0, true);
        g.text(font, Component.translatable("screen.wotn.alchemy.study.progress", formula.studiedUnits(),
                formula.totalStudyUnits(), percent(formula)), x, detailY(l, 69), 0xFFD8B56C, false);
        drawProgress(g, x, detailY(l, 84), right - x, formula);
        g.textWithWordWrap(font, Component.translatable(formula.hint()), x, detailY(l, 100), right - x,
                0xFF9CB5AF, false);
        g.text(font, Component.translatable("screen.wotn.alchemy.study.samples"), x, detailY(l, 151),
                0xFFDCEBE7, true);
        int startY = detailY(l, 167);
        for (int i = 0; i < formula.studyIngredients().size(); i++) {
            drawStudyIngredient(g, formula.studyIngredients().get(i), x, right, startY + i * 43, mx, my);
        }
        g.disableScissor();
        drawDetailScrollbar(g, l, formula);
        if (formula.state() == AlchemyFormulaSnapshot.State.ANALYZING) {
            g.centeredText(font, Component.translatable("screen.wotn.alchemy.study.analyzing", formatTime(remainingTicks)),
                    (x + right) / 2, l.tabY - 26, 0xFFFFCD73);
        } else {
            var selectedIngredient = selectedStudyIngredient(formula);
            boolean enabled = canSubmitStudyIngredient(selectedIngredient);
            Component label = selectedIngredient == null
                    ? Component.translatable("screen.wotn.alchemy.study.select_sample")
                    : selectedIngredient.owned() <= 0
                            ? Component.translatable("screen.wotn.alchemy.study.selected_missing")
                            : Component.translatable("screen.wotn.alchemy.study.submit_selected");
            drawButton(g, fullBottomButton(l), label, enabled, mx, my);
        }
    }

    private void drawStudyIngredient(GuiGraphicsExtractor g, AlchemyFormulaSnapshot.IngredientSnapshot ingredient,
            int x, int right, int y, int mx, int my) {
        boolean complete = ingredient.contributed() >= ingredient.required();
        Bounds b = new Bounds(x, y, right - x, 37);
        Identifier id = BuiltInItemId.of(ingredient.stack());
        boolean selected = id != null && id.toString().equals(selectedStudyItemId);
        g.fill(b.x, b.y, b.right(), b.bottom(), complete ? 0x70223D34
                : selected ? 0xB0334B3E : b.contains(mx, my) ? 0x90213631 : 0x70121F1D);
        g.outline(b.x, b.y, b.width, b.height, complete ? 0xFF74C8A7
                : selected ? 0xFFFFD47A : 0xFF405650);
        if (ingredient.revealed()) {
            g.item(ingredient.stack(), x + 7, y + 8);
            g.text(font, ingredient.stack().getHoverName(), x + 29, y + 7, 0xFFD5E5E0, false);
            g.text(font, Component.translatable("screen.wotn.alchemy.study.ingredient_count",
                    ingredient.contributed(), ingredient.required(), ingredient.owned()), x + 29, y + 21,
                    complete ? 0xFF79BFA4 : 0xFF899E98, false);
        } else {
            g.centeredText(font, Component.literal("?"), x + 15, y + 12, 0xFF65756F);
            g.text(font, Component.translatable("screen.wotn.alchemy.study.unknown_ingredient"),
                    x + 29, y + 13, 0xFF697B75, false);
        }
    }

    private void drawGrimoireDetails(GuiGraphicsExtractor g, Layout l, AlchemyFormulaSnapshot formula, int mx, int my) {
        int x = l.listRight + 12, right = l.right() - 12;
        g.enableScissor(l.listRight + 1, detailViewportTop(l), l.right() - 3, detailViewportBottom(l));
        g.fill(x, detailY(l, 49), right, detailY(l, 139), 0xA5101E1D);
        g.outline(x, detailY(l, 49), right - x, 90, 0xFF416B65);
        submitPotionPreview(g, formula, x + 2, detailY(l, 50), x + 88, detailY(l, 138));
        g.text(font, familyName(formula.family()), x + 94, detailY(l, 60), 0xFFF0FBF8, true);
        drawTierSelector(g, l, formula, mx, my);
        g.text(font, Component.translatable(formula.role()), x + 94, detailY(l, 110), 0xFF8BC8BC, false);

        g.text(font, Component.translatable("screen.wotn.alchemy.brewing_cost"), x, detailY(l, 158),
                0xFFDDEDEA, true);
        int ingredientRight = x + (right - x) * 41 / 100;
        for (int i = 0; i < formula.brewingIngredients().size(); i++) {
            var ingredient = formula.brewingIngredients().get(i);
            int y = detailY(l, 179 + i * 20);
            g.item(ingredient.stack(), x, y - 4);
            Component line = Component.literal(ingredient.owned() + " / " + ingredient.required() + "  ")
                    .append(ingredient.stack().getHoverName());
            g.text(font, font.split(line, ingredientRight - x - 22).getFirst(), x + 20, y,
                    ingredient.owned() >= ingredient.required() ? 0xFF8BDCB6 : 0xFFD17C70, false);
        }
        drawRuneLoadout(g, l, formula, ingredientRight + 12, right, mx, my);
        g.disableScissor();
        drawDetailScrollbar(g, l, formula);
        drawButton(g, brewOneBounds(l), Component.translatable("screen.wotn.alchemy.brew_one"), true, mx, my);
        drawButton(g, brewThreeBounds(l), Component.translatable("screen.wotn.alchemy.brew_three"), true, mx, my);
    }

    private void drawTierSelector(GuiGraphicsExtractor g, Layout l, AlchemyFormulaSnapshot formula, int mx, int my) {
        for (int tier = 1; tier <= 3; tier++) {
            AlchemyFormulaSnapshot candidate = learnedFormula(formula.family(), tier);
            Bounds b = tierBounds(l, tier);
            boolean selected = candidate != null && candidate.id().equals(formula.id());
            boolean enabled = candidate != null;
            g.fill(b.x, b.y, b.right(), b.bottom(), selected ? 0xD04A6042 : 0x80141F1D);
            g.outline(b.x, b.y, b.width, b.height, selected ? 0xFFFFD47A : enabled ? 0xFF5F8B80 : 0xFF2E3B38);
            g.centeredText(font, Component.literal(roman(tier)), b.centerX(), b.y + 6,
                    selected ? 0xFFFFE3A1 : enabled ? 0xFFADD8CF : 0xFF52615D);
        }
    }

    private void drawRuneLoadout(GuiGraphicsExtractor g, Layout l, AlchemyFormulaSnapshot formula,
            int x, int right, int mx, int my) {
        AlchemyPotionConfiguration config = formula.runeConfiguration();
        g.text(font, Component.translatable("screen.wotn.alchemy.runes.loadout"), x, detailY(l, 158),
                0xFFDDEDEA, true);
        Component slots = Component.translatable("screen.wotn.alchemy.runes.slots",
                config.occupiedSlots(), AlchemyPotionConfiguration.slotCapacity(formula.tier()));
        g.text(font, slots, right - font.width(slots), detailY(l, 158), 0xFFD8B76E, false);
        for (int slot = 0; slot < formula.tier(); slot++) {
            Bounds b = equippedSlotBounds(l, slot);
            g.fill(b.x, b.y, b.right(), b.bottom(), 0x80131F1D);
            g.outline(b.x, b.y, b.width, b.height, 0xFF52756C);
        }
        int cursor = 0;
        for (int i = 0; i < config.runes().size(); i++) {
            AlchemyRune rune = config.runes().get(i);
            Bounds b = equippedRuneBounds(l, cursor, rune.slots());
            AlchemyRuneSnapshot snapshot = runeSnapshot(rune);
            if (snapshot != null) drawCenteredItem(g, snapshot.stack(), b);
            g.outline(b.x, b.y, b.width, b.height,
                    selectedRuneOrigin == RuneSelectionOrigin.EQUIPPED
                            && i == selectedEquippedRuneIndex ? 0xFFFFFFFF : 0xFFFFD47A);
            cursor += rune.slots();
        }
        g.text(font, Component.translatable("screen.wotn.alchemy.runes.available"), x, detailY(l, 215),
                0xFF9EC8BF, true);
        List<AlchemyRuneSnapshot> available = availableRunes(formula.family());
        for (int i = 0; i < available.size(); i++) {
            AlchemyRuneSnapshot rune = available.get(i);
            Bounds b = availableRuneBounds(l, i);
            AlchemyRune type = AlchemyRune.fromId(rune.id());
            int availableCount = type == null ? 0 : availableRuneCount(formula, type, rune);
            boolean depleted = availableCount == 0;
            boolean canEquip = type != null && canEquip(formula, type);
            boolean selected = selectedRuneOrigin == RuneSelectionOrigin.INVENTORY
                    && rune.id().equals(selectedRuneId);
            g.fill(b.x, b.y, b.right(), b.bottom(), selected ? 0xB0345148
                    : b.contains(mx, my) ? 0xA02B433C : 0x70131F1D);
            g.outline(b.x, b.y, b.width, b.height, selected ? 0xFFFFD47A
                    : canEquip ? 0xFF4F8175 : 0xFF34433F);
            drawCenteredItem(g, rune.stack(), b);
            if (depleted) g.fill(b.x + 1, b.y + 1, b.right() - 1, b.bottom() - 1, 0xA00A1317);
            String count = Integer.toString(availableCount);
            g.text(font, Component.literal(count), b.right() - font.width(count) - 2, b.bottom() - 9,
                    depleted ? 0xFF687773 : 0xFFFFFFFF, true);
        }
        drawLoadoutRuneDetails(g, l, formula, x, right, mx, my);
    }

    private void drawLoadoutRuneDetails(GuiGraphicsExtractor g, Layout l, AlchemyFormulaSnapshot formula,
            int x, int right, int mx, int my) {
        AlchemyRuneSnapshot snapshot = selectedRuneSnapshot();
        AlchemyRune rune = snapshot == null ? null : AlchemyRune.fromId(snapshot.id());
        int y = detailY(l, 305);
        int panelBottom = detailY(l, runePanelNaturalBottom(l));
        g.fill(x, y, right, panelBottom, 0x60131F1D);
        g.outline(x, y, right - x, panelBottom - y, 0xFF3F5B54);
        if (snapshot == null || rune == null) {
            g.centeredText(font, Component.translatable("screen.wotn.alchemy.runes.select_detail"),
                    (x + right) / 2, y + 19, 0xFF6D8580);
            return;
        }
        g.item(snapshot.stack(), x + 8, y + 8);
        g.text(font, snapshot.stack().getHoverName(), x + 31, y + 7, 0xFFDDF6EF, true);
        g.text(font, Component.translatable("tooltip.wotn.rune.slots", rune.slots()),
                x + 31, y + 21, 0xFFD7B86E, false);
        g.textWithWordWrap(font, Component.translatable(rune.descriptionKey()),
                x + 8, y + 38, right - x - 16, 0xFF91AAA4, false);
        boolean removing = isSelectedEquippedRune(formula, rune);
        boolean enabled = removing || selectedRuneOrigin == RuneSelectionOrigin.INVENTORY
                && canEquip(formula, rune);
        Component label = Component.translatable(removing
                ? "screen.wotn.alchemy.runes.unequip" : "screen.wotn.alchemy.runes.equip");
        drawButton(g, loadoutRuneActionBounds(l), label, enabled, mx, my);
    }

    private void drawNoSelection(GuiGraphicsExtractor g, Layout l) {
        g.centeredText(font, Component.translatable("screen.wotn.alchemy.no_selection"),
                (l.listRight + l.right()) / 2, detailY(l, 150), 0xFF718681);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout l = layout();
        if (event.x() >= l.x && event.x() <= l.right()
                && event.y() >= l.tabY && event.y() <= l.bottom()) {
            int index = Math.clamp((int)((event.x() - l.x) * Tab.values().length / l.width), 0, Tab.values().length - 1);
            Tab selectedTab = Tab.values()[index];
            if (tab != selectedTab) clearRuneSelection();
            tab = selectedTab;
            listScroll = 0;
            detailScroll = 0;
            selectFirstVisible();
            playClick();
            return true;
        }
        List<AlchemyFormulaSnapshot> visible = visibleFormulas();
        for (int i = 0; i < visible.size(); i++) {
            Bounds b = new Bounds(l.x + 10, viewportTop(l) + i * 52 - listScroll,
                    l.listRight - l.x - 23, 45);
            if (event.y() >= viewportTop(l) && event.y() <= viewportBottom(l) && b.contains(event.x(), event.y())) {
                selectFormula(visible.get(i));
                return true;
            }
        }
        AlchemyFormulaSnapshot formula = selected();
        if (formula == null) return true;
        if (tab == Tab.LABORATORY) return handleStudyClick(l, formula, event) || super.mouseClicked(event, doubleClick);

        for (int tier = 1; tier <= 3; tier++) {
            AlchemyFormulaSnapshot candidate = learnedFormula(formula.family(), tier);
            if (candidate != null && detailViewportContains(l, event.x(), event.y())
                    && tierBounds(l, tier).contains(event.x(), event.y())) {
                selectFormula(candidate);
                return true;
            }
        }
        if (handleRuneLoadoutClick(l, formula, event)) return true;
        if (brewOneBounds(l).contains(event.x(), event.y())) {
            sendAction(formula, "", AlchemyActionPayload.Action.BREW_ONE); return true;
        }
        if (brewThreeBounds(l).contains(event.x(), event.y())) {
            sendAction(formula, "", AlchemyActionPayload.Action.BREW_THREE); return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleStudyClick(Layout l, AlchemyFormulaSnapshot formula, MouseButtonEvent event) {
        int x = l.listRight + 12, right = l.right() - 12, startY = detailY(l, 167);
        for (int i = 0; i < formula.studyIngredients().size(); i++) {
            var ingredient = formula.studyIngredients().get(i);
            Bounds b = new Bounds(x, startY + i * 43, right - x, 37);
            if (detailViewportContains(l, event.x(), event.y())
                    && b.contains(event.x(), event.y()) && ingredient.revealed()
                    && ingredient.contributed() < ingredient.required()) {
                Identifier itemId = BuiltInItemId.of(ingredient.stack());
                if (itemId != null) { selectedStudyItemId = itemId.toString(); playClick(); }
                return true;
            }
        }
        var selectedIngredient = selectedStudyIngredient(formula);
        if (fullBottomButton(l).contains(event.x(), event.y())
                && canSubmitStudyIngredient(selectedIngredient)) {
            sendAction(formula, selectedStudyItemId, AlchemyActionPayload.Action.SUBMIT_INGREDIENT);
            return true;
        }
        return false;
    }

    private boolean handleRuneLoadoutClick(Layout l, AlchemyFormulaSnapshot formula, MouseButtonEvent event) {
        AlchemyPotionConfiguration config = formula.runeConfiguration();
        int cursor = 0;
        for (int i = 0; i < config.runes().size(); i++) {
            AlchemyRune rune = config.runes().get(i);
            Bounds b = equippedRuneBounds(l, cursor, rune.slots());
            if (detailViewportContains(l, event.x(), event.y()) && b.contains(event.x(), event.y())) {
                selectedRuneId = rune.id();
                selectedRuneOrigin = RuneSelectionOrigin.EQUIPPED;
                selectedEquippedRuneIndex = i;
                playClick();
                return true;
            }
            cursor += rune.slots();
        }
        List<AlchemyRuneSnapshot> available = availableRunes(formula.family());
        for (int i = 0; i < available.size(); i++) {
            AlchemyRuneSnapshot snapshot = available.get(i);
            AlchemyRune rune = AlchemyRune.fromId(snapshot.id());
            if (rune != null && detailViewportContains(l, event.x(), event.y())
                    && availableRuneBounds(l, i).contains(event.x(), event.y())) {
                selectedRuneId = rune.id();
                selectedRuneOrigin = RuneSelectionOrigin.INVENTORY;
                selectedEquippedRuneIndex = -1;
                playClick();
                return true;
            }
        }
        AlchemyRuneSnapshot selected = selectedRuneSnapshot();
        AlchemyRune rune = selected == null ? null : AlchemyRune.fromId(selected.id());
        if (rune != null && detailViewportContains(l, event.x(), event.y())
                && loadoutRuneActionBounds(l).contains(event.x(), event.y())) {
            if (isSelectedEquippedRune(formula, rune)) {
                sendAction(formula, rune.id(), AlchemyActionPayload.Action.UNEQUIP_RUNE);
                clearRuneSelection();
                return true;
            }
            if (selectedRuneOrigin == RuneSelectionOrigin.INVENTORY && canEquip(formula, rune)) {
                sendAction(formula, rune.id(), AlchemyActionPayload.Action.EQUIP_RUNE);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        Layout l = layout();
        if (sy != 0.0D && mx >= l.x && mx <= l.listRight
                && my >= viewportTop(l) && my <= viewportBottom(l)) {
            int count = visibleFormulas().size();
            int max = maxScroll(l, count, 52);
            listScroll = Math.clamp(listScroll - (int)Math.signum(sy) * 39, 0, max);
            return true;
        }
        AlchemyFormulaSnapshot formula = selected();
        if (sy != 0.0D && detailViewportContains(l, mx, my) && formula != null) {
            detailScroll = Math.clamp(detailScroll - (int)Math.signum(sy) * 30,
                    0, maxDetailScroll(l, formula));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private boolean canEquip(AlchemyFormulaSnapshot formula, AlchemyRune rune) {
        AlchemyPotionConfiguration config = formula.runeConfiguration();
        AlchemyRuneSnapshot snapshot = runeSnapshot(rune);
        if (snapshot == null || !rune.family().equals(formula.family())) return false;
        if (availableRuneCount(formula, rune, snapshot) <= 0) return false;
        if (rune.prerequisite() != null && !config.has(rune.prerequisite())) return false;
        return config.equip(rune, formula.family(), formula.tier()) != config;
    }

    private void selectFormula(AlchemyFormulaSnapshot formula) {
        if (!formula.id().equals(selectedFormulaId)) clearRuneSelection();
        selectedFormulaId = formula.id();
        selectedStudyItemId = "";
        detailScroll = 0;
        ClientPacketDistributor.sendToServer(new SelectAlchemyFormulaPayload(sessionId, formula.id()));
        playClick();
    }

    private void sendAction(AlchemyFormulaSnapshot formula, String itemId, AlchemyActionPayload.Action action) {
        ClientPacketDistributor.sendToServer(new AlchemyActionPayload(sessionId, formula.id(), itemId, action));
        playClick();
    }

    private List<AlchemyFormulaSnapshot> visibleFormulas() {
        if (tab == Tab.LABORATORY) return formulas.stream()
                .filter(formula -> formula.state() != AlchemyFormulaSnapshot.State.LEARNED).toList();
        Map<String, AlchemyFormulaSnapshot> families = new LinkedHashMap<>();
        for (String family : List.of("jormungandr", "fenrir", "niflheim", "idunn")) {
            AlchemyFormulaSnapshot preferred = formulas.stream()
                    .filter(formula -> formula.family().equals(family)
                            && formula.state() == AlchemyFormulaSnapshot.State.LEARNED)
                    .max(java.util.Comparator.comparingInt(AlchemyFormulaSnapshot::tier)).orElse(null);
            AlchemyFormulaSnapshot selected = selected();
            if (selected != null && selected.family().equals(family)
                    && selected.state() == AlchemyFormulaSnapshot.State.LEARNED) preferred = selected;
            if (preferred != null) families.put(family, preferred);
        }
        return List.copyOf(families.values());
    }

    private boolean isVisible(AlchemyFormulaSnapshot formula) {
        return visibleFormulas().stream().anyMatch(candidate -> candidate.id().equals(formula.id()));
    }

    private void selectFirstVisible() {
        List<AlchemyFormulaSnapshot> visible = visibleFormulas();
        if (visible.stream().noneMatch(formula -> formula.id().equals(selectedFormulaId))) {
            selectedFormulaId = visible.stream().findFirst().map(AlchemyFormulaSnapshot::id).orElse("");
        }
    }

    private AlchemyFormulaSnapshot selected() {
        return formulas.stream().filter(formula -> formula.id().equals(selectedFormulaId)).findFirst().orElse(null);
    }

    private AlchemyFormulaSnapshot learnedFormula(String family, int tier) {
        return formulas.stream().filter(formula -> formula.family().equals(family) && formula.tier() == tier
                && formula.state() == AlchemyFormulaSnapshot.State.LEARNED).findFirst().orElse(null);
    }

    private AlchemyRuneSnapshot runeSnapshot(AlchemyRune rune) {
        return runes.stream().filter(snapshot -> snapshot.id().equals(rune.id())).findFirst().orElse(null);
    }

    private AlchemyRuneSnapshot selectedRuneSnapshot() {
        return runes.stream().filter(snapshot -> snapshot.id().equals(selectedRuneId)).findFirst().orElse(null);
    }

    private int availableRuneCount(AlchemyFormulaSnapshot formula, AlchemyRune rune,
            AlchemyRuneSnapshot snapshot) {
        if (!snapshot.id().equals(rune.id())) return 0;
        return Math.max(0, snapshot.owned() - formula.runeConfiguration().count(rune));
    }

    private boolean isSelectedEquippedRune(AlchemyFormulaSnapshot formula, AlchemyRune rune) {
        if (selectedRuneOrigin != RuneSelectionOrigin.EQUIPPED || selectedEquippedRuneIndex < 0) return false;
        List<AlchemyRune> equipped = formula.runeConfiguration().runes();
        return selectedEquippedRuneIndex < equipped.size()
                && equipped.get(selectedEquippedRuneIndex) == rune
                && rune.id().equals(selectedRuneId);
    }

    private void reconcileRuneSelection() {
        if (selectedRuneOrigin == RuneSelectionOrigin.NONE) return;
        AlchemyFormulaSnapshot formula = selected();
        AlchemyRuneSnapshot snapshot = selectedRuneSnapshot();
        AlchemyRune rune = snapshot == null ? null : AlchemyRune.fromId(snapshot.id());
        if (formula == null || rune == null || !rune.family().equals(formula.family())) {
            clearRuneSelection();
            return;
        }
        if (selectedRuneOrigin == RuneSelectionOrigin.INVENTORY && snapshot.owned() <= 0) {
            clearRuneSelection();
            return;
        }
        if (selectedRuneOrigin == RuneSelectionOrigin.EQUIPPED && !isSelectedEquippedRune(formula, rune)) {
            clearRuneSelection();
        }
    }

    private void clearRuneSelection() {
        selectedRuneId = "";
        selectedRuneOrigin = RuneSelectionOrigin.NONE;
        selectedEquippedRuneIndex = -1;
    }

    private List<AlchemyRuneSnapshot> availableRunes(String family) {
        return runes.stream().filter(snapshot -> snapshot.owned() > 0)
                .filter(snapshot -> {
                    AlchemyRune rune = AlchemyRune.fromId(snapshot.id());
                    return rune != null && rune.family().equals(family);
                }).toList();
    }

    private AlchemyFormulaSnapshot.IngredientSnapshot selectedStudyIngredient(AlchemyFormulaSnapshot formula) {
        return formula.studyIngredients().stream().filter(ingredient -> {
            Identifier id = BuiltInItemId.of(ingredient.stack());
            return id != null && id.toString().equals(selectedStudyItemId);
        }).findFirst().orElse(null);
    }

    private boolean canSubmitStudyIngredient(AlchemyFormulaSnapshot.IngredientSnapshot ingredient) {
        return ingredient != null && ingredient.revealed()
                && ingredient.contributed() < ingredient.required()
                && ingredient.owned() > 0 && activeAnalysisId.isBlank();
    }

    private Component familyName(String family) {
        return Component.translatable("screen.wotn.alchemy.family." + family);
    }

    private Bounds tierBounds(Layout l, int tier) {
        return new Bounds(l.listRight + 106 + (tier - 1) * 38, detailY(l, 76), 32, 22);
    }

    private Bounds equippedSlotBounds(Layout l, int slot) {
        Bounds base = runeAreaBounds(l);
        return new Bounds(base.x + slot * 34, detailY(l, 178), 30, 30);
    }

    private Bounds equippedRuneBounds(Layout l, int slot, int slots) {
        Bounds first = equippedSlotBounds(l, slot);
        return new Bounds(first.x, first.y, first.width + (slots - 1) * 34, first.height);
    }

    private Bounds availableRuneBounds(Layout l, int index) {
        Bounds base = runeAreaBounds(l);
        return new Bounds(base.x + (index % 3) * 34, detailY(l, 234 + (index / 3) * 34), 30, 30);
    }

    private void drawCenteredItem(GuiGraphicsExtractor g, ItemStack stack, Bounds bounds) {
        int itemSize = 16;
        g.item(stack, bounds.x + (bounds.width - itemSize) / 2,
                bounds.y + (bounds.height - itemSize) / 2);
    }

    private Bounds runeAreaBounds(Layout l) {
        int detailsX = l.listRight + 12, right = l.right() - 12;
        int ingredientRight = detailsX + (right - detailsX) * 41 / 100;
        return new Bounds(ingredientRight + 12, l.y + 158, right - ingredientRight - 12, 145);
    }

    private Bounds loadoutRuneActionBounds(Layout l) {
        Bounds area = runeAreaBounds(l);
        return new Bounds(area.x + 8, detailY(l, runeActionNaturalY(l)), area.width - 16, 23);
    }

    private int detailY(Layout l, int naturalY) {
        return l.y + naturalY - detailScroll;
    }

    private int detailViewportTop(Layout l) {
        return l.y + 49;
    }

    private int detailViewportBottom(Layout l) {
        return l.tabY - 42;
    }

    private boolean detailViewportContains(Layout l, double mx, double my) {
        return mx >= l.listRight + 1 && mx <= l.right() - 3
                && my >= detailViewportTop(l) && my <= detailViewportBottom(l);
    }

    private int runeActionNaturalY(Layout l) {
        AlchemyRuneSnapshot snapshot = selectedRuneSnapshot();
        AlchemyRune rune = snapshot == null ? null : AlchemyRune.fromId(snapshot.id());
        if (rune == null) return 380;
        Bounds area = runeAreaBounds(l);
        int descriptionHeight = font.wordWrapHeight(Component.translatable(rune.descriptionKey()),
                Math.max(1, area.width - 16));
        return Math.max(380, 343 + descriptionHeight + 8);
    }

    private int runePanelNaturalBottom(Layout l) {
        return runeActionNaturalY(l) + 35;
    }

    private int detailContentBottom(Layout l, AlchemyFormulaSnapshot formula) {
        if (formula == null) return 150;
        if (tab == Tab.GRIMOIRE) return runePanelNaturalBottom(l);
        int ingredientBottom = formula.studyIngredients().isEmpty()
                ? 167 : 167 + (formula.studyIngredients().size() - 1) * 43 + 37;
        return Math.max(204, ingredientBottom + 12);
    }

    private int maxDetailScroll(Layout l, AlchemyFormulaSnapshot formula) {
        int visibleBottom = detailViewportBottom(l) - l.y;
        return Math.max(0, detailContentBottom(l, formula) - visibleBottom);
    }

    private void clampDetailScroll(Layout l, AlchemyFormulaSnapshot formula) {
        detailScroll = Math.clamp(detailScroll, 0, maxDetailScroll(l, formula));
    }

    private void drawDetailScrollbar(GuiGraphicsExtractor g, Layout l, AlchemyFormulaSnapshot formula) {
        int max = maxDetailScroll(l, formula);
        if (max <= 0) return;
        int top = detailViewportTop(l), bottom = detailViewportBottom(l), height = bottom - top;
        int contentHeight = Math.max(height, detailContentBottom(l, formula) - 49);
        int thumb = Math.min(height, Math.max(20, height * height / contentHeight));
        int y = top + (height - thumb) * detailScroll / max;
        g.fill(l.right() - 7, top, l.right() - 4, bottom, 0x80132120);
        g.fill(l.right() - 7, y, l.right() - 4, y + thumb, 0xFF73B8AA);
    }

    private Bounds fullBottomButton(Layout l) {
        return new Bounds(l.listRight + 12, l.tabY - 35, l.right() - l.listRight - 24, 24);
    }

    private Bounds brewOneBounds(Layout l) {
        int x = l.listRight + 12, available = l.right() - x - 12;
        return new Bounds(x, l.tabY - 35, available / 2 - 3, 24);
    }

    private Bounds brewThreeBounds(Layout l) {
        Bounds one = brewOneBounds(l);
        return new Bounds(one.right() + 6, one.y, one.width, one.height);
    }

    private void drawButton(GuiGraphicsExtractor g, Bounds b, Component label, boolean enabled, int mx, int my) {
        boolean hover = enabled && b.contains(mx, my);
        g.fill(b.x, b.y, b.right(), b.bottom(), enabled ? hover ? 0xE04C6D54 : 0xD0375544 : 0xB0182523);
        g.outline(b.x, b.y, b.width, b.height, enabled ? hover ? 0xFFFFD47A : 0xFF76B99C : 0xFF3B4B47);
        g.centeredText(font, label, b.centerX(), b.y + 8, enabled ? 0xFFF1FBF7 : 0xFF667773);
    }

    private void drawProgress(GuiGraphicsExtractor g, int x, int y, int width, AlchemyFormulaSnapshot formula) {
        g.fill(x, y, x + width, y + 4, 0xFF14231F);
        g.fill(x, y, x + width * formula.studiedUnits() / Math.max(1, formula.totalStudyUnits()), y + 4,
                formula.state() == AlchemyFormulaSnapshot.State.LEARNED ? 0xFF70C7A7 : 0xFFD0AE63);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, Layout l, int count, int rowHeight) {
        int max = maxScroll(l, count, rowHeight);
        if (max <= 0) return;
        int top = viewportTop(l), bottom = viewportBottom(l), height = bottom - top;
        int content = Math.max(1, count * rowHeight - 7);
        int thumb = Math.max(24, height * height / Math.max(height, content));
        int y = top + (height - thumb) * listScroll / max;
        g.fill(l.listRight - 8, top, l.listRight - 5, bottom, 0x80132120);
        g.fill(l.listRight - 8, y, l.listRight - 5, y + thumb, 0xFF73B8AA);
    }

    private int viewportTop(Layout l) { return l.y + 67; }
    private int viewportBottom(Layout l) { return l.tabY - 5; }
    private int maxScroll(Layout l, int count, int rowHeight) {
        return Math.max(0, Math.max(0, count * rowHeight - 7) - (viewportBottom(l) - viewportTop(l)));
    }
    private void clampScroll(Layout l, int count, int rowHeight) {
        listScroll = Math.clamp(listScroll, 0, maxScroll(l, count, rowHeight));
    }
    private int percent(AlchemyFormulaSnapshot formula) {
        return formula.studiedUnits() * 100 / Math.max(1, formula.totalStudyUnits());
    }
    private int stateColor(AlchemyFormulaSnapshot.State state) {
        return switch (state) {
            case HIDDEN -> 0xFF52645E;
            case STUDYING -> 0xFFD0AE63;
            case ANALYZING -> 0xFFFFD078;
            case LEARNED -> 0xFF76D2AE;
        };
    }
    private static String roman(int tier) { return tier == 1 ? "I" : tier == 2 ? "II" : "III"; }
    private static String formatTime(int ticks) {
        int seconds = Math.max(0, (ticks + 19) / 20);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void submitPotionPreview(GuiGraphicsExtractor g, AlchemyFormulaSnapshot formula,
            int x0, int y0, int x1, int y1) {
        Identifier id = Identifier.tryParse(formula.id());
        if (id == null) return;
        ItemStack preview = formula.result().copyWithCount(1);
        preview.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(
                WhispersOfTheNorth.MODID, "alchemy_preview/" + id.getPath()));
        ItemStackRenderState state = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(state, preview, ItemDisplayContext.FIXED,
                minecraft.level, minecraft.player, 0);
        g.submitPictureInPictureRenderState(new ItemPreviewRenderState(state, -18.0F, age * 0.8F, -10.0F,
                x0, y0, x1, y1, 44.0F, new Matrix3x2f(g.pose()), g.peekScissorStack()));
    }

    private void playClick() {
        if (minecraft.player != null) minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 0.9F);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.isLeft() || event.isRight()) {
            int delta = event.isLeft() ? -1 : 1;
            tab = Tab.values()[Math.floorMod(tab.ordinal() + delta, Tab.values().length)];
            clearRuneSelection();
            listScroll = 0;
            detailScroll = 0;
            selectFirstVisible();
            return true;
        }
        return super.keyPressed(event);
    }

    private void closeSession() {
        if (!closeSent) {
            closeSent = true;
            ClientPacketDistributor.sendToServer(new CloseAlchemySessionPayload(sessionId));
        }
    }
    @Override public void onClose() { closeSession(); super.onClose(); }
    @Override public void removed() { closeSession(); super.removed(); }

    private Layout layout() {
        int w = Math.min(900, Math.max(600, width - 18));
        int h = Math.min(500, Math.max(340, height - 14));
        w = Math.min(w, width - 8); h = Math.min(h, height - 8);
        int x = (width - w) / 2, y = (height - h) / 2;
        int preferredListWidth = w * 35 / 100;
        int listWidth = w >= 600 ? Math.max(210, preferredListWidth)
                : Math.clamp(preferredListWidth, 140, Math.max(140, w - 244));
        return new Layout(x, y, w, h, x + listWidth, y + h - 43);
    }

    private enum Tab {
        LABORATORY("screen.wotn.alchemy.tab.laboratory"),
        GRIMOIRE("screen.wotn.alchemy.tab.grimoire");
        private final String key;
        Tab(String key) { this.key = key; }
    }
    private enum RuneSelectionOrigin { NONE, INVENTORY, EQUIPPED }
    private record Layout(int x, int y, int width, int height, int listRight, int tabY) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }
    private record Bounds(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        int centerX() { return x + width / 2; }
        boolean contains(double mx, double my) { return mx >= x && mx <= right() && my >= y && my <= bottom(); }
    }
    private static final class BuiltInItemId {
        private static Identifier of(ItemStack stack) {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        }
    }
}
