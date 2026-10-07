package dev.myriad.boze;

import dev.myriad.api.addon.AddonContext;
import dev.myriad.api.addon.MyriadAddon;
import dev.myriad.api.event.events.TickEvent;
import dev.myriad.api.module.Category;
import dev.myriad.api.setting.SettingColor;
import dev.myriad.api.ui.widget.HBox;
import dev.myriad.api.ui.widget.Slider;

/**
 * Puts every Boze module in Myriad's menu. Boze (<a href="https://boze.dev">boze.dev</a>) is a separate, closed
 * utility mod with its own addon API; this addon reads its module list through that API and mirrors each module as a
 * Myriad module: same on/off state, settings translated to Myriad settings, keybind and module-list options shared.
 * Boze stays the owner of all of it (its config, profiles and key handling), Myriad is a second front end.
 *
 * <p>Boze comes up on its own schedule, possibly after Myriad, so the bridge waits for it: nothing is registered until
 * Boze reports its modules, and modules Boze's own addons add later are picked up as they appear.
 *
 * <p>This class is the only one that runs without Boze installed. Everything that touches Boze's classes lives in
 * {@link BozeBridge} and is loaded only once they're known to exist.
 */
public final class BozeAddon implements MyriadAddon {
	/** How long to wait for Boze before saying so, in ticks (a minute). */
	private static final int PATIENCE = 20 * 60;

	private static Category fallback;

	private AddonContext ctx;
	private BozeBridge bridge;
	private int waited;

	/** The category for Boze modules that fit none of the shared ones. */
	static Category fallbackCategory() {
		return fallback;
	}

	@Override
	public void registerCategories(AddonContext ctx) {
		fallback = ctx.registerCategory("Boze", "", SettingColor.role(SettingColor.Mode.SECONDARY));
	}

	@Override
	public void initialize(AddonContext ctx) {
		this.ctx = ctx;
		ctx.settingWidgets().register(RangeSetting.class, s -> {
			HBox row = new HBox(3);
			row.add(new Slider(() -> s.get().min(), v -> s.set(new RangeSetting.Range(v, s.get().max())), s.lower(), s.upper(), s.decimals()));
			row.add(new Slider(() -> s.get().max(), v -> s.set(new RangeSetting.Range(s.get().min(), v)), s.lower(), s.upper(), s.decimals()));
			return row;
		}, false);
	}

	@Override
	public void postInitialize(AddonContext ctx) {
		if (!bozeApiPresent()) {
			ctx.logger().info("Boze isn't installed: nothing to bridge");
			return;
		}
		ctx.events().listen(TickEvent.Pre.class, e -> tick());
	}

	private void tick() {
		if (bridge == null) {
			if (!BozeBridge.ready()) {
				if (++waited == PATIENCE) ctx.logger().warn("Boze's API is present but Boze hasn't reported its modules after a minute; still waiting");
				return;
			}
			bridge = new BozeBridge(ctx);
		}
		bridge.tick();
	}

	/** Whether Boze's addon API classes are on the classpath (Boze ships them; the loader alone doesn't). */
	private static boolean bozeApiPresent() {
		try {
			Class.forName("dev.boze.api.internal.Instances", false, BozeAddon.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}
}
