package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.IntegerValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class VillagerShop extends BabyableShop<Villager> {

	private static final String DATA_KEY_PROFESSION = "profession";
	public static final Property<Profession> PROFESSION = new BasicProperty<Profession>()
			.dataKeyAccessor(DATA_KEY_PROFESSION, EnumSerializers.lenient(Profession.class))
			.defaultValue(Profession.NONE)
			.build();

	public static final Property<Villager.Type> VILLAGER_TYPE = new BasicProperty<Villager.Type>()
			.dataKeyAccessor("villagerType", EnumSerializers.lenient(Villager.Type.class))
			.defaultValue(Villager.Type.PLAINS)
			.build();

	public static final int MIN_VILLAGER_LEVEL = 1;
	public static final int MAX_VILLAGER_LEVEL = 5;
	public static final Property<Integer> VILLAGER_LEVEL = new BasicProperty<Integer>()
			.dataKeyAccessor("villagerLevel", NumberSerializers.INTEGER)
			.validator(IntegerValidators.bounded(MIN_VILLAGER_LEVEL, MAX_VILLAGER_LEVEL))
			.defaultValue(1)
			.build();

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration("villager-profession",
				MigrationPhase.ofShopObjectClass(VillagerShop.class)) {
			@Override
			public boolean migrate(ShopkeeperData shopkeeperData, String logPrefix) throws InvalidDataException {
				ShopObjectData shopObjectData = shopkeeperData.get(AbstractShopkeeper.SHOP_OBJECT_DATA);
				boolean migrated = false;
				// Migration from 'prof' key: TODO Added with 1.14 update, remove again at some point.
				String professionName = shopObjectData.getString("prof");
				if (professionName != null) {
					Log.warning(logPrefix + "Migrated villager profession from key 'prof' to key 'profession'.");
					shopObjectData.remove("prof");
					shopObjectData.set(DATA_KEY_PROFESSION, professionName);
					migrated = true;
				}

				// MC 1.14 migration:
				professionName = shopObjectData.getString(DATA_KEY_PROFESSION);
				if (professionName != null) {
					Profession newProfession = null;
					if (professionName.equals("PRIEST")) {
						newProfession = Profession.CLERIC;
					} else if (professionName.equals("BLACKSMITH")) {
						newProfession = Profession.ARMORER;
					}
					if (newProfession != null) {
						Log.warning(logPrefix + "Migrated villager profession from '"
								+ professionName + "' to '" + newProfession.name() + "'.");
						shopObjectData.set(PROFESSION, newProfession);
						migrated = true;
					}
				}
				return migrated;
			}
		});
	}

	private final PropertyValue<Profession> professionProperty = new PropertyValue<>(PROFESSION)
			.onValueChanged(this::applyProfession)
			.build(properties);
	private final PropertyValue<Villager.Type> villagerTypeProperty = new PropertyValue<>(VILLAGER_TYPE)
			.onValueChanged(this::applyVillagerType)
			.build(properties);
	private final PropertyValue<Integer> villagerLevelProperty = new PropertyValue<>(VILLAGER_LEVEL)
			.onValueChanged(this::applyVillagerLevel)
			.build(properties);

	public VillagerShop(LivingShops livingShops, SKLivingShopObjectType<VillagerShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		professionProperty.load(shopObjectData);
		villagerTypeProperty.load(shopObjectData);
		villagerLevelProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		professionProperty.save(shopObjectData);
		villagerTypeProperty.save(shopObjectData);
		villagerLevelProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		// TODO I wasn't able to reproduce this myself yet, but according to some reports villager shopkeepers would
		// sometimes lose their profession. Setting their experience to something above 0 is an attempt to resolve this.
		// Related (but shouldn't apply here since we use NoAI mobs): https://hub.spigotmc.org/jira/browse/SPIGOT-4776
		Villager entity = this.getEntity();
		entity.setVillagerExperience(1);
		this.applyProfession();
		this.applyVillagerType();
		this.applyVillagerLevel();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getProfessionEditorButton());
		editorButtons.add(this.getVillagerTypeEditorButton());
		editorButtons.add(this.getVillagerLevelEditorButton());
		return editorButtons;
	}

	// PROFESSION

	public Profession getProfession() {
		return professionProperty.getValue();
	}

	public void setProfession(Profession profession) {
		professionProperty.setValue(profession);
	}

	public void cycleProfession(boolean backwards) {
		this.setProfession(EnumUtils.cycleEnumConstant(Profession.class, this.getProfession(), backwards));
	}

	private void applyProfession() {
		Villager entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setProfession(this.getProfession());
	}

	private ItemStack getProfessionEditorItem() {
		ItemStack iconItem;
		switch (this.getProfession()) {
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
			iconItem = new ItemStack(Material.WHEAT); // Instead of COMPOSTER
			break;
		case FISHERMAN:
			iconItem = new ItemStack(Material.FISHING_ROD); // Instead of BARREL
			break;
		case FLETCHER:
			iconItem = new ItemStack(Material.FLETCHING_TABLE);
			break;
		case LEATHERWORKER:
			iconItem = new ItemStack(Material.LEATHER); // Instead of CAULDRON
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
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonVillagerProfession, Messages.buttonVillagerProfessionLore);
		return iconItem;
	}

	private Button getProfessionEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getProfessionEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleProfession(backwards);
				return true;
			}
		};
	}

	// VILLAGER TYPE

	public Villager.Type getVillagerType() {
		return villagerTypeProperty.getValue();
	}

	public void setVillagerType(Villager.Type villagerType) {
		villagerTypeProperty.setValue(villagerType);
	}

	public void cycleVillagerType(boolean backwards) {
		this.setVillagerType(EnumUtils.cycleEnumConstant(Villager.Type.class, this.getVillagerType(), backwards));
	}

	private void applyVillagerType() {
		Villager entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setVillagerType(this.getVillagerType());
	}

	private ItemStack getVillagerTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getVillagerType()) {
		default:
		case PLAINS:
			// Default brown color:
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
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonVillagerVariant, Messages.buttonVillagerVariantLore);
		return iconItem;
	}

	private Button getVillagerTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getVillagerTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleVillagerType(backwards);
				return true;
			}
		};
	}

	// VILLAGER LEVEL

	public int getVillagerLevel() {
		return villagerLevelProperty.getValue();
	}

	public void setVillagerLevel(int villagerLevel) {
		villagerLevelProperty.setValue(villagerLevel);
	}

	public void cycleVillagerLevel(boolean backwards) {
		int villagerLevel = this.getVillagerLevel();
		int nextLevel;
		if (backwards) {
			nextLevel = villagerLevel - 1;
		} else {
			nextLevel = villagerLevel + 1;
		}
		nextLevel = MathUtils.rangeModulo(nextLevel, MIN_VILLAGER_LEVEL, MAX_VILLAGER_LEVEL);
		this.setVillagerLevel(nextLevel);
	}

	private void applyVillagerLevel() {
		Villager entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setVillagerLevel(this.getVillagerLevel());
	}

	private ItemStack getVillagerLevelEditorItem() {
		ItemStack iconItem;
		switch (this.getVillagerLevel()) {
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
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonVillagerLevel, Messages.buttonVillagerLevelLore);
		return iconItem;
	}

	private Button getVillagerLevelEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getVillagerLevelEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleVillagerLevel(backwards);
				return true;
			}
		};
	}
}
