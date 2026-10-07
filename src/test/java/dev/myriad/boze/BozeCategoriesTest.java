package dev.myriad.boze;

import dev.boze.api.addon.AddonModule;
import dev.myriad.api.module.Categories;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BozeCategoriesTest {
	static final class Named extends AddonModule {
		Named(String name) {
			super(name, "");
		}
	}

	@Test
	void knownNamesLandInSharedCategories() {
		assertEquals(Categories.COMBAT, BozeCategories.of(new Named("AutoCrystal")));
		assertEquals(Categories.MOVEMENT, BozeCategories.of(new Named("ElytraFly")));
		assertEquals(Categories.RENDER, BozeCategories.of(new Named("Hole ESP")));
		assertEquals(Categories.PLAYER, BozeCategories.of(new Named("auto_tool")));
	}

	@Test
	void unknownNamesFallBack() {
		// The fallback category is registered by the addon at startup; here nothing is, so null stands for it.
		assertNull(BozeCategories.of(new Named("SomethingNew")));
	}
}
