package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.Locale;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class VillagerShop extends SKLivingShopObject {

	private Profession profession = Profession.NONE;

	public VillagerShop(LivingShops livingShops, SKLivingShopObjectType<VillagerShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);

		// profession:
		String professionName = configSection.getString("prof");
		// pre MC 1.14 migration:
		if (professionName != null) {
			String newProfessionName = null;
			if (professionName.equals("PRIEST")) {
				newProfessionName = Profession.CLERIC.name();
			} else if (professionName.equals("BLACKSMITH")) {
				newProfessionName = Profession.ARMORER.name();
			}
			if (newProfessionName != null) {
				Log.warning("Migrated villager shopkeeper '" + shopkeeper.getId() + "' of type '" + professionName
						+ "' to type '" + newProfessionName + "'.");
				professionName = newProfessionName;
				shopkeeper.markDirty();
			}
		}
		Profession profession = getProfession(professionName);
		if (profession == null) {
			// fallback:
			Log.warning("Missing or invalid villager profession '" + professionName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + Profession.NONE + "' now.");
			profession = Profession.NONE;
			shopkeeper.markDirty();
		}
		this.profession = profession;
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("prof", profession.name());
	}

	@Override
	public Villager getEntity() {
		assert super.getEntity().getType() == EntityType.VILLAGER;
		return (Villager) super.getEntity();
	}

	// SUB TYPES

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		this.getEntity().setProfession(profession);
	}

	@Override
	public ItemStack getSubTypeItem() {
		ItemStack item;
		switch (profession) {
		case ARMORER:
			item = new ItemStack(Material.BLAST_FURNACE);
			break;
		case BUTCHER:
			item = new ItemStack(Material.SMOKER);
			break;
		case CARTOGRAPHER:
			item = new ItemStack(Material.CARTOGRAPHY_TABLE);
			break;
		case CLERIC:
			item = new ItemStack(Material.BREWING_STAND);
			break;
		case FARMER:
			item = new ItemStack(Material.WHEAT); // instead of COMPOSTER
			break;
		case FISHERMAN:
			item = new ItemStack(Material.FISHING_ROD); // instead of BARREL
			break;
		case FLETCHER:
			item = new ItemStack(Material.FLETCHING_TABLE);
			break;
		case LEATHERWORKER:
			item = new ItemStack(Material.LEATHER); // instead of CAULDRON
			break;
		case LIBRARIAN:
			item = new ItemStack(Material.LECTERN);
			break;
		case MASON:
			item = new ItemStack(Material.STONECUTTER);
			break;
		case SHEPHERD:
			item = new ItemStack(Material.LOOM);
			break;
		case TOOLSMITH:
			item = new ItemStack(Material.SMITHING_TABLE);
			break;
		case WEAPONSMITH:
			item = new ItemStack(Material.GRINDSTONE);
			break;
		case NITWIT:
			item = new ItemStack(Material.LEATHER_CHESTPLATE);
			ItemUtils.setLeatherColor(item, Color.GREEN);
			break;
		case NONE:
		default:
			item = new ItemStack(Material.BARRIER);
			break;
		}
		assert item != null;
		ItemUtils.setLocalizedName(item, "entity.minecraft.villager." + profession.name().toLowerCase(Locale.ROOT));
		return item;
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		profession = Utils.getNextEnumConstant(Profession.class, profession);
		assert profession != null;
		this.applySubType();
	}

	private static Profession getProfession(String professionName) {
		if (professionName != null) {
			try {
				return Profession.valueOf(professionName);
			} catch (IllegalArgumentException e) {
			}
		}
		return null;
	}
}
