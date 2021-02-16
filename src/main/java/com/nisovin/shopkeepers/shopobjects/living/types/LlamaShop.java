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

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;

public class LlamaShop<E extends Llama> extends ChestedHorseShop<E> {

	private final Property<Llama.Color> colorProperty = new EnumProperty<>(shopkeeper, Llama.Color.class, "color", Llama.Color.CREAMY);
	private final Property<DyeColor> carpetColorProperty = new EnumProperty<DyeColor>(shopkeeper, DyeColor.class, "carpetColor", null) {
		@Override
		public boolean isNullable() {
			return true; // Null indicates 'no carpet'
		}
	};

	public LlamaShop(	LivingShops livingShops, SKLivingShopObjectType<? extends LlamaShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		colorProperty.load(configSection);
		carpetColorProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		colorProperty.save(configSection);
		carpetColorProperty.save(configSection);
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

	public Llama.Color getColor() {
		return colorProperty.getValue();
	}

	public void setColor(Llama.Color color) {
		colorProperty.setValue(color);
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // Null if not spawned
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(Llama.Color.class, this.getColor(), backwards));
	}

	private void applyColor(E entity) {
		if (entity == null) return;
		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getColor()) {
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
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonLlamaVariant, Messages.buttonLlamaVariantLore);
		return iconItem;
	}

	private EditorHandler.Button getColorEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleColor(backwards);
				return true;
			}
		};
	}

	// CARPET COLOR

	public DyeColor getCarpetColor() {
		return carpetColorProperty.getValue();
	}

	public void setCarpetColor(DyeColor carpetColor) {
		carpetColorProperty.setValue(carpetColor);
		shopkeeper.markDirty();
		this.applyCarpetColor(this.getEntity()); // Null if not spawned
	}

	public void cycleCarpetColor(boolean backwards) {
		this.setCarpetColor(EnumUtils.cycleEnumConstantNullable(DyeColor.class, this.getCarpetColor(), backwards));
	}

	private void applyCarpetColor(E entity) {
		if (entity == null) return;
		DyeColor carpetColor = this.getCarpetColor();
		entity.getInventory().setDecor(carpetColor == null ? null : new ItemStack(ItemUtils.getCarpetType(carpetColor)));
	}

	private ItemStack getCarpetColorEditorItem() {
		DyeColor carpetColor = this.getCarpetColor();
		ItemStack iconItem = new ItemStack(carpetColor == null ? Material.BARRIER : ItemUtils.getCarpetType(carpetColor));
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonLlamaCarpetColor, Messages.buttonLlamaCarpetColorLore);
		return iconItem;
	}

	private EditorHandler.Button getCarpetColorEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCarpetColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleCarpetColor(backwards);
				return true;
			}
		};
	}
}
