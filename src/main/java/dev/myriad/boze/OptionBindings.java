package dev.myriad.boze;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.client.module.BaseModule;
import dev.boze.api.option.BindOption;
import dev.boze.api.option.ColorOption;
import dev.boze.api.option.ModeOption;
import dev.boze.api.option.Option;
import dev.boze.api.option.ParentOption;
import dev.boze.api.option.RangeSliderOption;
import dev.boze.api.option.SliderOption;
import dev.boze.api.option.ToggleOption;
import dev.boze.api.render.ClientColor;
import dev.boze.api.render.ColorMaker;
import dev.myriad.api.setting.BoolSetting;
import dev.myriad.api.setting.ColorSetting;
import dev.myriad.api.setting.DoubleSetting;
import dev.myriad.api.setting.EnumSetting;
import dev.myriad.api.setting.KeybindSetting;
import dev.myriad.api.setting.SettingColor;
import dev.myriad.api.setting.SettingGroup;
import dev.myriad.api.setting.Settings;
import dev.myriad.api.util.ColorUtil;
import dev.myriad.api.util.Keybind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Boze options as Myriad settings. Boze's options form a tree: a {@code PageOption} is a tab of the module's GUI and a
 * {@code ParentOption} a folder, both pure containers; any other option can also have children (a toggle whose
 * sub-options only matter while it's on). Myriad has flat groups, so:
 * <ul>
 *   <li>a page or folder becomes a setting group named after it ("Render", or "Render · Boxes" when nested), and
 *       options outside any go in General;</li>
 *   <li>a toggle's children sit in the same group and are shown only while it's on, as Myriad settings do with
 *       {@code visible};</li>
 *   <li>Toggle → Bool, Slider → Double (decimals from the step), RangeSlider → {@link RangeSetting}, Mode → Enum,
 *       Bind → Keybind, Color → Color plus an "… Outline" opacity when Boze keeps fill and outline apart.</li>
 * </ul>
 * Each binding writes Myriad edits to Boze as they happen and, on {@link #pull}, copies Boze's value in. Boze colours
 * can be animated (rainbows, gradients); Myriad shows the colour as it was when it last changed in Boze, and a colour
 * picked in Myriad is given to Boze as a plain colour.
 */
final class OptionBindings {
	private static final Logger LOG = LoggerFactory.getLogger("Myriad/Boze");
	private static final Field CHILDREN = childrenField();

	/** Makes Boze colours; tests swap it since the real one needs the running client. */
	static IntFunction<ClientColor> colors = ColorMaker::staticColor;

	interface Binding {
		void pull();
	}

	private final List<Binding> bindings = new ArrayList<>();
	private boolean pulling;

	OptionBindings(BaseModule boze, Settings settings) {
		Option<?> moduleBind = boze instanceof AddonModule a ? a.getBindOption() : null;
		List<Option<?>> all = flatten(boze.getOptions());
		Map<Option<?>, String> groups = new IdentityHashMap<>();
		Map<String, Map<String, Integer>> nameCounts = new HashMap<>();
		for (Option<?> o : all) {
			if (o instanceof ParentOption || o == moduleBind || isModuleBind(o)) continue;
			String group = groupOf(o);
			groups.put(o, group);
			nameCounts.computeIfAbsent(group, g -> new HashMap<>()).merge(o.name, 1, Integer::sum);
		}
		Map<String, Set<String>> used = new HashMap<>();
		for (Option<?> o : all) {
			String group = groups.get(o);
			if (group == null) continue;
			String name = BozeModule.displayName(o.name);
			if (nameCounts.get(group).get(o.name) > 1 && o.getParent() != null) name = BozeModule.displayName(o.getParent().name) + " " + name;
			Set<String> taken = used.computeIfAbsent(group, g -> new java.util.HashSet<>());
			String unique = name;
			for (int i = 2; !taken.add(unique); i++) unique = name + " " + i;
			try {
				bind(settings.group(group), unique, o);
			} catch (RuntimeException e) {
				LOG.warn("Skipping option {} of {}: {}", o.getFullName(), boze.getName(), e.toString());
			}
		}
	}

	/** Copies every option's current Boze value into its setting. */
	void pull() {
		pulling = true;
		try {
			for (Binding b : bindings) b.pull();
		} finally {
			pulling = false;
		}
	}

	List<Binding> bindings() {
		return Collections.unmodifiableList(bindings);
	}

	private void bind(SettingGroup group, String name, Option<?> o) {
		String desc = BozeModule.oneLine(o.description);
		Supplier<Boolean> visible = visibility(o);
		switch (o) {
			case ToggleOption t -> {
				BoolSetting s = group.bool(name).description(desc).defaultValue(t.getValue()).visible(visible).onChanged(v -> push(() -> t.setValue(v))).build();
				bindings.add(() -> s.set(t.getValue()));
			}
			case SliderOption sl -> {
				int decimals = decimalsOf(sl.step);
				DoubleSetting s = group.doubleSetting(name).description(desc).defaultValue(sl.getValue()).range(sl.min, sl.max).decimals(decimals)
					.visible(visible).onChanged(v -> push(() -> sl.setValue(v))).build();
				bindings.add(() -> s.set(sl.getValue()));
			}
			case RangeSliderOption r -> {
				RangeSetting s = group.add(new RangeSetting(name, desc, new RangeSetting.Range(r.getMin(), r.getMax()), visible, r.min, r.max, decimalsOf(r.step)));
				s.onChanged(v -> push(() -> r.setValue(new double[]{v.min(), v.max()})));
				bindings.add(() -> s.set(new RangeSetting.Range(r.getMin(), r.getMax())));
			}
			case ModeOption<?> m -> bindMode(group, name, desc, visible, m);
			case BindOption b -> {
				KeybindSetting s = group.keybind(name).description(desc).defaultValue(BozeModule.toKeybind(b.getValue())).visible(visible).build();
				s.onChanged(k -> push(() -> {
					b.setValue(BozeModule.toBind(k));
					// Boze binds have no modifiers; show what will actually trigger.
					if (k.modifiers() != 0) s.set(new Keybind(k.code(), k.mouse(), 0));
				}));
				bindings.add(() -> {
					if (!BozeModule.sameKey(s.get(), b.getValue())) s.set(BozeModule.toKeybind(b.getValue()));
				});
			}
			case ColorOption c -> bindColor(group, name, desc, visible, c);
			default -> LOG.debug("No Myriad setting for Boze option {} ({})", o.getFullName(), o.getClass().getName());
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <E extends Enum<E>> void bindMode(SettingGroup group, String name, String desc, Supplier<Boolean> visible, ModeOption<?> raw) {
		ModeOption<E> m = (ModeOption<E>) raw;
		EnumSetting<E> s = group.enumSetting(name, m.getValue()).description(desc).visible(visible).onChanged(v -> push(() -> m.setValue(v))).build();
		bindings.add(() -> s.set(m.getValue()));
	}

	private void bindColor(SettingGroup group, String name, String desc, Supplier<Boolean> visible, ColorOption c) {
		ColorOption.Value start = c.getValue();
		ColorSetting color = group.color(name).description(desc).defaultValue(argb(start)).visible(visible).build();
		DoubleSetting outline = c.isSingleOpacity() ? null
			: group.doubleSetting(name + " Outline").description("Outline opacity of " + name + ".").defaultValue(start.outlineOpacity).range(0, 1).decimals(2).visible(visible).build();
		var last = new Object() {
			ColorOption.Value value = start;
			float fill = start.fillOpacity, outlineOpacity = start.outlineOpacity;
		};
		Runnable pushColor = () -> {
			int argb = color.argb();
			float fill = ColorUtil.alpha(argb) / 255f;
			ClientColor cc = colors.apply(argb & 0xFFFFFF);
			ColorOption.Value v = outline == null ? new ColorOption.Value("_default", cc, fill)
				: new ColorOption.Value("_default", cc, fill, outline.get().floatValue());
			c.setValue(v);
			last.value = c.getValue();
			last.fill = last.value.fillOpacity;
			last.outlineOpacity = last.value.outlineOpacity;
		};
		color.onChanged(v -> push(pushColor));
		if (outline != null) outline.onChanged(v -> push(pushColor));
		bindings.add(() -> {
			ColorOption.Value v = c.getValue();
			if (v == last.value && v.fillOpacity == last.fill && v.outlineOpacity == last.outlineOpacity) return;
			last.value = v;
			last.fill = v.fillOpacity;
			last.outlineOpacity = v.outlineOpacity;
			color.set(SettingColor.of(argb(v)));
			if (outline != null) outline.set((double) v.outlineOpacity);
		});
	}

	private void push(Runnable write) {
		if (!pulling) write.run();
	}

	static int argb(ColorOption.Value v) {
		int rgb = v.color == null ? 0xFFFFFF : v.color.getPacked() & 0xFFFFFF;
		return ColorUtil.withAlpha(rgb, Math.clamp(Math.round(v.fillOpacity * 255), 0, 255));
	}

	/** How many decimals a slider with this step needs: 1 → 0, 0.5 → 1, 0.05 → 2. */
	static int decimalsOf(double step) {
		if (!(step > 0)) return 2;
		int d = 0;
		double s = step;
		while (d < 4 && Math.abs(s - Math.rint(s)) > 1e-6) {
			s *= 10;
			d++;
		}
		return d;
	}

	/** The module's own keybind shows up as a root "Bind" option on addon modules; the mirror's keybind covers it. */
	private static boolean isModuleBind(Option<?> o) {
		return o instanceof BindOption && o.getParent() == null && o.name.equalsIgnoreCase("Bind");
	}

	/** The group an option goes in: its containing pages and folders, outermost first, or General. */
	static String groupOf(Option<?> o) {
		List<String> names = new ArrayList<>(2);
		for (Option<?> p = o.getParent(); p != null; p = p.getParent()) {
			if (p instanceof ParentOption) names.addFirst(BozeModule.displayName(p.name));
		}
		return names.isEmpty() ? "General" : String.join(" · ", names);
	}

	/**
	 * Visible when Boze's GUI would show it (its own condition, mostly the mode, see {@link BozeVisibility}, and the
	 * API's), and every toggle it sits under is on.
	 */
	static Supplier<Boolean> visibility(Option<?> o) {
		List<ToggleOption> gates = new ArrayList<>(1);
		for (Option<?> p = o.getParent(); p != null; p = p.getParent()) if (p instanceof ToggleOption t) gates.add(t);
		BooleanSupplier boze = BozeVisibility.of(o);
		return () -> {
			try {
				if (!o.isVisible() || boze != null && !boze.getAsBoolean()) return false;
				for (ToggleOption t : gates) if (!t.getValue()) return false;
				return true;
			} catch (RuntimeException e) {
				return true;
			}
		};
	}

	/**
	 * Every option reachable from {@code roots}, each once, parents before children. Boze's list may already be flat
	 * or hold only the roots; children are reached through the option's (private) child list either way.
	 */
	static List<Option<?>> flatten(List<Option<?>> roots) {
		Map<Option<?>, Boolean> seen = new LinkedHashMap<>();
		for (Option<?> o : roots) collect(o, seen);
		return new ArrayList<>(seen.keySet());
	}

	private static void collect(Option<?> o, Map<Option<?>, Boolean> seen) {
		if (o == null || seen.containsKey(o)) return;
		seen.put(o, Boolean.TRUE);
		for (Option<?> child : childrenOf(o)) collect(child, seen);
	}

	@SuppressWarnings("unchecked")
	static List<Option<?>> childrenOf(Option<?> o) {
		if (CHILDREN == null) return List.of();
		try {
			List<Option<?>> children = (List<Option<?>>) CHILDREN.get(o);
			return children == null ? List.of() : children;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return List.of();
		}
	}

	private static Field childrenField() {
		try {
			Field f = Option.class.getDeclaredField("children");
			f.setAccessible(true);
			return f;
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOG.warn("Boze's Option has no reachable child list; nested options are bridged only if Boze lists them itself");
			return null;
		}
	}
}
