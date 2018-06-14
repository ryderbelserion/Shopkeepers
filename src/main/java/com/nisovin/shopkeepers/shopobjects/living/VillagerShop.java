package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.Log;

public class VillagerShop extends LivingEntityShop {

	private Profession profession = Profession.FARMER;

	protected VillagerShop(LivingEntityObjectType<VillagerShop> livingObjectType, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);

		// load profession:
		String professionInput;
		if (configSection.isInt("prof")) {
			// import from pre 1.10 profession ids:
			int profId = configSection.getInt("prof");
			professionInput = String.valueOf(profId);
			this.profession = getProfessionFromOldId(profId);
			shopkeeper.markDirty();
		} else {
			professionInput = configSection.getString("prof");
			this.profession = getProfession(professionInput);
		}
		// validate:
		if (!isVillagerProfession(profession)) {
			// fallback:
			Log.warning("Missing or invalid villager profession '" + professionInput
					+ "'. Using '" + Profession.FARMER + "' now.");
			this.profession = Profession.FARMER;
			shopkeeper.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("prof", profession.name());
	}

	// SUB TYPES

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		assert entity.getType() == EntityType.VILLAGER;
		((Villager) entity).setProfession(profession);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new Wool(this.getProfessionWoolColor()).toItemStack(1);
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		this.profession = this.getNextVillagerProfession();
		this.applySubType();
	}

	private DyeColor getProfessionWoolColor() {
		switch (profession) {
		case FARMER:
			return DyeColor.BROWN;
		case LIBRARIAN:
			return DyeColor.WHITE;
		case PRIEST:
			return DyeColor.MAGENTA;
		case BLACKSMITH:
			return DyeColor.GRAY;
		case BUTCHER:
			return DyeColor.SILVER;
		default:
			// TODO update this once we only support MC 1.11 upwards
			if (profession.name().equals("NITWIT")) {
				return DyeColor.GREEN;
			}
			// unknown profession:
			return DyeColor.RED;
		}
	}

	private Profession getNextVillagerProfession() {
		Profession[] professions = Profession.values();
		int id = profession.ordinal();
		while (true) {
			id += 1;
			if (id >= professions.length) {
				id = 0;
			}
			Profession nextProfession = professions[id];
			if (isVillagerProfession(nextProfession)) {
				return nextProfession;
			} else {
				continue;
			}
		}
	}

	// pre 1.10 ids:
	private static Profession getProfessionFromOldId(int oldProfessionId) {
		switch (oldProfessionId) {
		case 0:
			return Profession.FARMER;
		case 1:
			return Profession.LIBRARIAN;
		case 2:
			return Profession.PRIEST;
		case 3:
			return Profession.BLACKSMITH;
		case 4:
			return Profession.BUTCHER;
		default:
			return null;
		}
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
		if (profession.ordinal() >= Profession.FARMER.ordinal()
				&& profession.ordinal() <= Profession.BUTCHER.ordinal()) {
			return true;
		}
		// TODO: update this once we only support MC 1.11 upwards
		if (profession.name().equals("NITWIT")) {
			return true;
		}
		return false;
	}
}
