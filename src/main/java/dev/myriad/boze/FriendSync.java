package dev.myriad.boze;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Keeps Myriad's and Boze's friend lists the same. Neither side announces changes, so {@link #sync} compares both
 * lists with the last ones it saw: a name added on one side is added to the other, a name removed on one side is
 * removed from the other. The first sync merges the two lists, so nothing anyone was friends with is lost. Names
 * compare without regard to case, as Minecraft names do.
 */
final class FriendSync {
	/** One side's list and how to change it. */
	record Side(Supplier<Collection<String>> all, Consumer<String> add, Consumer<String> remove) {
	}

	private final Side myriad, boze;
	/** Names both sides held after the last sync; empty before the first, which therefore merges the two lists. */
	private final Set<String> last = names();

	FriendSync(Side myriad, Side boze) {
		this.myriad = myriad;
		this.boze = boze;
	}

	void sync() {
		Set<String> m = names(myriad.all().get());
		Set<String> b = names(boze.all().get());
		// A name both sides had that one side dropped was removed there; the other side follows.
		for (String n : last) {
			if (!m.contains(n) && b.contains(n)) boze.remove().accept(n);
			if (!b.contains(n) && m.contains(n)) myriad.remove().accept(n);
		}
		for (String n : m) if (!b.contains(n) && !last.contains(n)) boze.add().accept(n);
		for (String n : b) if (!m.contains(n) && !last.contains(n)) myriad.add().accept(n);
		// Remember only what both sides hold now: a name one side wouldn't take is offered again next time, never
		// mistaken for a removal.
		last.clear();
		last.addAll(names(myriad.all().get()));
		last.retainAll(names(boze.all().get()));
	}

	private static Set<String> names() {
		return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	}

	private static Set<String> names(Collection<String> from) {
		Set<String> s = names();
		for (String n : from) if (n != null && !n.isBlank()) s.add(n.trim());
		return s;
	}
}
