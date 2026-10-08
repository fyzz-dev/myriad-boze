package dev.myriad.boze;

import dev.boze.api.option.Option;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * When Boze's own GUI shows an option. Boze's client modules don't use the API's visibility ({@link Option#isVisible()}
 * is always true for them): each of their options is a subclass of an API option that holds Boze's internal option,
 * and that keeps the condition its GUI uses (mostly "only in these modes") as its one {@link BooleanSupplier}.
 * <p>
 * Boze is obfuscated and its names change between versions, so the condition is found by shape, not name: a field the
 * option's own (non-API) class declares, holding an object with exactly one {@code BooleanSupplier} field. Anything
 * else (a plain API option, as Boze addons use, or a Boze update that changes the shape) gets no condition, and the
 * option shows as before.
 */
final class BozeVisibility {
	/** Per option class, the field holding Boze's internal option, if it has one. */
	private static final Map<Class<?>, Optional<Field>> HOLDERS = new ConcurrentHashMap<>();
	/** Per internal option class, its condition field, if it has exactly one. */
	private static final Map<Class<?>, Optional<Field>> CONDITIONS = new ConcurrentHashMap<>();

	private BozeVisibility() {
	}

	/** Boze's own condition for showing {@code o}, or null if it has none this bridge can find. */
	static BooleanSupplier of(Option<?> o) {
		try {
			Optional<Field> holder = HOLDERS.computeIfAbsent(o.getClass(), c -> holder(o));
			if (holder.isEmpty()) return null;
			Object inner = holder.get().get(o);
			if (inner == null) return null;
			Optional<Field> condition = CONDITIONS.computeIfAbsent(inner.getClass(), BozeVisibility::onlyCondition);
			return condition.isEmpty() ? null : (BooleanSupplier) condition.get().get(inner);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	/** The field of {@code o}'s own (non-API) classes that holds an object with a single condition. */
	private static Optional<Field> holder(Option<?> o) {
		for (Class<?> c = o.getClass(); c != null && !isApi(c); c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
				try {
					f.setAccessible(true);
					Object inner = f.get(o);
					if (inner != null && onlyCondition(inner.getClass()).isPresent()) return Optional.of(f);
				} catch (ReflectiveOperationException | RuntimeException e) {
					// Not this one.
				}
			}
		}
		return Optional.empty();
	}

	/** The one BooleanSupplier field of {@code type} or its superclasses; empty if there are none or several. */
	private static Optional<Field> onlyCondition(Class<?> type) {
		Field found = null;
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) || f.getType() != BooleanSupplier.class) continue;
				if (found != null) return Optional.empty();
				found = f;
			}
		}
		if (found == null) return Optional.empty();
		try {
			found.setAccessible(true);
			return Optional.of(found);
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private static boolean isApi(Class<?> c) {
		return c == Object.class || c.getName().startsWith("dev.boze.api.");
	}
}
