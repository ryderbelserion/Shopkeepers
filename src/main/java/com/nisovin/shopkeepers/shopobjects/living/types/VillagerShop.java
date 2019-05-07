package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.DyeColor;
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

	private static final Profession DEFAULT_PROFESSION = Profession.NONE;
	private static final Villager.Type DEFAULT_VILLAGER_TYPE = Villager.Type.PLAINS;
	private final int DEFAULT_VILLAGER_LEVEL = 1; // novice

	private Profession profession = DEFAULT_PROFESSION;
	private Villager.Type villagerType = DEFAULT_VILLAGER_TYPE;
	private int villagerLevel = DEFAULT_VILLAGER_LEVEL;

	public VillagerShop(LivingShops livingShops, SKLivingShopObjectType<VillagerShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.loadProfession(configSection);
		this.loadVillagerType(configSection);
		this.loadVillagerLevel(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		this.saveProfession(configSection);
		this.saveVillagerType(configSection);
		this.saveVillagerLevel(configSection);
	}

	@Override
	protected void onSpawn(Villager entity) {
		super.onSpawn(entity);
		this.applyProfession(entity);
		this.applyVillagerType(entity);
		this.applyVillagerLevel(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getProfessionEditorButton());
		editorButtons.add(this.getVillagerTypeEditorButton());
		editorButtons.add(this.getVillagerLevelEditorButton());
		return editorButtons;
	}

	// PROFESSION

	private void loadProfession(ConfigurationSection configSection) {
		String professionName = configSection.getString("profession");
		if (professionName == null) {
			// migration from 'prof' key:
			// TODO added with 1.14 update, remove again at some point
			professionName = configSection.getString("prof");
			shopkeeper.markDirty();
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
		Profession profession = Utils.parseEnumValue(Profession.class, professionName);
		if (profession == null) {
			// fallback:
			Log.warning("Missing or invalid villager profession '" + professionName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + DEFAULT_PROFESSION + "' now.");
			profession = DEFAULT_PROFESSION;
			shopkeeper.markDirty();
		}
		this.profession = profession;
	}

	private void saveProfession(ConfigurationSection configSection) {
		configSection.set("profession", profession.name());
	}

	public void setProfession(Profession profession) {
		Validate.notNull(profession, "Profession is null!");
		this.profession = profession;
		shopkeeper.markDirty();
		this.applyProfession(this.getEntity()); // null if not active
	}

	private void applyProfession(Villager entity) {
		if (entity == null) return;
		entity.setProfession(profession);
	}

	public void cycleProfession() {
		this.setProfession(Utils.getNextEnumConstant(Profession.class, profession));
	}

	private ItemStack getProfessionEditorItem() {
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

	private EditorHandler.Button getProfessionEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getProfessionEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleProfession();
				return true;
			}
		};
	}

	// VILLAGER TYPE

	private void loadVillagerType(ConfigurationSection configSection) {
		String typeName = configSection.getString("villagerType");
		Villager.Type villagerType = Utils.parseEnumValue(Villager.Type.class, typeName);
		if (villagerType == null) {
			// fallback:
			Log.warning("Missing or invalid villager type '" + typeName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + DEFAULT_VILLAGER_TYPE + "' now.");
			villagerType = DEFAULT_VILLAGER_TYPE;
			shopkeeper.markDirty();
		}
		this.villagerType = villagerType;
	}

	private void saveVillagerType(ConfigurationSection configSection) {
		configSection.set("villagerType", villagerType.name());
	}

	public void setVillagerType(Villager.Type villagerType) {
		Validate.notNull(villagerType, "Villager type is null!");
		this.villagerType = villagerType;
		shopkeeper.markDirty();
		this.applyVillagerType(this.getEntity()); // null if not active
	}

	private void applyVillagerType(Villager entity) {
		if (entity == null) return;
		entity.setVillagerType(villagerType);
	}

	public void cycleVillagerType() {
		this.setVillagerType(Utils.getNextEnumConstant(Villager.Type.class, villagerType));
	}

	private ItemStack getVillagerTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (villagerType) {
		default:
		case PLAINS:
			// default brown color
			break;
		case DESERT:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		case JUNGLE:
			ItemUtils.setLeatherColor(iconItem, Color.YELLOW.mixColors(Color.ORANGE));
			break;
		case SAVANNA:
			ItemUtils.setLeatherColor(iconItem, Color.RED);
			break;
		case SNOW:
			ItemUtils.setLeatherColor(iconItem, DyeColor.CYAN.getColor());
			break;
		case SWAMP:
			ItemUtils.setLeatherColor(iconItem, DyeColor.PURPLE.getColor());
			break;
		case TAIGA:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.BROWN));
			break;
		}
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getVillagerTypeEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getVillagerTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleVillagerType();
				return true;
			}
		};
	}

	// VILLAGER LEVEL

	private void loadVillagerLevel(ConfigurationSection configSection) {
		Integer villagerLevel;
		if (!configSection.isInt("villagerLevel")) {
			villagerLevel = null;
			villagerLevel = DEFAULT_VILLAGER_LEVEL;
		} else {
			villagerLevel = configSection.getInt("villagerLevel");
		}
		if (villagerLevel == null || villagerLevel < 1 || villagerLevel > 5) {
			// fallback:
			Log.warning("Missing or invalid villager level '" + villagerLevel + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + DEFAULT_VILLAGER_LEVEL + "' now.");
			villagerLevel = DEFAULT_VILLAGER_LEVEL;
			shopkeeper.markDirty();
		}
		this.villagerLevel = villagerLevel;
	}

	private void saveVillagerLevel(ConfigurationSection configSection) {
		configSection.set("villagerLevel", villagerLevel);
	}

	public void setVillagerLevel(int villagerLevel) {
		Validate.isTrue(villagerLevel >= 1 && villagerLevel <= 5, "Invalid villager level: " + villagerLevel);
		this.villagerLevel = villagerLevel;
		shopkeeper.markDirty();
		this.applyVillagerLevel(this.getEntity()); // null if not active
	}

	private void applyVillagerLevel(Villager entity) {
		if (entity == null) return;
		entity.setVillagerLevel(villagerLevel);
	}

	public void cycleVillagerLevel() {
		this.setVillagerLevel(villagerLevel >= 5 ? 1 : villagerLevel + 1);
	}

	private ItemStack getVillagerLevelEditorItem() {
		ItemStack iconItem;
		switch (villagerLevel) {
		default:
		case 1:
			iconItem = new ItemStack(Material.COBBLESTONE);
			break;
		case 2:
			iconItem = new ItemStack(Material.IRON_INGOT);
			break;
		case 3:
			iconItem = new ItemStack(Material.GOLD_INGOT);
			break;
		case 4:
			iconItem = new ItemStack(Material.EMERALD);
			break;
		case 5:
			iconItem = new ItemStack(Material.DIAMOND);
			break;
		}
		assert iconItem != null;
		// TODO use more specific text
		// ItemUtils.setLocalizedName(iconItem, "entity.minecraft.villager." +
		// profession.name().toLowerCase(Locale.ROOT));
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getVillagerLevelEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getVillagerLevelEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleVillagerLevel();
				return true;
			}
		};
	}
}
