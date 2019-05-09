package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

public class LlamaShop<E extends Llama> extends ChestedHorseShop<E> {

	private static final Property<Llama.Color> PROPERTY_COLOR = new EnumProperty<>(Llama.Color.class, "color", Llama.Color.CREAMY);
	private static final Property<DyeColor> PROPERTY_CARPET_COLOR = new EnumProperty<DyeColor>(DyeColor.class, "carpetColor", null) {
		@Override
		public boolean isNullable() {
			// null to indicate 'no carpet'
			return true;
		}
	};

	private Llama.Color color = PROPERTY_COLOR.getDefaultValue();
	private DyeColor carpetColor = PROPERTY_CARPET_COLOR.getDefaultValue();

	public LlamaShop(	LivingShops livingShops, SKLivingShopObjectType<? extends LlamaShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.color = PROPERTY_COLOR.load(shopkeeper, configSection);
		this.carpetColor = PROPERTY_CARPET_COLOR.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_COLOR.save(shopkeeper, configSection, color);
		PROPERTY_CARPET_COLOR.save(shopkeeper, configSection, carpetColor);
	}

	@Override
	protected void onSpawn(E entity) {
		super.onSpawn(entity);
		this.applyColor(entity);
		this.applyCarpetColor(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getCarpetColorEditorButton());
		return editorButtons;
	}

	// COLOR

	public void setColor(Llama.Color color) {
		this.color = color;
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // null if not active
	}

	private void applyColor(E entity) {
		if (entity == null) return;
		entity.setColor(color);
	}

	public void cycleColor() {
		this.setColor(Utils.cycleEnumConstant(Llama.Color.class, color));
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (color) {
		case BROWN:
			ItemUtils.setLeatherColor(iconItem, DyeColor.BROWN.getColor());
			break;
		case GRAY:
			ItemUtils.setLeatherColor(iconItem, Color.SILVER);
			break;
		case WHITE:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case CREAMY:
		default:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.ORANGE));
			break;
		}
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getColorEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleColor();
				return true;
			}
		};
	}

	// CARPET COLOR

	public void setCarpetColor(DyeColor carpetColor) {
		this.carpetColor = carpetColor;
		shopkeeper.markDirty();
		this.applyCarpetColor(this.getEntity()); // null if not active
	}

	private void applyCarpetColor(E entity) {
		if (entity == null) return;
		entity.getInventory().setDecor(carpetColor == null ? null : new ItemStack(ItemUtils.getCarpetType(carpetColor)));
	}

	public void cycleCarpetColor() {
		this.setCarpetColor(Utils.cycleEnumConstantNullable(DyeColor.class, carpetColor));
	}

	private ItemStack getCarpetColorEditorItem() {
		ItemStack iconItem = new ItemStack(carpetColor == null ? Material.BARRIER : ItemUtils.getCarpetType(carpetColor));
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getCarpetColorEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCarpetColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCarpetColor();
				return true;
			}
		};
	}
}
