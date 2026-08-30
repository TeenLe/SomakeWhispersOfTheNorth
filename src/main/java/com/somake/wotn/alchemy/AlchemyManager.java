package com.somake.wotn.alchemy;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.network.AlchemyActionPayload;
import com.somake.wotn.network.AlchemyFormulaSnapshot;
import com.somake.wotn.network.AlchemyFormulaSnapshot.IngredientSnapshot;
import com.somake.wotn.network.AlchemyRuneSnapshot;
import com.somake.wotn.network.OpenAlchemyPayload;
import com.somake.wotn.network.UpdateAlchemyPayload;
import com.somake.wotn.registry.ModAttachments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AlchemyManager extends SimpleJsonResourceReloadListener<AlchemyFormula> {
    public static final AlchemyManager INSTANCE = new AlchemyManager();
    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            WhispersOfTheNorth.MODID, "alchemy_formulas");
    private static final String NONE = "";

    private volatile Map<Identifier, AlchemyFormula> formulas = Map.of();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> completionNotices = new HashMap<>();

    private AlchemyManager() {
        super(AlchemyFormula.CODEC, FileToIdConverter.json("alchemy"));
    }

    public static void registerReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, INSTANCE);
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INSTANCE.sessions.remove(event.getEntity().getUUID());
        INSTANCE.completionNotices.remove(event.getEntity().getUUID());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        if (now % 20L != 0L) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            AlchemyProgress progress = INSTANCE.progress(player);
            if (progress.activeAnalysis().isEmpty()) {
                INSTANCE.completionNotices.remove(player.getUUID());
                continue;
            }
            AlchemyProgress.Analysis analysis = progress.activeAnalysis().get();
            if (analysis.completesAt() > now) continue;
            INSTANCE.finishAnalysis(player, progress, analysis);
            if (INSTANCE.completionNotices.getOrDefault(player.getUUID(), Long.MIN_VALUE) != analysis.completesAt()) {
                INSTANCE.completionNotices.put(player.getUUID(), analysis.completesAt());
                player.sendSystemMessage(Component.translatable("message.wotn.alchemy.analysis_complete"));
            }
            Session session = INSTANCE.sessions.get(player.getUUID());
            if (session != null) INSTANCE.sync(player, session, "screen.wotn.alchemy.message.analysis_complete");
        }
    }

    @Override
    protected void apply(Map<Identifier, AlchemyFormula> preparations,
            ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, AlchemyFormula> valid = new HashMap<>();
        preparations.forEach((id, formula) -> {
            if (!BuiltInRegistries.ITEM.containsKey(formula.result()) || formula.totalStudyUnits() < 1) {
                WhispersOfTheNorth.LOGGER.warn("Ignoring invalid alchemy formula {}", id);
                return;
            }
            boolean missingCost = java.util.stream.Stream.concat(
                    formula.studyIngredients().stream(), formula.brewingIngredients().stream())
                    .anyMatch(cost -> !BuiltInRegistries.ITEM.containsKey(cost.item()));
            if (missingCost) {
                WhispersOfTheNorth.LOGGER.warn("Ignoring alchemy formula {} with unknown ingredient", id);
                return;
            }
            valid.put(id, formula);
        });
        this.formulas = Map.copyOf(valid);
        WhispersOfTheNorth.LOGGER.info("Loaded {} alchemy formulas", this.formulas.size());
    }

    public void open(ServerPlayer player) {
        AlchemyProgress progress = normalizeProgress(player, progress(player));
        String selected = progress.activeAnalysis().map(AlchemyProgress.Analysis::formulaId)
                .orElseGet(() -> progress.studies().stream().findFirst().map(AlchemyProgress.Study::formulaId)
                        .orElseGet(() -> progress.learnedFormulas().stream().findFirst().orElse(NONE)));
        Session session = new Session(UUID.randomUUID(), selected);
        sessions.put(player.getUUID(), session);
        PacketDistributor.sendToPlayer(player, snapshot(player, session, NONE));
    }

    public void select(ServerPlayer player, UUID sessionId, String formulaId) {
        Session session = validSession(player, sessionId);
        AlchemyProgress progress = progress(player);
        if (session == null || formula(formulaId).isEmpty() || !isVisible(progress, formulaId)) return;
        session.selectedFormulaId = formulaId;
        sync(player, session, NONE);
    }

    public void act(ServerPlayer player, UUID sessionId, String formulaId, String itemId,
            AlchemyActionPayload.Action action) {
        Session session = validSession(player, sessionId);
        Optional<Map.Entry<Identifier, AlchemyFormula>> entry = formula(formulaId);
        if (session == null || entry.isEmpty()) return;
        session.selectedFormulaId = formulaId;
        switch (action) {
            case SUBMIT_INGREDIENT -> submitIngredient(player, session, entry.get(), itemId);
            case EQUIP_RUNE -> configureRune(player, session, entry.get(), itemId, true);
            case UNEQUIP_RUNE -> configureRune(player, session, entry.get(), itemId, false);
            case BREW_ONE -> brew(player, session, entry.get(), 1, session.configurationFor(entry.get()));
            case BREW_THREE -> brew(player, session, entry.get(), 3, session.configurationFor(entry.get()));
        }
    }

    public void close(ServerPlayer player, UUID sessionId) {
        Session session = validSession(player, sessionId);
        if (session != null) sessions.remove(player.getUUID());
    }

    public void resetProgress(ServerPlayer player) {
        setProgress(player, AlchemyProgress.DEFAULT);
        completionNotices.remove(player.getUUID());
        Session session = sessions.get(player.getUUID());
        if (session != null) {
            session.selectedFormulaId = orderedFormulaIds().stream()
                    .filter(id -> formulas.get(id).prerequisite().isEmpty())
                    .findFirst().map(Identifier::toString).orElse(NONE);
            sync(player, session, "screen.wotn.alchemy.message.progress_reset");
        }
        player.sendSystemMessage(Component.translatable("message.wotn.alchemy.progress_reset"));
    }

    public void unlockAll(ServerPlayer player) {
        List<String> formulaIds = orderedFormulaIds().stream().map(Identifier::toString).toList();
        setProgress(player, progress(player).unlockAll(formulaIds));
        completionNotices.remove(player.getUUID());
        Session session = sessions.get(player.getUUID());
        if (session != null) {
            session.selectedFormulaId = formulaIds.stream().findFirst().orElse(NONE);
            sync(player, session, "screen.wotn.alchemy.message.all_unlocked");
        }
        player.sendSystemMessage(Component.translatable("message.wotn.alchemy.all_unlocked"));
    }

    private void submitIngredient(ServerPlayer player, Session session,
            Map.Entry<Identifier, AlchemyFormula> entry, String itemId) {
        AlchemyProgress progress = progress(player);
        if (progress.activeAnalysis().isPresent()) {
            sync(player, session, "screen.wotn.alchemy.message.analysis_busy");
            return;
        }
        String formulaId = entry.getKey().toString();
        if (progress.isLearned(formulaId) || !prerequisiteMet(progress, entry.getValue())) {
            sync(player, session, "screen.wotn.alchemy.message.prerequisite");
            return;
        }
        if (progress.study(formulaId).isEmpty()) {
            progress = progress.openStudy(formulaId);
        }
        AlchemyProgress.Study study = progress.study(formulaId).orElse(null);
        Optional<AlchemyFormula.Cost> cost = entry.getValue().studyCost(itemId);
        if (study == null || cost.isEmpty()) return;
        int remaining = cost.get().count() - study.studied(itemId);
        if (remaining <= 0) {
            sync(player, session, "screen.wotn.alchemy.message.ingredient_complete");
            return;
        }
        int accepted = Math.min(remaining, count(player, cost.get()));
        if (accepted <= 0 || !consumeItem(player, cost.get(), accepted)) {
            sync(player, session, "screen.wotn.alchemy.message.missing_ingredient");
            return;
        }
        long completesAt = now(player) + entry.getValue().analysisTicks();
        setProgress(player, progress.startAnalysis(formulaId, itemId, accepted, completesAt));
        sync(player, session, "screen.wotn.alchemy.message.analysis_started");
    }

    private void finishAnalysis(ServerPlayer player, AlchemyProgress progress,
            AlchemyProgress.Analysis analysis) {
        Optional<Map.Entry<Identifier, AlchemyFormula>> entry = formula(analysis.formulaId());
        if (entry.isEmpty()) {
            setProgress(player, new AlchemyProgress(progress.learnedFormulas(), progress.studies(), Optional.empty()));
            return;
        }
        AlchemyFormula formula = entry.get().getValue();
        AlchemyProgress.Study current = progress.study(analysis.formulaId()).orElse(null);
        Optional<AlchemyFormula.Cost> cost = formula.studyCost(analysis.itemId());
        if (current == null || cost.isEmpty()) {
            setProgress(player, new AlchemyProgress(progress.learnedFormulas(), progress.studies(), Optional.empty()));
            return;
        }
        AlchemyProgress.Study updated = current.add(analysis.itemId(), analysis.count(), cost.get().count());
        boolean complete = formula.studyIngredients().stream()
                .allMatch(required -> updated.studied(required.item().toString()) >= required.count());
        setProgress(player, progress.finishAnalysis(analysis.formulaId(), analysis.itemId(),
                analysis.count(), cost.get().count(), complete));
    }

    private void brew(ServerPlayer player, Session session,
            Map.Entry<Identifier, AlchemyFormula> entry, int batches,
            AlchemyPotionConfiguration configuration) {
        AlchemyProgress progress = progress(player);
        String id = entry.getKey().toString();
        if (!progress.isLearned(id)) return;
        Item resultItem = BuiltInRegistries.ITEM.getValue(entry.getValue().result());
        if (resultItem == null) {
            sync(player, session, "screen.wotn.alchemy.message.cannot_brew");
            return;
        }
        int count = entry.getValue().resultCount() * batches;
        ItemStack output = new ItemStack(resultItem, count);
        List<AlchemyFormula.Cost> costs = new ArrayList<>(entry.getValue().brewingIngredients());
        if (!configuration.isValidFor(entry.getValue().family(), entry.getValue().tier())) return;
        if (!ownsRunes(player, configuration)) {
            sync(player, session, "screen.wotn.alchemy.message.rune_missing");
            return;
        }
        output.set(com.somake.wotn.registry.ModDataComponents.ALCHEMY_CONFIGURATION.get(), configuration);
        if (!consumeAndGive(player, costs, batches, output)) {
            sync(player, session, "screen.wotn.alchemy.message.cannot_brew");
            return;
        }
        sync(player, session, "screen.wotn.alchemy.message.brewed");
    }

    private void configureRune(ServerPlayer player, Session session,
            Map.Entry<Identifier, AlchemyFormula> entry, String runeId, boolean equip) {
        AlchemyFormula formula = entry.getValue();
        if (!progress(player).isLearned(entry.getKey().toString())) return;
        AlchemyRune rune = AlchemyRune.fromId(runeId);
        if (rune == null || !rune.family().equals(formula.family())) return;
        AlchemyPotionConfiguration current = session.configurationFor(entry);
        AlchemyPotionConfiguration updated = equip
                ? current.equip(rune, formula.family(), formula.tier())
                : current.unequip(rune);
        if (updated == current || !updated.isValidFor(formula.family(), formula.tier())) return;
        if (equip && !ownsRunes(player, updated)) {
            sync(player, session, "screen.wotn.alchemy.message.rune_missing");
            return;
        }
        session.configurations.put(entry.getKey().toString(), updated);
        sync(player, session, NONE);
    }

    private OpenAlchemyPayload snapshot(ServerPlayer player, Session session, String messageKey) {
        AlchemyProgress progress = normalizeProgress(player, progress(player));
        List<AlchemyFormulaSnapshot> result = new ArrayList<>();
        for (Identifier id : orderedFormulaIds()) {
            String formulaId = id.toString();
            if (!isVisible(progress, formulaId)) continue;
            AlchemyFormula formula = formulas.get(id);
            Item resultItem = BuiltInRegistries.ITEM.getValue(formula.result());
            if (resultItem == null) continue;
            AlchemyProgress.Study study = progress.study(formulaId).orElse(null);
            boolean active = progress.activeAnalysis().map(analysis -> analysis.formulaId().equals(formulaId)).orElse(false);
            int totalStudied = study == null ? 0 : formula.studyIngredients().stream()
                    .mapToInt(cost -> Math.min(cost.count(), study.studied(cost.item().toString()))).sum();
            int highestLearnedTier = highestLearnedTier(progress, formula.family());
            AlchemyFormulaSnapshot.State state = progress.isLearned(formulaId)
                    ? AlchemyFormulaSnapshot.State.LEARNED
                    : active ? AlchemyFormulaSnapshot.State.ANALYZING
                    : AlchemyFormulaSnapshot.State.STUDYING;
            AlchemyPotionConfiguration configuration = session.configurationFor(Map.entry(id, formula));
            List<IngredientSnapshot> brewing = new ArrayList<>(brewingIngredients(player, formula));
            result.add(new AlchemyFormulaSnapshot(formulaId, formula.family(), formula.tier(),
                    new ItemStack(resultItem, formula.resultCount()), formula.hiddenTitle(), formula.hint(),
                    formula.description(), formula.role(), formula.beneficial(), formula.analysisTicks(), state,
                    totalStudied, formula.totalStudyUnits(), studyIngredients(player, progress, formulaId, formula),
                    brewing,
                    AlchemyModifierCatalog.forFamily(formula.family()).stream()
                            .map(modifier -> new AlchemyFormulaSnapshot.ModifierSnapshot(modifier.requiredTier(),
                                    modifier.nameKey(), modifier.descriptionKey(), highestLearnedTier >= modifier.requiredTier()))
                            .toList(), configuration));
        }
        String activeId = progress.activeAnalysis().map(AlchemyProgress.Analysis::formulaId).orElse(NONE);
        return new OpenAlchemyPayload(session.id, result, runeSnapshots(player), session.selectedFormulaId,
                activeId, remainingTicks(player, progress), messageKey);
    }

    private List<AlchemyRuneSnapshot> runeSnapshots(ServerPlayer player) {
        return AlchemyRune.ordered().stream().map(rune -> {
            Item item = BuiltInRegistries.ITEM.getValue(rune.itemId());
            return new AlchemyRuneSnapshot(rune.id(), new ItemStack(item), rune.slots(), count(player, item));
        }).toList();
    }

    private List<IngredientSnapshot> studyIngredients(ServerPlayer player, AlchemyProgress progress,
            String formulaId, AlchemyFormula formula) {
        AlchemyProgress.Study study = progress.study(formulaId).orElse(null);
        AlchemyProgress.Analysis analysis = progress.activeAnalysis().orElse(null);
        List<IngredientSnapshot> snapshots = new ArrayList<>();
        boolean previousIngredientsComplete = true;
        for (int i = 0; i < formula.studyIngredients().size(); i++) {
            AlchemyFormula.Cost cost = formula.studyIngredients().get(i);
            int contributed = study == null ? 0 : Math.min(cost.count(), study.studied(cost.item().toString()));
            boolean analyzing = analysis != null && analysis.formulaId().equals(formulaId)
                    && analysis.itemId().equals(cost.item().toString());
            boolean revealed = contributed > 0 || analyzing || previousIngredientsComplete;
            snapshots.add(new IngredientSnapshot(cost.displayStack(), cost.count(), contributed,
                    count(player, cost), revealed, analyzing));
            previousIngredientsComplete &= contributed >= cost.count();
        }
        return List.copyOf(snapshots);
    }

    private List<IngredientSnapshot> brewingIngredients(ServerPlayer player, AlchemyFormula formula) {
        return formula.brewingIngredients().stream().map(cost -> {
            return new IngredientSnapshot(cost.displayStack(), cost.count(), 0, count(player, cost), true, false);
        }).toList();
    }

    private AlchemyProgress normalizeProgress(ServerPlayer player, AlchemyProgress progress) {
        AlchemyProgress normalized = progress;
        for (String learned : progress.learnedFormulas()) {
            if (normalized.study(learned).isPresent()) {
                List<AlchemyProgress.Study> studies = normalized.studies().stream()
                        .filter(study -> !study.formulaId().equals(learned)).toList();
                normalized = new AlchemyProgress(normalized.learnedFormulas(), studies, normalized.activeAnalysis());
            }
        }
        if (normalized != progress) setProgress(player, normalized);
        return normalized;
    }

    private boolean prerequisiteMet(AlchemyProgress progress, AlchemyFormula formula) {
        return formula.prerequisite().isEmpty() || progress.isLearned(formula.prerequisite().get());
    }

    private boolean isVisible(AlchemyProgress progress, String formulaId) {
        if (progress.isLearned(formulaId) || progress.study(formulaId).isPresent()) return true;
        return formula(formulaId).map(entry -> prerequisiteMet(progress, entry.getValue())).orElse(false);
    }

    private int count(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item.builtInRegistryHolder())) count += stack.getCount();
        }
        return count;
    }

    private int count(ServerPlayer player, AlchemyFormula.Cost cost) {
        int count = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (cost.matches(stack)) count += stack.getCount();
        }
        return count;
    }

    private boolean ownsRunes(ServerPlayer player, AlchemyPotionConfiguration configuration) {
        for (AlchemyRune rune : AlchemyRune.ordered()) {
            if (!configuration.has(rune)) continue;
            Item item = BuiltInRegistries.ITEM.getValue(rune.itemId());
            if (item == null || count(player, item) < configuration.count(rune)) return false;
        }
        return true;
    }

    private boolean consumeItem(ServerPlayer player, AlchemyFormula.Cost cost, int amount) {
        SimpleContainer staged = copyInventory(player);
        if (!remove(staged, cost, amount)) return false;
        commitInventory(player, staged);
        return true;
    }

    private boolean consumeAndGive(ServerPlayer player, List<AlchemyFormula.Cost> costs,
            int multiplier, ItemStack output) {
        SimpleContainer staged = copyInventory(player);
        for (AlchemyFormula.Cost cost : costs) {
            if (!remove(staged, cost, cost.count() * multiplier)) return false;
        }
        ItemStack remainder = insert(staged, output.copy());
        if (!remainder.isEmpty()) return false;
        commitInventory(player, staged);
        return true;
    }

    private ItemStack insert(SimpleContainer container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) continue;
            int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            stack.shrink(moved);
        }
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            if (!container.getItem(slot).isEmpty()) continue;
            int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            container.setItem(slot, stack.copyWithCount(moved));
            stack.shrink(moved);
        }
        return stack;
    }

    private SimpleContainer copyInventory(ServerPlayer player) {
        SimpleContainer staged = new SimpleContainer(Inventory.INVENTORY_SIZE);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            staged.setItem(slot, player.getInventory().getItem(slot).copy());
        }
        return staged;
    }

    private boolean remove(SimpleContainer container, Item item, int amount) {
        int available = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item.builtInRegistryHolder())) available += stack.getCount();
        }
        if (available < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(item.builtInRegistryHolder())) continue;
            int removed = Math.min(stack.getCount(), remaining);
            stack.shrink(removed);
            remaining -= removed;
        }
        return remaining == 0;
    }

    private boolean remove(SimpleContainer container, AlchemyFormula.Cost cost, int amount) {
        int available = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (cost.matches(stack)) available += stack.getCount();
        }
        if (available < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!cost.matches(stack)) continue;
            int removed = Math.min(stack.getCount(), remaining);
            stack.shrink(removed);
            remaining -= removed;
        }
        return remaining == 0;
    }

    private void commitInventory(ServerPlayer player, SimpleContainer staged) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            player.getInventory().setItem(slot, staged.getItem(slot).copy());
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private int remainingTicks(ServerPlayer player, AlchemyProgress progress) {
        if (progress.activeAnalysis().isEmpty()) return 0;
        return (int)Math.min(Integer.MAX_VALUE,
                Math.max(0L, progress.activeAnalysis().get().completesAt() - now(player)));
    }

    private long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    private AlchemyProgress progress(ServerPlayer player) {
        return player.getData(ModAttachments.ALCHEMY_PROGRESS);
    }

    private void setProgress(ServerPlayer player, AlchemyProgress progress) {
        player.setData(ModAttachments.ALCHEMY_PROGRESS, progress);
    }

    private void sync(ServerPlayer player, Session session, String messageKey) {
        PacketDistributor.sendToPlayer(player, new UpdateAlchemyPayload(snapshot(player, session, messageKey)));
    }

    private Session validSession(ServerPlayer player, UUID sessionId) {
        Session session = sessions.get(player.getUUID());
        return session != null && session.id.equals(sessionId) ? session : null;
    }

    private Optional<Map.Entry<Identifier, AlchemyFormula>> formula(String formulaId) {
        Identifier id = Identifier.tryParse(formulaId);
        if (id == null) return Optional.empty();
        AlchemyFormula formula = formulas.get(id);
        return formula == null ? Optional.empty() : Optional.of(Map.entry(id, formula));
    }

    private List<Identifier> orderedFormulaIds() {
        return formulas.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Identifier, AlchemyFormula> entry) -> entry.getValue().tier())
                        .thenComparingInt(entry -> familyOrder(entry.getValue().family())))
                .map(Map.Entry::getKey).toList();
    }

    private int highestLearnedTier(AlchemyProgress progress, String family) {
        return formulas.entrySet().stream().filter(entry -> entry.getValue().family().equals(family))
                .filter(entry -> progress.isLearned(entry.getKey().toString()))
                .mapToInt(entry -> entry.getValue().tier()).max().orElse(0);
    }

    private int familyOrder(String family) {
        return switch (family) {
            case "jormungandr" -> 0;
            case "fenrir" -> 1;
            case "niflheim" -> 2;
            case "idunn" -> 3;
            default -> 4;
        };
    }

    private static final class Session {
        private final UUID id;
        private String selectedFormulaId;
        private final Map<String, AlchemyPotionConfiguration> configurations = new HashMap<>();
        private Session(UUID id, String selectedFormulaId) {
            this.id = id;
            this.selectedFormulaId = selectedFormulaId;
        }

        private AlchemyPotionConfiguration configurationFor(Map.Entry<Identifier, AlchemyFormula> entry) {
            return configurations.computeIfAbsent(entry.getKey().toString(),
                    ignored -> AlchemyPotionConfiguration.DEFAULT);
        }
    }
}
