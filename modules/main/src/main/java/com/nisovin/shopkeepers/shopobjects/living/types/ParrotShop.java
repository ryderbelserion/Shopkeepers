package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ParrotShop extends SittableShop<Parrot> {

	private final Property<Parrot.Variant> variantProperty = new EnumProperty<>(shopkeeper, Parrot.Variant.class, "parrotVariant", Parrot.Variant.RED);

	public ParrotShop(	LivingShops livingShops, SKLivingShopObjectType<ParrotShop> livingObjectType,
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
	protected void onSpawn(Parrot entity) {
		super.onSpawn(entity);
		this.applyVariant(entity);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
	}

	// VARIANT

	public Parrot.Variant getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(Parrot.Variant variant) {
		Validate.notNull(variant, "variant is null");
		variantProperty.setValue(variant);
		shopkeeper.markDirty();
		this.applyVariant(this.getEntity()); // Null if not spawned
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(EnumUtils.cycleEnumConstant(Parrot.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant(Parrot entity) {
		if (entity == null) return;
		entity.setVariant(this.getVariant());
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem;
		switch (this.getVariant()) {
		case BLUE:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.BLUE));
			break;
		case CYAN:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.LIGHT_BLUE));
			break;
		case GRAY:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.LIGHT_GRAY));
			break;
		case GREEN:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.LIME));
			break;
		case RED:
		default:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.RED));
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonParrotVariant, Messages.buttonParrotVariantLore);
		return iconItem;
	}

	private Button getVariantEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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
