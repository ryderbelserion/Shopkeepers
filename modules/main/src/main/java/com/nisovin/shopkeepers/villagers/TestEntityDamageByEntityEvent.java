package com.nisovin.shopkeepers.villagers;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * A test event that is called to check if a player can damage the given entity (e.g. when the
 * player tries to hire a villager).
 */
public class TestEntityDamageByEntityEvent extends EntityDamageByEntityEvent {

	public TestEntityDamageByEntityEvent(Entity damager, Entity damagee) {
		super(
				damager,
				damagee,
				DamageCause.CUSTOM,
				DamageSource.builder(Unsafe.assertNonNull(DamageType.GENERIC))
						.withCausingEntity(damager)
						.withDirectEntity(damager)
						.build(),
				1.0D
		);
	}
}
