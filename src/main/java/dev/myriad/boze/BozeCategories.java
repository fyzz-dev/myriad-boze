package dev.myriad.boze;

import dev.boze.api.client.module.BaseModule;
import dev.myriad.api.module.Categories;
import dev.myriad.api.module.Category;

import java.util.Locale;
import java.util.Map;

/**
 * Which Myriad category a Boze module belongs in. Boze's API doesn't say which of its GUI categories a module is in,
 * so this guesses: first from the module class's package (a {@code ...combat...} package is a combat module), then
 * from a table of module names, and otherwise the Boze category, where everything unplaced is still easy to find.
 * A module in the wrong window is only a cosmetic miss; add it to {@link #NAMES} to fix it.
 */
final class BozeCategories {
	private BozeCategories() {
	}

	/** Package segments that name a category, normalised, to shared categories. */
	private static final Map<String, Category> PACKAGES = Map.ofEntries(
		Map.entry("combat", Categories.COMBAT),
		Map.entry("ghost", Categories.COMBAT),
		Map.entry("movement", Categories.MOVEMENT),
		Map.entry("travel", Categories.MOVEMENT),
		Map.entry("render", Categories.RENDER),
		Map.entry("visual", Categories.RENDER),
		Map.entry("visuals", Categories.RENDER),
		Map.entry("player", Categories.PLAYER),
		Map.entry("world", Categories.WORLD),
		Map.entry("misc", Categories.MISC),
		Map.entry("miscellaneous", Categories.MISC),
		Map.entry("chat", Categories.MISC)
	);

	/**
	 * Boze category names (an addon's {@code createCategory}) to shared categories. "Client" is one of Boze's GUI
	 * categories but also a package segment of everything Boze ({@code dev.boze.client...}), so it's only here.
	 */
	private static final Map<String, Category> CATEGORY_NAMES = Map.of("client", Categories.MISC);

	/** Module names (normalised: lower case, no spaces) to shared categories. Boze's naming follows the usual client vocabulary. */
	private static final Map<String, Category> NAMES = Map.ofEntries(
		// Combat
		entry("autocrystal", Categories.COMBAT), entry("autoanchor", Categories.COMBAT), entry("anchoraura", Categories.COMBAT),
		entry("aura", Categories.COMBAT), entry("killaura", Categories.COMBAT), entry("trigger", Categories.COMBAT),
		entry("triggerbot", Categories.COMBAT), entry("aimassist", Categories.COMBAT), entry("surround", Categories.COMBAT),
		entry("holefill", Categories.COMBAT), entry("selftrap", Categories.COMBAT), entry("autotrap", Categories.COMBAT),
		entry("burrow", Categories.COMBAT), entry("autoweb", Categories.COMBAT), entry("autototem", Categories.COMBAT),
		entry("offhand", Categories.COMBAT), entry("automine", Categories.COMBAT), entry("packetmine", Categories.COMBAT),
		entry("pearlphase", Categories.COMBAT), entry("autopearl", Categories.COMBAT), entry("autobed", Categories.COMBAT),
		entry("bedaura", Categories.COMBAT), entry("cevbreaker", Categories.COMBAT), entry("pistonaura", Categories.COMBAT),
		entry("pistoncrystal", Categories.COMBAT), entry("autoarmor", Categories.COMBAT), entry("antiregear", Categories.COMBAT),
		entry("quiver", Categories.COMBAT), entry("bowaimbot", Categories.COMBAT), entry("criticals", Categories.COMBAT),
		entry("autohitcrystal", Categories.COMBAT), entry("holesnap", Categories.COMBAT), entry("obsidian", Categories.COMBAT),
		entry("autocity", Categories.COMBAT), entry("cityesp", Categories.RENDER), entry("fastbow", Categories.COMBAT),
		entry("autoexp", Categories.COMBAT), entry("mace", Categories.COMBAT), entry("maceaura", Categories.COMBAT),
		entry("autoeat", Categories.PLAYER),
		// Movement
		entry("elytrafly", Categories.MOVEMENT), entry("elytraflight", Categories.MOVEMENT), entry("autopilot", Categories.MOVEMENT),
		entry("elytraautopilot", Categories.MOVEMENT), entry("speed", Categories.MOVEMENT), entry("sprint", Categories.MOVEMENT),
		entry("step", Categories.MOVEMENT), entry("nofall", Categories.MOVEMENT), entry("velocity", Categories.MOVEMENT),
		entry("noslow", Categories.MOVEMENT), entry("fly", Categories.MOVEMENT), entry("flight", Categories.MOVEMENT),
		entry("jesus", Categories.MOVEMENT), entry("strafe", Categories.MOVEMENT), entry("autowalk", Categories.MOVEMENT),
		entry("reversestep", Categories.MOVEMENT), entry("boatfly", Categories.MOVEMENT), entry("entitycontrol", Categories.MOVEMENT),
		entry("antihunger", Categories.MOVEMENT), entry("safewalk", Categories.MOVEMENT), entry("parkour", Categories.MOVEMENT),
		entry("spider", Categories.MOVEMENT), entry("longjump", Categories.MOVEMENT), entry("timer", Categories.MOVEMENT),
		entry("elytrarecast", Categories.MOVEMENT), entry("packetfly", Categories.MOVEMENT), entry("pearlrecall", Categories.MOVEMENT),
		entry("inventorymove", Categories.MOVEMENT), entry("inventorywalk", Categories.MOVEMENT),
		// Player
		entry("fastuse", Categories.PLAYER), entry("fastplace", Categories.PLAYER), entry("fastbreak", Categories.PLAYER),
		entry("autotool", Categories.PLAYER), entry("autoreplenish", Categories.PLAYER), entry("stackreplenish", Categories.PLAYER),
		entry("reach", Categories.PLAYER), entry("xcarry", Categories.PLAYER), entry("antiaim", Categories.PLAYER),
		entry("autorespawn", Categories.PLAYER), entry("autolog", Categories.PLAYER), entry("autodisconnect", Categories.PLAYER),
		entry("middleclick", Categories.PLAYER), entry("nointeract", Categories.PLAYER), entry("portals", Categories.PLAYER),
		entry("armorupgrader", Categories.PLAYER), entry("autoupgrade", Categories.PLAYER), entry("autosmith", Categories.PLAYER),
		entry("autofish", Categories.PLAYER), entry("autoshulker", Categories.PLAYER), entry("autoremount", Categories.PLAYER),
		entry("invmanager", Categories.PLAYER), entry("inventorymanager", Categories.PLAYER), entry("autosign", Categories.PLAYER),
		entry("antiafk", Categories.PLAYER), entry("blink", Categories.PLAYER), entry("ghosthand", Categories.PLAYER),
		entry("multitask", Categories.PLAYER), entry("nopush", Categories.PLAYER), entry("wallinteract", Categories.PLAYER),
		// Render
		entry("esp", Categories.RENDER), entry("tracers", Categories.RENDER), entry("nametags", Categories.RENDER),
		entry("fullbright", Categories.RENDER), entry("freecam", Categories.RENDER), entry("freelook", Categories.RENDER),
		entry("norender", Categories.RENDER), entry("xray", Categories.RENDER), entry("chams", Categories.RENDER),
		entry("holeesp", Categories.RENDER), entry("blockesp", Categories.RENDER), entry("storageesp", Categories.RENDER),
		entry("voidesp", Categories.RENDER), entry("burrowesp", Categories.RENDER), entry("shaders", Categories.RENDER),
		entry("viewmodel", Categories.RENDER), entry("zoom", Categories.RENDER), entry("crosshair", Categories.RENDER),
		entry("breakesp", Categories.RENDER), entry("blockhighlight", Categories.RENDER), entry("skybox", Categories.RENDER),
		entry("logoutspots", Categories.RENDER), entry("trajectories", Categories.RENDER), entry("itemesp", Categories.RENDER),
		entry("pearlesp", Categories.RENDER), entry("cameraclip", Categories.RENDER), entry("noweather", Categories.RENDER),
		entry("ambience", Categories.RENDER), entry("capes", Categories.RENDER), entry("tooltips", Categories.RENDER),
		entry("betterchat", Categories.MISC), entry("crystalchams", Categories.RENDER), entry("hitparticles", Categories.RENDER),
		entry("popchams", Categories.RENDER), entry("totempops", Categories.RENDER),
		// World
		entry("scaffold", Categories.WORLD), entry("airplace", Categories.WORLD), entry("nuker", Categories.WORLD),
		entry("autobuild", Categories.WORLD), entry("baritone", Categories.WORLD), entry("printer", Categories.WORLD),
		entry("highwaybuilder", Categories.WORLD), entry("tunneler", Categories.WORLD), entry("avoid", Categories.WORLD),
		entry("autoreplace", Categories.WORLD), entry("packetplace", Categories.WORLD), entry("liquidfiller", Categories.WORLD),
		// Misc
		entry("antikick", Categories.MISC), entry("discordrpc", Categories.MISC), entry("notifier", Categories.MISC),
		entry("notifications", Categories.MISC), entry("spammer", Categories.MISC), entry("autoreconnect", Categories.MISC),
		entry("friends", Categories.MISC), entry("betterportals", Categories.MISC), entry("packetlogger", Categories.MISC),
		entry("visualrange", Categories.MISC), entry("autoreply", Categories.MISC), entry("chatmanager", Categories.MISC),
		entry("fakeplayer", Categories.MISC), entry("antispam", Categories.MISC), entry("announcer", Categories.MISC)
	);

	private static Map.Entry<String, Category> entry(String name, Category category) {
		return Map.entry(name, category);
	}

	static Category of(BaseModule module) {
		Category fromPackage = fromPackage(module.getClass().getName());
		if (fromPackage != null) return fromPackage;
		Category fromName = NAMES.get(normalize(module.getName()));
		return fromName != null ? fromName : BozeAddon.fallbackCategory();
	}

	/** The shared category a Boze category name stands for, or null for one of Boze's own ("Addons", "Hud"). */
	static Category byName(String name) {
		String n = normalize(name);
		Category c = PACKAGES.get(n);
		return c != null ? c : CATEGORY_NAMES.get(n);
	}

	/** The category named by the innermost package segment that names one, if any. */
	static Category fromPackage(String className) {
		String[] parts = className.toLowerCase(Locale.ROOT).split("\\.");
		for (int i = parts.length - 2; i >= 0; i--) {
			Category c = PACKAGES.get(parts[i]);
			if (c != null) return c;
		}
		return null;
	}

	static String normalize(String s) {
		return s.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
	}
}
