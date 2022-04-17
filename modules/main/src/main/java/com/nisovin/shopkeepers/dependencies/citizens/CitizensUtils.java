package com.nisovin.shopkeepers.dependencies.citizens;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;

/**
 * Utilities related to the Citizens plugin and Citizens NPCs.
 */
public final class CitizensUtils {

	public static boolean isNPC(Entity entity) {
		Validate.notNull(entity, "entity is null");
		return entity.hasMetadata("NPC");
	}

	/**
	 * This class can only be accessed when the Citizens plugin is loaded.
	 */
	public static final class Internal {

		public static DataKey toDataKey(DataContainer dataContainer) {
			Validate.notNull(dataContainer, "dataContainer is null");
			DataKey dataKey = new MemoryDataKey();
			insertIntoDataKey(dataKey, "", dataContainer);
			return dataKey;
		}

		private static void insertIntoDataKey(
				DataKey dataKey,
				String parentPath,
				DataContainer dataContainer
		) {
			assert dataKey != null && dataContainer != null;
			dataContainer.getValues().forEach((key, value) -> {
				String path = parentPath.isEmpty() ? key : (parentPath + "." + key);
				DataContainer valueContainer = DataContainer.of(value);
				if (valueContainer != null) {
					insertIntoDataKey(dataKey, path, valueContainer);
				} else {
					dataKey.setRaw(path, value);
				}
			});
		}

		public static DataContainer toDataContainer(MemoryDataKey dataKey) {
			Validate.notNull(dataKey, "dataKey is null");
			// MemoryDataKey#getRaw("") returns the root MemoryConfiguration:
			return DataContainer.ofNonNull(Unsafe.assertNonNull(dataKey.getRaw("")));
		}

		public static DataContainer saveNpc(NPC npc) {
			Validate.notNull(npc, "npc is null");
			MemoryDataKey dataKey = new MemoryDataKey();
			npc.save(dataKey);
			return toDataContainer(dataKey);
		}

		public static void loadNpc(NPC npc, DataContainer npcData) {
			Validate.notNull(npc, "npc is null");
			Validate.notNull(npcData, "npcData is null");

			// For some traits and NPC properties it may be necessary to respawn the NPC in order
			// for the changes to have an effect. For example, loading a new stored location has no
			// effect on the location of any currently spawned NPC entity. And some properties, such
			// as the NPC type or name, are not applied until the NPC entity is recreated.
			// Note: We don't use NPC#isSpawned here because it takes into account whether the
			// entity has died.
			if (npc.getEntity() != null) {
				if (!npc.despawn()) {
					Log.warning("Failed to despawn Citizens NPC " + npc.getId()
							+ "! Some changes to the NPC's data might have no effect!");
				}
			}

			// Note: Some properties, such as the id or unique id, cannot be loaded.
			// Note: Loading the MobType trait does not automatically change the NPC's type. We
			// therefore need to manually apply the loaded NPC type.
			// Note: NPC#load might automatically respawn the NPC again.
			// Note: Citizens ignores the loaded MobType when it respawns the NPC, and instead even
			// overwrites the stored MobType with the NPC's previous and still cached entity type.
			// We therefore need to manually load and apply the mob type before we load the rest of
			// the NPC data.
			DataKey npcDataKey = toDataKey(npcData);
			String mobTypeName = npcDataKey.getString("traits.type", EntityType.PLAYER.name());
			// TODO This does not take any (version-specific) mob type migrations into account (e.g.
			// for pig zombies). However, these migrations are currently also broken in Citizens
			// itself (SimpleNPCDataStore does not account for mob type migrations either).
			EntityType mobType = EntityUtils.parseEntityType(mobTypeName);
			if (mobType == null) {
				Log.warning("Failed to parse Citizens NPC mob type: " + mobTypeName);
			} else {
				npc.setBukkitEntityType(mobType);
			}

			// This automatically respawns the NPC again, if it is meant to be spawned (i.e. based
			// on the loaded 'Spawned' and 'CurrentLocation' traits):
			npc.load(npcDataKey);
		}

		private Internal() {
		}
	}

	private CitizensUtils() {
	}
}
