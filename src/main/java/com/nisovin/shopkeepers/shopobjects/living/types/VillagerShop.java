package com.nisovin.shopkeepers.shopobjects.living.types;

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
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class VillagerShop extends SKLivingShopObject {

	private Profession profession = Profession.FARMER;

	public VillagerShop(LivingShops livingShops, SKLivingShopObjectType<VillagerShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);

		// load profession:
		String professionName = configSection.getString("prof");
		Profession profession = getProfession(professionName);
		// validate:
		if (!isVillagerProfession(profession)) {
			// fallback:
			Log.warning("Missing or invalid villager profession '" + professionName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + Profession.FARMER + "' now.");
			profession = Profession.FARMER;
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
		switch (profession) {
		case FARMER:
			return new ItemStack(Material.BROWN_WOOL, 1);
		case LIBRARIAN:
			return new ItemStack(Material.WHITE_WOOL, 1);
		case PRIEST:
			return new ItemStack(Material.MAGENTA_WOOL, 1);
		case BLACKSMITH:
			return new ItemStack(Material.GRAY_WOOL, 1);
		case BUTCHER:
			return new ItemStack(Material.LIGHT_GRAY_WOOL, 1);
		case NITWIT:
			return new ItemStack(Material.GREEN_WOOL, 1);
		default:
			// unknown profession:
			return new ItemStack(Material.RED_WOOL, 1);
		}
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		profession = Utils.getNextEnumConstant(Profession.class, profession, (profession) -> {
			return isVillagerProfession(profession);
		});
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

	private static boolean isVillagerProfession(Profession profession) {
		if (profession == null) return false;
		// TODO update this once all legacy zombie profession got removed
		return (profession.ordinal() >= Profession.FARMER.ordinal()
				&& profession.ordinal() <= Profession.NITWIT.ordinal());
	}
}
