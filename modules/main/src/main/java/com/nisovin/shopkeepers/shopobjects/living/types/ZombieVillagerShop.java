package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.KeyedSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class ZombieVillagerShop extends ZombieShop<ZombieVillager> {

	public static final Property<Profession> PROFESSION = new BasicProperty<Profession>()
			.dataKeyAccessor("profession", KeyedSerializers.forRegistry(Profession.class, Registry.VILLAGER_PROFESSION))
			.defaultValue(Profession.NONE)
			.build();

	private final PropertyValue<Profession> professionProperty = new PropertyValue<>(PROFESSION)
			.onValueChanged(Unsafe.initialized(this)::applyProfession)
			.build(properties);

	public ZombieVillagerShop(
			LivingShops livingShops,
			SKLivingShopObjectType<ZombieVillagerShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		professionProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		professionProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyProfession();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getProfessionEditorButton());
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
		this.setProfession(RegistryUtils.cycleKeyed(
				Registry.VILLAGER_PROFESSION,
				this.getProfession(),
				backwards
		));
	}

	private void applyProfession() {
		ZombieVillager entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVillagerProfession(this.getProfession());
	}

	private ItemStack getProfessionEditorItem() {
		ItemStack iconItem;
		switch (this.getProfession().getKey().getKey()) {
			case "armorer":
				iconItem = new ItemStack(Material.BLAST_FURNACE);
				break;
			case "butcher":
				iconItem = new ItemStack(Material.SMOKER);
				break;
			case "cartographer":
				iconItem = new ItemStack(Material.CARTOGRAPHY_TABLE);
				break;
			case "cleric":
				iconItem = new ItemStack(Material.BREWING_STAND);
				break;
			case "farmer":
				iconItem = new ItemStack(Material.WHEAT); // Instead of COMPOSTER
				break;
			case "fisherman":
				iconItem = new ItemStack(Material.FISHING_ROD); // Instead of BARREL
				break;
			case "fletcher":
				iconItem = new ItemStack(Material.FLETCHING_TABLE);
				break;
			case "leatherworker":
				iconItem = new ItemStack(Material.LEATHER); // Instead of CAULDRON
				break;
			case "librarian":
				iconItem = new ItemStack(Material.LECTERN);
				break;
			case "mason":
				iconItem = new ItemStack(Material.STONECUTTER);
				break;
			case "shephard":
				iconItem = new ItemStack(Material.LOOM);
				break;
			case "toolsmith":
				iconItem = new ItemStack(Material.SMITHING_TABLE);
				break;
			case "weaponsmith":
				iconItem = new ItemStack(Material.GRINDSTONE);
				break;
			case "nitwit":
				iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
				ItemUtils.setLeatherColor(iconItem, Color.GREEN);
				break;
			default:
				iconItem = new ItemStack(Material.BARRIER);
				break;
		}

        ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonZombieVillagerProfession,
				Messages.buttonZombieVillagerProfessionLore
		);

		return iconItem;
	}

	private Button getProfessionEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getProfessionEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleProfession(backwards);
				return true;
			}
		};
	}
}
