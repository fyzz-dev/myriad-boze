package dev.myriad.boze;

import dev.boze.api.client.module.BaseModule;
import dev.boze.api.utility.input.Bind;
import dev.myriad.api.module.Category;
import dev.myriad.api.module.Module;
import dev.myriad.api.util.Keybind;

/**
 * A Myriad module standing in for one Boze module. Toggling it toggles the Boze module; its settings are the Boze
 * module's options as Myriad settings ({@link OptionBindings}); its keybind, hold mode and module-list visibility are
 * Boze's. Boze owns and saves all of it ({@link #isMirror}): {@link BozeBridge} reads Boze's side back regularly, and
 * every edit here is written straight to Boze.
 *
 * <p>Chat feedback starts off because Boze announces toggles itself (its "notify" per module, set in its GUI); turn it
 * on for Myriad's line as well.
 */
final class BozeModule extends Module {
	private final BaseModule boze;
	private final OptionBindings options;
	/** Set while copying Boze's values in, so the change listeners don't write them straight back. */
	private boolean pulling;

	BozeModule(BaseModule boze, Category category) {
		super(category, displayName(boze.getName()), oneLine(boze.getDescription()));
		this.boze = boze;
		pulling = true;
		try {
			keybind.set(toKeybind(boze.getBind()));
			holdMode.set(boze.isOnlyWhileHolding());
			visibleInList.set(boze.isVisible());
			chatFeedback.set(false);
		} finally {
			pulling = false;
		}
		keybind.onChanged(k -> {
			if (pulling) return;
			boze.setBind(toBind(k));
			// Boze binds have no modifiers; show what will actually trigger.
			if (k.modifiers() != 0) keybind.set(new Keybind(k.code(), k.mouse(), 0));
		});
		holdMode.onChanged(v -> {
			if (!pulling) boze.setOnlyWhileHolding(v);
		});
		visibleInList.onChanged(v -> {
			if (!pulling) boze.setVisible(v);
		});
		options = new OptionBindings(boze, settings);
	}

	BaseModule boze() {
		return boze;
	}

	@Override
	public boolean isMirror() {
		return true;
	}

	@Override
	protected void onEnable() {
		if (!boze.getState()) boze.setState(true);
	}

	@Override
	protected void onDisable() {
		if (boze.getState()) boze.setState(false);
	}

	/** Follows Boze's on/off state (its keybind, GUI, commands and profiles all change it behind Myriad's back). */
	void pullState() {
		boolean on = boze.getState();
		if (on != isEnabled()) setEnabledSilently(on);
	}

	/** Copies Boze's current option values, bind and module-list options in, where they differ. */
	void pullSettings() {
		pulling = true;
		try {
			Keybind k = keybind.get();
			Bind b = boze.getBind();
			if (!sameKey(k, b)) keybind.set(toKeybind(b));
			holdMode.set(boze.isOnlyWhileHolding());
			visibleInList.set(boze.isVisible());
			options.pull();
		} finally {
			pulling = false;
		}
	}

	@Override
	public String hudInfo() {
		String info = boze.getArrayListInfo();
		return info == null || info.isBlank() ? null : info;
	}

	static boolean sameKey(Keybind k, Bind b) {
		boolean set = b != null && b.getBind() >= 0;
		if (!set) return !k.isSet();
		return k.isSet() && k.mouse() == b.isButton() && k.code() == b.getBind();
	}

	static Keybind toKeybind(Bind b) {
		if (b == null || b.getBind() < 0) return Keybind.NONE;
		return b.isButton() ? Keybind.mouse(b.getBind()) : Keybind.key(b.getBind());
	}

	static Bind toBind(Keybind k) {
		return k.isSet() ? new Bind(k.mouse(), k.code()) : new Bind(false, -1);
	}

	/** "AutoCrystal" reads as "Auto Crystal", "XCarry" as "X Carry", "AutoEXP" as "Auto EXP"; names with spaces stay. */
	static String displayName(String name) {
		if (name == null || name.isBlank()) return "?";
		if (name.contains(" ")) return name;
		StringBuilder sb = new StringBuilder(name.length() + 4);
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (i > 0 && Character.isUpperCase(c)) {
				char prev = name.charAt(i - 1);
				boolean nextLower = i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
				if (Character.isLowerCase(prev) || Character.isDigit(prev) || (Character.isUpperCase(prev) && nextLower)) sb.append(' ');
			}
			sb.append(c);
		}
		return sb.toString();
	}

	static String oneLine(String s) {
		return s == null ? "" : s.replace("\\n", " ").replace('\n', ' ').replaceAll("\\s+", " ").trim();
	}
}
