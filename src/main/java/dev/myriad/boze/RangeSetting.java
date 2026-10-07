package dev.myriad.boze;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.myriad.api.setting.Setting;

import java.util.function.Supplier;

/** A low/high pair, for Boze's two-handle range sliders. Its editor (two sliders) is registered in {@link BozeAddon}. */
public class RangeSetting extends Setting<RangeSetting.Range> {
	public record Range(double min, double max) {
	}

	private final double lower, upper;
	private final int decimals;

	public RangeSetting(String name, String description, Range defaultValue, Supplier<Boolean> visible, double lower, double upper, int decimals) {
		super(name, description, defaultValue, visible);
		this.lower = lower;
		this.upper = upper;
		this.decimals = decimals;
	}

	public double lower() {
		return lower;
	}

	public double upper() {
		return upper;
	}

	public int decimals() {
		return decimals;
	}

	@Override
	protected Range validate(Range v) {
		if (v == null) return defaultValue;
		double scale = Math.pow(10, decimals);
		double a = Math.clamp(Math.round(v.min() * scale) / scale, lower, upper);
		double b = Math.clamp(Math.round(v.max() * scale) / scale, lower, upper);
		return new Range(Math.min(a, b), Math.max(a, b));
	}

	@Override
	public JsonElement toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("min", value.min());
		o.addProperty("max", value.max());
		return o;
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonObject()) {
			JsonObject o = json.getAsJsonObject();
			set(new Range(o.get("min").getAsDouble(), o.get("max").getAsDouble()));
		}
	}

	@Override
	public boolean parse(String input) {
		String[] p = input.trim().split("[\\s,]+");
		if (p.length != 2) return false;
		try {
			set(new Range(Double.parseDouble(p[0]), Double.parseDouble(p[1])));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public String valueString() {
		String f = "%." + decimals + "f";
		return String.format(f + " – " + f, value.min(), value.max());
	}
}
