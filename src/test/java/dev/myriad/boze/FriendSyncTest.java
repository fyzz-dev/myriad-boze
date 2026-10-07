package dev.myriad.boze;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FriendSyncTest {
	private final Set<String> myriad = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	private final Set<String> boze = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	private final FriendSync sync = new FriendSync(
		new FriendSync.Side(() -> myriad, myriad::add, myriad::remove),
		new FriendSync.Side(() -> boze, boze::add, boze::remove));

	@Test
	void firstSyncMergesBothLists() {
		myriad.add("Alice");
		boze.add("Bob");
		sync.sync();
		assertEquals(Set.of("Alice", "Bob"), Set.copyOf(myriad));
		assertEquals(Set.of("Alice", "Bob"), Set.copyOf(boze));
	}

	@Test
	void addsAndRemovalsCarryOverBothWays() {
		sync.sync();
		myriad.add("Alice");
		boze.add("Bob");
		sync.sync();
		assertEquals(Set.of("Alice", "Bob"), Set.copyOf(boze));
		assertEquals(Set.of("Alice", "Bob"), Set.copyOf(myriad));

		myriad.remove("Bob");
		sync.sync();
		assertEquals(Set.of("Alice"), Set.copyOf(boze), "removed in Myriad, gone from Boze");

		boze.remove("alice");
		sync.sync();
		assertEquals(Set.of(), Set.copyOf(myriad), "removed in Boze (any case), gone from Myriad");
	}

	@Test
	void caseDiffersAreTheSameFriend() {
		myriad.add("Alice");
		boze.add("ALICE");
		sync.sync();
		assertEquals(1, myriad.size());
		assertEquals(1, boze.size());
		sync.sync();
		assertEquals(1, boze.size());
	}

	@Test
	void aSideThatRefusesAnAddIsNotFoughtWith() {
		// Boze adds nothing (say it rejects the name): the next sync must not keep trying forever or remove it from Myriad.
		FriendSync stubborn = new FriendSync(
			new FriendSync.Side(() -> myriad, myriad::add, myriad::remove),
			new FriendSync.Side(() -> boze, n -> {
			}, boze::remove));
		stubborn.sync();
		myriad.add("Alice");
		stubborn.sync();
		stubborn.sync();
		assertEquals(Set.of("Alice"), Set.copyOf(myriad));
		assertEquals(Set.of(), Set.copyOf(boze));
	}
}
