package dev.myriad.boze;

import dev.boze.api.BozeInstance;
import dev.boze.api.addon.Addon;
import dev.boze.api.client.ModuleManager;
import dev.boze.api.client.module.BaseModule;
import dev.boze.api.internal.Instances;
import dev.myriad.api.Myriad;
import dev.myriad.api.addon.AddonContext;
import dev.myriad.api.module.Category;
import dev.myriad.api.setting.SettingColor;
import dev.myriad.api.util.MyriadId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds Boze's modules and keeps their Myriad mirrors in step. Runs once per tick from {@link BozeAddon}:
 * <ul>
 *   <li>every tick, each mirror follows its Boze module's on/off state (a toggle from Boze's GUI or keybind shows in
 *       Myriad the same tick);</li>
 *   <li>every half second, settings and binds are read back from Boze, so edits made in Boze's own GUI or a profile it
 *       loaded appear in Myriad;</li>
 *   <li>every second, modules Boze gained since (its addons load after it) are bridged too.</li>
 * </ul>
 * Reads are plain field getters, a few thousand per second at most, so this costs nothing measurable; Boze has no
 * change events for settings, so polling is the only way to notice its side.
 */
final class BozeBridge {
	private final AddonContext ctx;
	/** Boze module to its mirror; null for one that couldn't be bridged, so it isn't retried every second. */
	private final Map<BaseModule, BozeModule> bridged = new IdentityHashMap<>();
	private final List<BozeModule> mirrors = new ArrayList<>();
	/** Categories registered for Boze addons' own category names. */
	private final Map<String, Category> addonCategories = new HashMap<>();
	private int tick;
	private boolean announced;

	BozeBridge(AddonContext ctx) {
		this.ctx = ctx;
		discover();
	}

	/** Whether Boze has started and filled its module registry. */
	static boolean ready() {
		try {
			return Instances.getModules() != null && !ModuleManager.getClientModules().isEmpty();
		} catch (RuntimeException | LinkageError e) {
			return false;
		}
	}

	void tick() {
		tick++;
		for (BozeModule m : mirrors) m.pullState();
		if (tick % 10 == 0) for (BozeModule m : mirrors) m.pullSettings();
		if (tick % 20 == 0) discover();
	}

	private void discover() {
		List<BaseModule> all = new ArrayList<>(ModuleManager.getClientModules());
		all.addAll(ModuleManager.getAddonModules());
		int added = 0;
		for (BaseModule boze : all) {
			if (bridged.containsKey(boze)) continue;
			BozeModule mirror = null;
			try {
				mirror = ctx.registerModule(new BozeModule(boze, categoryOf(boze)));
				added++;
			} catch (RuntimeException e) {
				ctx.logger().warn("Could not bridge Boze module {}: {}", boze.getName(), e.toString());
			}
			bridged.put(boze, mirror);
			if (mirror != null) mirrors.add(mirror);
		}
		if (added == 0) return;
		ctx.logger().info("Bridged {} Boze modules ({} in all)", added, mirrors.size());
		if (!announced) {
			announced = true;
			Myriad.notifications().success("Boze", mirrors.size() + " Boze modules are in the menu");
		}
	}

	/**
	 * A module of one of Boze's own addons goes in that addon's category if it declared one; anything else by the
	 * heuristics in {@link BozeCategories}.
	 */
	private Category categoryOf(BaseModule boze) {
		for (Addon addon : BozeInstance.INSTANCE.getAddons()) {
			if (!addon.modules.contains(boze)) continue;
			String name = addon.getCategory();
			if (name == null || name.isBlank()) break;
			Category shared = BozeCategories.byName(name);
			if (shared != null) return shared;
			return addonCategories.computeIfAbsent(name, n -> Myriad.categories().get(ctx.id(MyriadId.toPath(n)))
				.orElseGet(() -> ctx.registerCategory(n, "", SettingColor.role(SettingColor.Mode.CYAN))));
		}
		return BozeCategories.of(boze);
	}
}
