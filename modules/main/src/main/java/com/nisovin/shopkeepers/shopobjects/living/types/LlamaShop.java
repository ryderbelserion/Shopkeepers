package com.nisovin.shopkeepers.shopobjects.living.types;

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
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class LlamaShop<E extends Llama> extends ChestedHorseShop<E> {

	private final Property<Llama.Color> colorProperty = new EnumProperty<>(Llama.Color.class)
			.key("color")
			.defaultValue(Llama.Color.CREAMY)
			.onValueChanged(this::applyColor)
			.build(properties);
	private final Property<DyeColor> carpetColorProperty = new EnumProperty<DyeColor>(DyeColor.class)
			.key("carpetColor")
			.nullable() // Null indicates 'no carpet'
			.defaultValue(null)
			.onValueChanged(this::applyCarpetColor)
			.build(properties);

	public LlamaShop(	LivingShops livingShops, SKLivingShopObjectType<? extends LlamaShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		colorProperty.load(shopObjectData);
		carpetColorProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		colorProperty.save(shopObjectData);
		carpetColorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyColor();
		this.applyCarpetColor();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
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
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(Llama.Color.class, this.getColor(), backwards));
	}

	private void applyColor() {
		E entity = this.getEntity();
		if (entity == null) return; // Not spawned
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
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonLlamaVariant, Messages.buttonLlamaVariantLore);
		return iconItem;
	}

	private Button getColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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
	}

	public void cycleCarpetColor(boolean backwards) {
		this.setCarpetColor(EnumUtils.cycleEnumConstantNullable(DyeColor.class, this.getCarpetColor(), backwards));
	}

	private void applyCarpetColor() {
		E entity = this.getEntity();
		if (entity == null) return; // Not spawned
		DyeColor carpetColor = this.getCarpetColor();
		entity.getInventory().setDecor(carpetColor == null ? null : new ItemStack(ItemUtils.getCarpetType(carpetColor)));
	}

	private ItemStack getCarpetColorEditorItem() {
		DyeColor carpetColor = this.getCarpetColor();
		ItemStack iconItem = new ItemStack(carpetColor == null ? Material.BARRIER : ItemUtils.getCarpetType(carpetColor));
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonLlamaCarpetColor, Messages.buttonLlamaCarpetColorLore);
		return iconItem;
	}

	private Button getCarpetColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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
