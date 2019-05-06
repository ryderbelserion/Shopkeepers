package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class VillagerShop extends BabyableShop<Villager> {

	private Profession profession = Profession.NONE;

	public VillagerShop(LivingShops livingShops, SKLivingShopObjectType<VillagerShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);

		// profession:
		String professionName = configSection.getString("profession");
		if (professionName == null) {
			// migration from 'prof' key:
			// TODO added with 1.14 update, remove again at some point
			professionName = configSection.getString("prof");
		}
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

	private static Profession getProfession(String professionName) {
		if (professionName == null) return null;
		try {
			return Profession.valueOf(professionName);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("profession", profession.name());
	}

	@Override
	protected void onSpawn(Villager entity) {
		super.onSpawn(entity);
		this.applyProfession(entity);
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.getEditorButtons(); // assumes modifiable
		editorButtons.add(new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getProfessionEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleProfession();
				return true;
			}
		});
		return editorButtons;
	}

	// PROFESSION

	public void setProfession(Profession profession) {
		Validate.notNull(profession, "Profession is null!");
		this.profession = profession;
		shopkeeper.markDirty();
		this.applyProfession(this.getEntity()); // null if not active
	}

	protected void applyProfession(Villager entity) {
		if (entity == null) return;
		entity.setProfession(profession);
	}

	public void cycleProfession() {
		this.setProfession(Utils.getNextEnumConstant(Profession.class, profession));
	}

	protected ItemStack getProfessionEditorItem() {
		ItemStack iconItem;
		switch (profession) {
		case ARMORER:
			iconItem = new ItemStack(Material.BLAST_FURNACE);
			break;
		case BUTCHER:
			iconItem = new ItemStack(Material.SMOKER);
			break;
		case CARTOGRAPHER:
			iconItem = new ItemStack(Material.CARTOGRAPHY_TABLE);
			break;
		case CLERIC:
			iconItem = new ItemStack(Material.BREWING_STAND);
			break;
		case FARMER:
			iconItem = new ItemStack(Material.WHEAT); // instead of COMPOSTER
			break;
		case FISHERMAN:
			iconItem = new ItemStack(Material.FISHING_ROD); // instead of BARREL
			break;
		case FLETCHER:
			iconItem = new ItemStack(Material.FLETCHING_TABLE);
			break;
		case LEATHERWORKER:
			iconItem = new ItemStack(Material.LEATHER); // instead of CAULDRON
			break;
		case LIBRARIAN:
			iconItem = new ItemStack(Material.LECTERN);
			break;
		case MASON:
			iconItem = new ItemStack(Material.STONECUTTER);
			break;
		case SHEPHERD:
			iconItem = new ItemStack(Material.LOOM);
			break;
		case TOOLSMITH:
			iconItem = new ItemStack(Material.SMITHING_TABLE);
			break;
		case WEAPONSMITH:
			iconItem = new ItemStack(Material.GRINDSTONE);
			break;
		case NITWIT:
			iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
			ItemUtils.setLeatherColor(iconItem, Color.GREEN);
			break;
		case NONE:
		default:
			iconItem = new ItemStack(Material.BARRIER);
			break;
		}
		assert iconItem != null;
		// TODO use more specific text
		// ItemUtils.setLocalizedName(iconItem, "entity.minecraft.villager." +
		// profession.name().toLowerCase(Locale.ROOT));
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}
}
