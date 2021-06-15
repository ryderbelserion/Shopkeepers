package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.property.StringProperty;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

// TODO Use actual Axolotl type once we only support Bukkit 1.17 upwards
// TODO Editor option to play dead?
public class AxolotlShop extends BabyableShop<Animals> {

	// Use correct enum type once we only support Bukkit 1.17 upwards
	private final Property<String> variantProperty = new StringProperty(shopkeeper, "axolotlVariant", "LUCY");

	public AxolotlShop(	LivingShops livingShops, SKLivingShopObjectType<AxolotlShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		variantProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		variantProperty.save(configSection);
	}

	@Override
	protected void onSpawn(Animals entity) {
		super.onSpawn(entity);
		this.applyVariant(entity);
	}

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
	}

	// VARIANT

	public String getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(String variant) {
		Validate.notNull(variant, "variant is null");
		variantProperty.setValue(variant);
		shopkeeper.markDirty();
		this.applyVariant(this.getEntity()); // Null if not spawned
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(NMSManager.getProvider().cycleAxolotlVariant(this.getVariant(), backwards));
		// this.setVariant(EnumUtils.cycleEnumConstant(Axolotl.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant(Animals entity) {
		if (entity == null) return;
		NMSManager.getProvider().setAxolotlVariant(this.getEntity(), this.getVariant());
		// entity.setVariant(this.getVariant());
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem;
		switch (this.getVariant()) {
		case "LUCY":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.PINK));
			break;
		case "WILD":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.BROWN));
			break;
		case "GOLD":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.YELLOW));
			break;
		case "CYAN":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.WHITE));
			break;
		case "BLUE":
		default:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.BLUE));
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonAxolotlVariant, Messages.buttonAxolotlVariantLore);
		return iconItem;
	}

	private EditorHandler.Button getVariantEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getVariantEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleVariant(backwards);
				return true;
			}
		};
	}
}
