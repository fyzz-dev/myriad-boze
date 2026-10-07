package dev.myriad.boze;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.option.BindOption;
import dev.boze.api.option.ColorOption;
import dev.boze.api.option.ModeOption;
import dev.boze.api.option.PageOption;
import dev.boze.api.option.ParentOption;
import dev.boze.api.option.RangeSliderOption;
import dev.boze.api.option.SliderOption;
import dev.boze.api.option.ToggleOption;
import dev.boze.api.utility.input.Bind;
import dev.myriad.api.setting.BoolSetting;
import dev.myriad.api.setting.ColorSetting;
import dev.myriad.api.setting.DoubleSetting;
import dev.myriad.api.setting.EnumSetting;
import dev.myriad.api.setting.KeybindSetting;
import dev.myriad.api.setting.Setting;
import dev.myriad.api.setting.SettingColor;
import dev.myriad.api.setting.SettingGroup;
import dev.myriad.api.setting.Settings;
import dev.myriad.api.util.Keybind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boze options become the right Myriad settings, in the right groups, and values travel both ways. */
class OptionBindingsTest {
	enum Mode { Normal, Fast, Strict }

	/** A Boze module as an addon would declare one, with the option shapes seen in Boze's example addon. */
	static final class Fake extends AddonModule {
		final ToggleOption rotate = new ToggleOption(this, "Rotate", "Turn to face", true);
		final SliderOption delay = new SliderOption(this, "Delay", "Ticks between actions", 2, 0, 20, 1);
		final SliderOption speed = new SliderOption(this, "Speed", "Blocks per tick", 0.5, 0, 2, 0.05);
		final ModeOption<Mode> mode = new ModeOption<>(this, "Mode", "How", Mode.Fast);
		final BindOption swap = new BindOption(this, "SwapKey", "Swap items", GLFW.GLFW_KEY_R, false);
		// A toggle with children: shown only while it's on.
		final ToggleOption render = new ToggleOption(this, "Render", "Draw it", false);
		final ColorOption color = new ColorOption(this, "Color", "Box colour", new FakeColor(0xFF0000), 0.25f, 1.0f, render);
		final SliderOption width = new SliderOption(this, "Width", "Line width", 1, 0, 5, 0.5, render);
		// A page with a folder in it.
		final PageOption misc = new PageOption(this, "Misc", "More");
		final RangeSliderOption distance = new RangeSliderOption(this, "Distance", "Range", 4, 16, 0, 64, 1, misc);
		final ParentOption sounds = new ParentOption(this, "Sounds", "Noises");
		final ToggleOption ding = new ToggleOption(this, "Enabled", "Play", true, sounds);
		// Two options named alike, under different toggles, in the same group.
		final ToggleOption a = new ToggleOption(this, "Alpha", "", true);
		final ToggleOption b = new ToggleOption(this, "Beta", "", true);
		final SliderOption aColor = new SliderOption(this, "Amount", "", 1, 0, 2, 1, a);
		final SliderOption bColor = new SliderOption(this, "Amount", "", 1, 0, 2, 1, b);

		Fake() {
			super("FakeModule", "A fake");
			sounds.setParent(misc);
		}
	}

	@BeforeAll
	static void colors() {
		OptionBindings.colors = FakeColor::new;
	}

	private static Setting<?> setting(Settings s, String key) {
		return s.get(key).orElseThrow(() -> new AssertionError("no setting " + key + " in " + s.all().stream().map(x -> x.group().name() + "." + x.name()).toList()));
	}

	@Test
	void typesAndGroups() {
		Fake boze = new Fake();
		Settings settings = new Settings();
		new OptionBindings(boze, settings);

		assertEquals(List.of("General", "Misc", "Misc · Sounds"), settings.groups().stream().map(SettingGroup::name).toList());
		assertTrue(setting(settings, "general.rotate") instanceof BoolSetting);
		assertTrue(setting(settings, "general.delay") instanceof DoubleSetting d && d.decimals() == 0 && d.min() == 0 && d.max() == 20);
		assertTrue(setting(settings, "general.speed") instanceof DoubleSetting d && d.decimals() == 2);
		assertTrue(setting(settings, "general.mode") instanceof EnumSetting<?> e && e.get() == Mode.Fast);
		assertTrue(setting(settings, "general.swap_key") instanceof KeybindSetting k && k.get().equals(Keybind.key(GLFW.GLFW_KEY_R)));
		assertTrue(setting(settings, "general.color") instanceof ColorSetting);
		assertTrue(setting(settings, "general.color_outline") instanceof DoubleSetting);
		assertTrue(setting(settings, "misc.distance") instanceof RangeSetting r && r.get().equals(new RangeSetting.Range(4, 16)));
		assertTrue(setting(settings, "misc_sounds.enabled") instanceof BoolSetting);
		// The module's own bind is the mirror's keybind, not a setting.
		assertTrue(settings.get("general.bind").isEmpty());
		// Alike names are told apart by their parent.
		assertTrue(setting(settings, "general.alpha_amount") instanceof DoubleSetting);
		assertTrue(setting(settings, "general.beta_amount") instanceof DoubleSetting);
	}

	@Test
	void childrenHideWhileTheirToggleIsOff() {
		Fake boze = new Fake();
		Settings settings = new Settings();
		new OptionBindings(boze, settings);
		Setting<?> width = setting(settings, "general.width");
		assertFalse(width.isVisible());
		boze.render.setValue(true);
		assertTrue(width.isVisible());
		assertTrue(setting(settings, "general.render").isVisible());
	}

	@Test
	void myriadEditsReachBoze() {
		Fake boze = new Fake();
		Settings settings = new Settings();
		new OptionBindings(boze, settings);

		((BoolSetting) setting(settings, "general.rotate")).set(false);
		assertFalse(boze.rotate.getValue());
		((DoubleSetting) setting(settings, "general.delay")).set(7.0);
		assertEquals(7.0, boze.delay.getValue());
		((DoubleSetting) setting(settings, "general.delay")).set(99.0);
		assertEquals(20.0, boze.delay.getValue(), "clamped by Myriad's range, which is Boze's");
		@SuppressWarnings("unchecked") EnumSetting<Mode> mode = (EnumSetting<Mode>) setting(settings, "general.mode");
		mode.set(Mode.Strict);
		assertEquals(Mode.Strict, boze.mode.getValue());
		((RangeSetting) setting(settings, "misc.distance")).set(new RangeSetting.Range(10, 12));
		assertEquals(10, boze.distance.getMin());
		assertEquals(12, boze.distance.getMax());

		KeybindSetting swap = (KeybindSetting) setting(settings, "general.swap_key");
		swap.set(Keybind.mouse(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
		assertTrue(boze.swap.getValue().isButton());
		assertEquals(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, boze.swap.getBind());
		swap.set(Keybind.key(GLFW.GLFW_KEY_F, GLFW.GLFW_MOD_CONTROL));
		assertEquals(GLFW.GLFW_KEY_F, boze.swap.getBind());
		assertEquals(0, swap.get().modifiers(), "Boze has no modifiers, so Myriad shows the key that will fire");
		swap.set(Keybind.NONE);
		assertEquals(-1, boze.swap.getBind());

		ColorSetting color = (ColorSetting) setting(settings, "general.color");
		assertEquals(0x40FF0000, color.argb(), "red at a quarter opacity");
		color.set(SettingColor.of(0x8000FF00));
		assertEquals(0x00FF00, boze.color.getValue().color.getPacked());
		assertEquals(0.5f, boze.color.getValue().fillOpacity, 0.01f);
		assertEquals(1.0f, boze.color.getValue().outlineOpacity, 0.01f);
		((DoubleSetting) setting(settings, "general.color_outline")).set(0.3);
		assertEquals(0.3f, boze.color.getValue().outlineOpacity, 0.01f);
		assertEquals(0.5f, boze.color.getValue().fillOpacity, 0.01f);
	}

	@Test
	void pullFollowsBoze() {
		Fake boze = new Fake();
		Settings settings = new Settings();
		OptionBindings bindings = new OptionBindings(boze, settings);

		boze.rotate.setValue(false);
		boze.delay.setValue(13.0);
		boze.mode.setValue(Mode.Normal);
		boze.distance.setValue(new double[]{20, 30});
		boze.swap.setBind(GLFW.GLFW_KEY_G, false);
		boze.color.setValue(new ColorOption.Value("_default", new FakeColor(0x0000FF), 1.0f, 0.5f));
		int[] changes = {0};
		setting(settings, "general.rotate").onChanged(v -> changes[0]++);

		bindings.pull();
		assertFalse(((BoolSetting) setting(settings, "general.rotate")).get());
		assertEquals(13.0, ((DoubleSetting) setting(settings, "general.delay")).get());
		assertEquals(Mode.Normal, setting(settings, "general.mode").get());
		assertEquals(new RangeSetting.Range(20, 30), setting(settings, "misc.distance").get());
		assertEquals(Keybind.key(GLFW.GLFW_KEY_G), setting(settings, "general.swap_key").get());
		assertEquals(0xFF0000FF, ((ColorSetting) setting(settings, "general.color")).argb());
		assertEquals(0.5, ((DoubleSetting) setting(settings, "general.color_outline")).get());
		assertEquals(1, changes[0]);

		// Pulling what Myriad already has changes nothing, and pulling never writes back.
		bindings.pull();
		assertEquals(1, changes[0]);
		assertFalse(boze.rotate.getValue());
	}

	@Test
	void helpers() {
		assertEquals(0, OptionBindings.decimalsOf(1));
		assertEquals(1, OptionBindings.decimalsOf(0.5));
		assertEquals(2, OptionBindings.decimalsOf(0.05));
		assertEquals(1, OptionBindings.decimalsOf(0.1));
		assertEquals(2, OptionBindings.decimalsOf(0));

		assertEquals("Auto Crystal", BozeModule.displayName("AutoCrystal"));
		assertEquals("X Carry", BozeModule.displayName("XCarry"));
		assertEquals("Auto EXP", BozeModule.displayName("AutoEXP"));
		assertEquals("ESP", BozeModule.displayName("ESP"));
		assertEquals("No Fall", BozeModule.displayName("NoFall"));
		assertEquals("Already Spaced", BozeModule.displayName("Already Spaced"));
		assertEquals("Line one line two", BozeModule.oneLine("Line one\\nline two"));

		assertNull(BozeCategories.fromPackage("dev.boze.client.modules.AutoCrystal"));
		assertNotNull(BozeCategories.fromPackage("dev.boze.client.modules.combat.AutoCrystal"));
		assertEquals("Combat", BozeCategories.fromPackage("dev.boze.client.modules.combat.AutoCrystal").name());
		assertEquals("Render", BozeCategories.byName("Visuals").name());
		assertEquals("Misc", BozeCategories.byName("Client").name());
		assertNull(BozeCategories.byName("Addons"));
	}
}
