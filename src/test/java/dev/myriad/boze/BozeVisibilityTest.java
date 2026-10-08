package dev.myriad.boze;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.option.ModeOption;
import dev.boze.api.option.SliderOption;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boze's client options keep their GUI's show condition on an internal object (seen on Boze for 26.2: a subclass of
 * the API option with one field holding the internal option, whose only BooleanSupplier is the condition). These fakes
 * have that shape, with readable names in place of Boze's obfuscated ones.
 */
class BozeVisibilityTest {
	enum Mode { Recast, Control }

	/** Boze's internal option: some state and the one condition. */
	static final class InternalOption {
		final String label;
		final BooleanSupplier shown;

		InternalOption(String label, BooleanSupplier shown) {
			this.label = label;
			this.shown = shown;
		}
	}

	/** An internal object with two conditions: ambiguous, so the bridge must not guess. */
	static final class TwoConditions {
		final BooleanSupplier first = () -> false;
		final BooleanSupplier second = () -> false;
	}

	static final class ClientSlider extends SliderOption {
		final Object internal;

		ClientSlider(AddonModule owner, String name, Object internal) {
			super(owner, name, "", 1, 0, 2, 1);
			this.internal = internal;
		}
	}

	/** A second option class, so its (ambiguous) shape is looked up on its own. */
	static final class AmbiguousSlider extends SliderOption {
		final Object internal;

		AmbiguousSlider(AddonModule owner, String name, Object internal) {
			super(owner, name, "", 1, 0, 2, 1);
			this.internal = internal;
		}
	}

	static final class Module extends AddonModule {
		final ModeOption<Mode> mode = new ModeOption<>(this, "Mode", "", Mode.Recast);
		final ClientSlider speed = new ClientSlider(this, "Speed", new InternalOption("Speed", () -> mode.getValue() == Mode.Control));
		final AmbiguousSlider ambiguous = new AmbiguousSlider(this, "Other", new TwoConditions());
		final SliderOption plain = new SliderOption(this, "Plain", "", 1, 0, 2, 1);

		Module() {
			super("VisibilityFake", "");
		}
	}

	@Test
	void modeOnlyOptionsFollowTheMode() {
		Module m = new Module();
		var shown = OptionBindings.visibility(m.speed);
		assertFalse(shown.get());
		m.mode.setValue(Mode.Control);
		assertTrue(shown.get());
	}

	@Test
	void unknownShapesShowAsBefore() {
		Module m = new Module();
		assertNull(BozeVisibility.of(m.ambiguous));
		assertNull(BozeVisibility.of(m.plain));
		assertTrue(OptionBindings.visibility(m.ambiguous).get());
		assertTrue(OptionBindings.visibility(m.plain).get());
	}
}
