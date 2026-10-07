package dev.myriad.boze;

import dev.boze.api.render.ClientColor;
import dev.boze.api.render.ClientColorBinding;

/** A Boze colour that needs no running client. */
final class FakeColor extends ClientColor {
	private final int packed;

	FakeColor(int packed) {
		this.packed = packed & 0xFFFFFF;
	}

	@Override
	public String getIdentifier() {
		return "_default";
	}

	@Override
	public ClientColor copy() {
		return new FakeColor(packed);
	}

	@Override
	public void choose(ClientColorBinding binding) {
	}

	@Override
	public void unchoose(ClientColorBinding binding) {
	}

	@Override
	public void delete() {
	}

	@Override
	public int getRed() {
		return packed >> 16 & 0xFF;
	}

	@Override
	public int getGreen() {
		return packed >> 8 & 0xFF;
	}

	@Override
	public int getBlue() {
		return packed & 0xFF;
	}

	@Override
	public int getPacked() {
		return packed;
	}
}
