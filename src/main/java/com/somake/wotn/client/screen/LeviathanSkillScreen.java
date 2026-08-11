package com.somake.wotn.client.screen;

import com.somake.wotn.client.LeviathanSkillSelection;
import com.somake.wotn.client.renderer.state.ItemPreviewRenderState;
import com.somake.wotn.network.CloseForgeSessionPayload;
import com.somake.wotn.network.ForgeWeaponSnapshot;
import com.somake.wotn.network.OpenLeviathanSkillsPayload;
import com.somake.wotn.network.SelectForgeWeaponPayload;
import com.somake.wotn.network.SelectLeviathanSkillPayload;
import com.somake.wotn.network.UnlockSkillNodePayload;
import com.somake.wotn.network.UpdateForgeSessionPayload;
import com.somake.wotn.skilltree.LeviathanSkillTree;
import com.somake.wotn.skilltree.WeaponSkillProgress;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2f;

public final class LeviathanSkillScreen extends Screen {
    private static final int NODE_RADIUS = 12;

    private final UUID sessionId;
    private List<ForgeWeaponSnapshot> weapons;
    private UUID selectedWeaponId;
    private Tab tab = Tab.PROGRESSION;
    private int selectedNode = LeviathanSkillTree.THROW;
    private int selectedLoadoutSlot = 2;
    private double treePanX;
    private double treePanY;
    private int age;
    private int feedbackTicks;
    private boolean serverClosing;
    private boolean closeSent;

    public LeviathanSkillScreen(OpenLeviathanSkillsPayload payload) {
        super(Component.translatable("screen.wotn.mastery.title"));
        this.sessionId = payload.sessionId();
        this.weapons = payload.weapons();
        this.selectedWeaponId = payload.selectedWeaponId();
    }

    public UUID sessionId() { return sessionId; }

    public void update(UpdateForgeSessionPayload payload) {
        if (!sessionId.equals(payload.sessionId())) return;
        this.weapons = payload.weapons();
        this.selectedWeaponId = payload.selectedWeaponId();
        this.feedbackTicks = 18;
    }

    @Override public void tick() { age++; if (feedbackTicks > 0) feedbackTicks--; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB20A171D);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout l = layout();
        float time = age + partialTick;
        drawFrame(graphics, l);
        drawHeader(graphics, l);
        drawWeaponRail(graphics, l, mouseX, mouseY, time);
        if (selected() != null) {
            drawTabs(graphics, l, mouseX, mouseY);
            if (tab == Tab.PROGRESSION) drawTree(graphics, l, mouseX, mouseY, time);
            else drawLoadout(graphics, l, mouseX, mouseY, time);
            drawDetails(graphics, l);
        } else {
            drawEmptyState(graphics, l);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFrame(GuiGraphicsExtractor g, Layout l) {
        g.fill(l.x, l.y, l.right(), l.bottom(), 0xE80A1317);
        g.fillGradient(l.x, l.y, l.right(), l.y + 35, 0xD1244550, 0x60111F24);
        g.outline(l.x, l.y, l.width, l.height, 0xFF456F7B);
        g.outline(l.x + 2, l.y + 2, l.width - 4, l.height - 4, 0x6A79C8DA);
        g.verticalLine(l.railRight, l.y + 35, l.contentBottom, 0x8050838F);
        g.horizontalLine(l.x, l.right(), l.contentBottom, 0x8050838F);
    }

    private void drawHeader(GuiGraphicsExtractor g, Layout l) {
        g.text(font, title, l.x + 14, l.y + 12, 0xFFDDFBFF, true);
        ForgeWeaponSnapshot weapon = selected();
        if (weapon == null) {
            g.centeredText(font, Component.translatable("screen.wotn.mastery.no_weapon"),
                    l.x + l.width / 2, l.y + 13, 0xFF738A91);
            return;
        }
        WeaponSkillProgress p = weapon.progress();
        int cx = l.x + l.width / 2;
        g.centeredText(font, Component.translatable("screen.wotn.mastery.level", p.masteryLevel()), cx, l.y + 8, 0xFFB6DAE2);
        int bx = cx - 47, by = l.y + 23;
        g.fill(bx, by, bx + 94, by + 3, 0xFF13272E);
        if (p.masteryLevel() >= WeaponSkillProgress.MAX_MASTERY_LEVEL) g.fill(bx, by, bx + 94, by + 3, 0xFFD9A85F);
        else {
            float f = p.masteryXp() / (float) WeaponSkillProgress.xpRequired(p.masteryLevel());
            g.fill(bx, by, bx + Mth.floor(94 * f), by + 3, 0xFF72D5EE);
            g.centeredText(font, Component.translatable("screen.wotn.mastery.xp", p.masteryXp(),
                    WeaponSkillProgress.xpRequired(p.masteryLevel())), cx, by + 5, 0xFF66838B);
        }
        Component points = Component.translatable("screen.wotn.mastery.points", p.points());
        g.text(font, points, l.right() - font.width(points) - 14, l.y + 12, 0xFFD9A85F, true);
    }

    private void drawWeaponRail(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY, float time) {
        int cx = l.x + (l.railRight - l.x) / 2;
        g.centeredText(font, Component.translatable("screen.wotn.mastery.weapons"), cx, l.y + 45, 0xFF7898A3);
        int y = l.y + 61;
        for (ForgeWeaponSnapshot weapon : weapons) {
            boolean selected = weapon.weaponId().equals(selectedWeaponId);
            boolean hovered = mouseX >= l.x + 9 && mouseX <= l.railRight - 8 && mouseY >= y && mouseY <= y + 70;
            g.fill(l.x + 9, y, l.railRight - 8, y + 70, selected ? 0xA51A333B : 0x70101F24);
            g.outline(l.x + 9, y, l.railRight - l.x - 17, 70,
                    selected ? 0xFF69B7C8 : hovered ? 0xFF527F89 : 0xFF29434A);
            submitWeaponPreview(g, weapon.stack(), l.x + 13, y + 2, l.railRight - 12, y + 48,
                    34.0F, -18.0F, time * 0.65F, -28.0F);
            g.centeredText(font, weapon.stack().getHoverName(), cx, y + 52,
                    selected ? 0xFFDDFBFF : 0xFF819CA3);
            g.centeredText(font, Component.translatable("screen.wotn.mastery.level", weapon.progress().masteryLevel()),
                    cx, y + 62, selected ? 0xFFD9A85F : 0xFF576C72);
            y += 78;
        }
        if (weapons.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("screen.wotn.mastery.no_eligible_weapons"),
                    l.x + 14, l.y + 68, l.railRight - l.x - 28, 0xFF657D84, false);
        }
    }

    private void drawEmptyState(GuiGraphicsExtractor g, Layout l) {
        int cx = (l.railRight + l.right()) / 2;
        g.centeredText(font, Component.translatable("screen.wotn.mastery.empty_title"), cx, l.y + 125, 0xFFDDFBFF);
        g.textWithWordWrap(font, Component.translatable("screen.wotn.mastery.empty_description"),
                cx - 130, l.y + 145, 260, 0xFF7F9BA3, false);
    }

    private void drawTabs(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        int center = (l.railRight + l.right()) / 2, y = l.y + 46;
        int gap = Math.min(92, (l.right() - l.railRight) / 4);
        drawTab(g, Tab.PROGRESSION, center - gap, y, mx, my);
        drawTab(g, Tab.LOADOUT, center + gap, y, mx, my);
    }

    private void drawTab(GuiGraphicsExtractor g, Tab candidate, int x, int y, int mx, int my) {
        boolean active = tab == candidate, hover = Math.abs(mx - x) < 52 && my >= y - 5 && my <= y + 13;
        g.centeredText(font, Component.translatable(candidate.key), x, y,
                active ? 0xFFDDFBFF : hover ? 0xFF9DCBD5 : 0xFF607E86);
        if (active) g.horizontalLine(x - 42, x + 42, y + 12, 0xFFD9A85F);
    }

    private void drawTree(GuiGraphicsExtractor g, Layout l, int mx, int my, float time) {
        TreeViewport v = treeViewport(l);
        int ox = v.centerX() + Mth.floor(treePanX), oy = v.centerY() - 16 + Mth.floor(treePanY);
        WeaponSkillProgress p = progress();
        g.enableScissor(v.left, v.top, v.right, v.bottom);
        for (LeviathanSkillTree.Node n : LeviathanSkillTree.NODES) if (n.id() != LeviathanSkillTree.ROOT) {
            LeviathanSkillTree.Node parent = LeviathanSkillTree.byId(n.parentId());
            drawConnection(g, ox + parent.treeX(), oy + parent.treeY(), ox + n.treeX(), oy + n.treeY(),
                    p.isUnlocked(parent.id()), p.isUnlocked(n.id()));
        }
        for (LeviathanSkillTree.Node n : LeviathanSkillTree.NODES) drawNode(g, n, ox + n.treeX(), oy + n.treeY(), mx, my, time, p);
        g.disableScissor();
        g.centeredText(font, Component.translatable("screen.wotn.mastery.tree_help"), v.centerX(), v.top + 5, 0xFF58727A);
    }

    private void drawConnection(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, boolean parent, boolean unlocked) {
        int color = unlocked ? 0xE072D5EE : parent ? 0x9A5A8994 : 0x60304850;
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) { float p = steps == 0 ? 0 : i / (float) steps;
            int x = Mth.floor(Mth.lerp(p, x0, x1)), y = Mth.floor(Mth.lerp(p, y0, y1)); g.fill(x, y, x + 2, y + 2, color); }
    }

    private void drawNode(GuiGraphicsExtractor g, LeviathanSkillTree.Node n, int x, int y, int mx, int my, float time, WeaponSkillProgress p) {
        boolean unlocked = p.isUnlocked(n.id()), available = n.canUnlock(p), selected = selectedNode == n.id(), hovered = nodeAt(mx, my, x, y);
        int r = n.type() == LeviathanSkillTree.Type.ACTIVE ? NODE_RADIUS + 3 : NODE_RADIUS;
        int fill = unlocked ? 0xD01C4653 : available ? 0xB0263335 : 0xC0131A1D;
        int border = unlocked ? 0xFF72D5EE : available ? 0xFFD9A85F : n.implemented() ? 0xFF40545A : 0xFF29363A;
        diamond(g, x, y, r, fill); diamondOutline(g, x, y, r, border);
        if (selected || hovered) diamondOutline(g, x, y, r + 5, selected ? 0xB0DDFBFF : 0x7072D5EE);
        diamondOutline(g, x, y, 5, border);
        if (n.cost() > 0 && !unlocked) g.centeredText(font, Component.literal(Integer.toString(n.cost())), x, y + r + 3, available ? 0xFFD9A85F : 0xFF66767A);
    }

    private void drawLoadout(GuiGraphicsExtractor g, Layout l, int mx, int my, float time) {
        ForgeWeaponSnapshot w = selected(); int cx = (l.railRight + l.right()) / 2, cy = l.y + 120;
        diamond(g, cx, cy, 31, 0x35265B68);
        submitWeaponPreview(g, w.stack(), cx - 40, cy - 38, cx + 40, cy + 38, 35, -15, time * .72F, -25);
        drawLoadoutSlot(g, 1, w.primarySkill(), cx - 84, cy, mx, my);
        drawLoadoutSlot(g, 2, w.secondarySkill(), cx + 84, cy, mx, my);
        g.centeredText(font, Component.translatable("screen.wotn.mastery.secondary_slot"), cx, cy + 42, 0xFFD9A85F);
        int y = cy + 75;
        drawRune(g, LeviathanSkillSelection.THROW, cx - 72, y, mx, my);
        drawRune(g, LeviathanSkillSelection.IMBUE, cx, y, mx, my);
        drawRune(g, LeviathanSkillSelection.ICE_SPIKES, cx + 72, y, mx, my);
    }

    private void drawLoadoutSlot(GuiGraphicsExtractor g, int slot, int skill, int x, int y, int mx, int my) {
        int border = selectedLoadoutSlot == slot ? 0xFFD9A85F : nodeAt(mx, my, x, y) ? 0xFFDDFBFF : 0xFF72D5EE;
        diamond(g, x, y, 21, 0xC0193942); diamondOutline(g, x, y, 21, border); diamondOutline(g, x, y, 7, border);
        g.centeredText(font, Component.translatable("screen.wotn.mastery.slot", slot, LeviathanSkillSelection.keyMessage(slot)), x, y - 34, border);
        Component name = skill == 0 ? Component.translatable("screen.wotn.mastery.empty_slot") : nodeName(LeviathanSkillTree.byId(nodeIdForSkill(skill)));
        g.centeredText(font, name, x, y + 29, skill == 0 ? 0xFF60747A : 0xFFDDFBFF);
    }

    private void drawRune(GuiGraphicsExtractor g, int skill, int x, int y, int mx, int my) {
        int node = nodeIdForSkill(skill); boolean unlocked = progress().isUnlocked(node);
        int current = selectedLoadoutSlot == 1 ? selected().primarySkill() : selected().secondarySkill();
        int border = !unlocked ? 0xFF40545A : current == skill ? 0xFFD9A85F : 0xFF72D5EE;
        diamond(g, x, y, 18, unlocked ? 0xC01A3C46 : 0xC012181B); diamondOutline(g, x, y, 18, border); diamondOutline(g, x, y, 7, border);
        g.centeredText(font, nodeName(LeviathanSkillTree.byId(node)), x, y + 26, unlocked ? 0xFFB9DCE3 : 0xFF53666B);
    }

    private void drawDetails(GuiGraphicsExtractor g, Layout l) {
        LeviathanSkillTree.Node n = LeviathanSkillTree.byId(selectedNode); WeaponSkillProgress p = progress();
        int x = l.x + 14, y = l.contentBottom + 9, right = l.right() - 14;
        g.text(font, nodeName(n), x, y, 0xFFDDFBFF, true);
        Component type = Component.translatable("screen.wotn.node.type." + n.type().name().toLowerCase());
        g.text(font, type, right - font.width(type), y, 0xFF72D5EE, false);
        g.textWithWordWrap(font, nodeDescription(n), x, y + 14, Math.max(80, l.width - 185), 0xFFA9C1C8, false);
        Component action = unlockAction(n, p); int color = feedbackTicks > 0 ? 0xFFDDFBFF : n.canUnlock(p) ? 0xFFD9A85F : 0xFF6B8187;
        g.text(font, action, right - font.width(action), y + 28, color, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (e.button() != 0) return super.mouseClicked(e, dbl);
        Layout l = layout(); int y = l.y + 61;
        for (ForgeWeaponSnapshot w : weapons) {
            if (e.x() >= l.x + 9 && e.x() <= l.railRight - 8 && e.y() >= y && e.y() <= y + 70) {
                ClientPacketDistributor.sendToServer(new SelectForgeWeaponPayload(sessionId, w.weaponId())); return true;
            }
            y += 78;
        }
        if (selected() == null) return true;
        int center = (l.railRight + l.right()) / 2, tabY = l.y + 46, gap = Math.min(92, (l.right() - l.railRight) / 4);
        if (Math.abs(e.y() - tabY) <= 15) {
            if (Math.abs(e.x() - (center - gap)) < 55) { tab = Tab.PROGRESSION; return true; }
            if (Math.abs(e.x() - (center + gap)) < 55) { tab = Tab.LOADOUT; return true; }
        }
        if (tab == Tab.PROGRESSION) return clickTree(e, l, dbl);
        return clickLoadout(e, l);
    }

    private boolean clickTree(MouseButtonEvent e, Layout l, boolean dbl) {
        TreeViewport v = treeViewport(l); if (!v.contains(e.x(), e.y())) return false;
        int ox = v.centerX() + Mth.floor(treePanX), oy = v.centerY() - 16 + Mth.floor(treePanY);
        for (LeviathanSkillTree.Node n : LeviathanSkillTree.NODES) if (nodeAt(e.x(), e.y(), ox + n.treeX(), oy + n.treeY())) {
            selectedNode = n.id(); if (dbl) unlockSelected(); else playSelect(); return true;
        }
        return false;
    }

    private boolean clickLoadout(MouseButtonEvent e, Layout l) {
        int cx = (l.railRight + l.right()) / 2, cy = l.y + 120;
        if (nodeAt(e.x(), e.y(), cx - 84, cy)) { selectedLoadoutSlot = 1; return true; }
        if (nodeAt(e.x(), e.y(), cx + 84, cy)) { selectedLoadoutSlot = 2; return true; }
        int y = cy + 75;
        if (nodeAt(e.x(), e.y(), cx - 72, y)) return equip(3);
        if (nodeAt(e.x(), e.y(), cx, y)) return equip(1);
        if (nodeAt(e.x(), e.y(), cx + 72, y)) return equip(2);
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (tab == Tab.PROGRESSION && e.button() == 0 && selected() != null) { treePanX = Mth.clamp(treePanX + dx, -135, 135); treePanY = Mth.clamp(treePanY + dy, -100, 100); return true; }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (tab == Tab.PROGRESSION && selected() != null) { treePanX = Mth.clamp(treePanX + sx * 18, -135, 135); treePanY = Mth.clamp(treePanY + sy * 18, -100, 100); return true; }
        return super.mouseScrolled(x, y, sx, sy);
    }
    @Override public boolean keyPressed(KeyEvent e) { if (e.isLeft() || e.isRight()) { tab = tab == Tab.PROGRESSION ? Tab.LOADOUT : Tab.PROGRESSION; return true; } if (e.isConfirmation() && tab == Tab.PROGRESSION) { unlockSelected(); return true; } return super.keyPressed(e); }

    private void unlockSelected() { LeviathanSkillTree.Node n = LeviathanSkillTree.byId(selectedNode); if (selected() != null && n.canUnlock(progress())) { ClientPacketDistributor.sendToServer(new UnlockSkillNodePayload(sessionId, n.id())); playConfirm(); } }
    private boolean equip(int skill) { int node = nodeIdForSkill(skill); selectedNode = node; if (!progress().isUnlocked(node)) return true; ClientPacketDistributor.sendToServer(new SelectLeviathanSkillPayload(sessionId, selectedLoadoutSlot, skill)); playConfirm(); return true; }

    private ForgeWeaponSnapshot selected() { return selectedWeaponId == null ? null : weapons.stream().filter(w -> w.weaponId().equals(selectedWeaponId)).findFirst().orElse(null); }
    private WeaponSkillProgress progress() { return selected() == null ? WeaponSkillProgress.DEFAULT : selected().progress(); }
    private int nodeIdForSkill(int skill) { return switch (skill) { case 1 -> LeviathanSkillTree.IMBUE; case 2 -> LeviathanSkillTree.ICE_SPIKES; default -> LeviathanSkillTree.THROW; }; }

    private void submitWeaponPreview(GuiGraphicsExtractor g, ItemStack stack, int x0, int y0, int x1, int y1, float scale, float rx, float ry, float rz) {
        ItemStackRenderState state = new ItemStackRenderState(); minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.FIXED, minecraft.level, minecraft.player, 0);
        g.submitPictureInPictureRenderState(new ItemPreviewRenderState(state, rx, ry, rz, x0, y0, x1, y1, scale, new Matrix3x2f(g.pose()), g.peekScissorStack()));
    }

    private Component unlockAction(LeviathanSkillTree.Node n, WeaponSkillProgress p) {
        if (!n.implemented()) return Component.translatable("screen.wotn.node.coming_soon"); if (p.isUnlocked(n.id())) return Component.translatable("screen.wotn.node.unlocked");
        if (!p.isUnlocked(n.parentId())) return Component.translatable("screen.wotn.node.requires", nodeName(LeviathanSkillTree.byId(n.parentId())));
        if (p.masteryLevel() < n.requiredMastery()) return Component.translatable("screen.wotn.node.requires_mastery", n.requiredMastery());
        if (p.points() < n.cost()) return Component.translatable("screen.wotn.node.insufficient"); return Component.translatable("screen.wotn.node.unlock", n.cost());
    }
    private Component nodeName(LeviathanSkillTree.Node n) { return Component.translatable("screen.wotn.node." + n.key()); }
    private Component nodeDescription(LeviathanSkillTree.Node n) { return Component.translatable("screen.wotn.node." + n.key() + ".description"); }
    private void playSelect() { if (minecraft.player != null) minecraft.player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, .18F, 1.7F); }
    private void playConfirm() { if (minecraft.player != null) minecraft.player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, .6F, .85F); }
    private static boolean nodeAt(double mx, double my, int x, int y) { return Math.abs(mx - x) + Math.abs(my - y) <= 21; }

    private Layout layout() { int w = Math.min(720, Math.max(260, width - 20)), h = Math.min(400, Math.max(190, height - 12)); w = Math.min(w, width - 8); h = Math.min(h, height - 8); int x = (width - w) / 2, y = (height - h) / 2; int rail = Math.min(132, Math.max(100, w / 5)); int detail = Math.min(76, Math.max(62, h / 4)); return new Layout(x, y, w, h, x + rail, y + h - detail); }
    private TreeViewport treeViewport(Layout l) { return new TreeViewport(l.railRight + 5, l.y + 66, l.right() - 5, l.contentBottom - 5); }
    private static void diamond(GuiGraphicsExtractor g, int x, int y, int r, int c) { for (int o = -r; o <= r; o++) { int hw = r - Math.abs(o); g.horizontalLine(x - hw, x + hw, y + o, c); } }
    private static void diamondOutline(GuiGraphicsExtractor g, int x, int y, int r, int c) { for (int i = 0; i < r; i++) { g.fill(x-i-1,y-r+i,x-i+1,y-r+i+1,c); g.fill(x+i,y-r+i,x+i+2,y-r+i+1,c); g.fill(x-i-1,y+r-i,x-i+1,y+r-i+1,c); g.fill(x+i,y+r-i,x+i+2,y+r-i+1,c); } }

    private void closeSession() { if (!serverClosing && !closeSent) { closeSent = true; ClientPacketDistributor.sendToServer(new CloseForgeSessionPayload(sessionId)); } }
    @Override public void onClose() { closeSession(); super.onClose(); }
    @Override public void removed() { closeSession(); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    private enum Tab { PROGRESSION("screen.wotn.mastery.tab.progression"), LOADOUT("screen.wotn.mastery.tab.loadout"); private final String key; Tab(String key) { this.key = key; } }
    private record Layout(int x, int y, int width, int height, int railRight, int contentBottom) { int right(){return x+width;} int bottom(){return y+height;} }
    private record TreeViewport(int left,int top,int right,int bottom){int centerX(){return(left+right)/2;}int centerY(){return(top+bottom)/2;}boolean contains(double x,double y){return x>=left&&x<=right&&y>=top&&y<=bottom;}}
}
