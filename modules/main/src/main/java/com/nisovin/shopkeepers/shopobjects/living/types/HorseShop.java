package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

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

public class HorseShop extends BabyableShop<Horse> {

	public static enum HorseArmor {

		LEATHER(Material.LEATHER_HORSE_ARMOR),
		IRON(Material.IRON_HORSE_ARMOR),
		GOLD(Material.GOLDEN_HORSE_ARMOR),
		DIAMOND(Material.DIAMOND_HORSE_ARMOR);

		private final Material material;

		private HorseArmor(Material material) {
			assert material != null;
			this.material = material;
		}

		public Material getMaterial() {
			return material;
		}
	}

	private final Property<Horse.Color> colorProperty = new EnumProperty<>(Horse.Color.class)
			.key("color")
			.defaultValue(Horse.Color.BROWN)
			.onValueChanged(this::applyColor)
			.build(properties);
	private final Property<Horse.Style> styleProperty = new EnumProperty<>(Horse.Style.class)
			.key("style")
			.defaultValue(Horse.Style.NONE)
			.onValueChanged(this::applyStyle)
			.build(properties);
	private final Property<HorseArmor> armorProperty = new EnumProperty<HorseArmor>(HorseArmor.class)
			.key("armor")
			.nullable() // Null indicates 'no armor'
			.defaultValue(null)
			.onValueChanged(this::applyArmor)
			.build(properties);

	public HorseShop(	LivingShops livingShops, SKLivingShopObjectType<HorseShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		colorProperty.load(shopObjectData);
		styleProperty.load(shopObjectData);
		armorProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		colorProperty.save(shopObjectData);
		styleProperty.save(shopObjectData);
		armorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyColor();
		this.applyStyle();
		this.applyArmor();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getStyleEditorButton());
		editorButtons.add(this.getArmorEditorButton());
		return editorButtons;
	}

	// COLOR

	public Horse.Color getColor() {
		return colorProperty.getValue();
	}

	public void setColor(Horse.Color color) {
		colorProperty.setValue(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(Horse.Color.class, this.getColor(), backwards));
	}

	private void applyColor() {
		Horse entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getColor()) {
		case BLACK:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(31, 31, 31));
			break;
		case BROWN:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(54, 25, 8));
			break;
		case CHESTNUT:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(110, 59, 38));
			break;
		case CREAMY:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(98, 65, 28));
			break;
		case DARK_BROWN:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(39, 21, 13));
			break;
		case GRAY:
			ItemUtils.setLeatherColor(iconItem, Color.SILVER);
			break;
		case WHITE:
		default:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonHorseColor, Messages.buttonHorseColorLore);
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

	// STYLE

	public Horse.Style getStyle() {
		return styleProperty.getValue();
	}

	public void setStyle(Horse.Style style) {
		styleProperty.setValue(style);
	}

	public void cycleStyle(boolean backwards) {
		this.setStyle(EnumUtils.cycleEnumConstant(Horse.Style.class, this.getStyle(), backwards));
	}

	private void applyStyle() {
		Horse entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setStyle(this.getStyle());
	}

	private ItemStack getStyleEditorItem() {
		ItemStack iconItem = new ItemStack(Material.WHITE_BANNER);
		BannerMeta meta = (BannerMeta) iconItem.getItemMeta();
		meta.addPattern(new Pattern(DyeColor.BROWN, PatternType.CURLY_BORDER));
		meta.addPattern(new Pattern(DyeColor.BROWN, PatternType.TRIANGLES_BOTTOM));
		meta.addPattern(new Pattern(DyeColor.BROWN, PatternType.TRIANGLES_TOP));
		iconItem.setItemMeta(meta);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonHorseStyle, Messages.buttonHorseStyleLore);
		return iconItem;
	}

	private Button getStyleEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getStyleEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleStyle(backwards);
				return true;
			}
		};
	}

	// ARMOR

	public HorseArmor getArmor() {
		return armorProperty.getValue();
	}

	public void setArmor(HorseArmor armor) {
		armorProperty.setValue(armor);
	}

	public void cycleArmor(boolean backwards) {
		this.setArmor(EnumUtils.cycleEnumConstantNullable(HorseArmor.class, this.getArmor(), backwards));
	}

	private void applyArmor() {
		Horse entity = this.getEntity();
		if (entity == null) return; // Not spawned
		HorseArmor armor = this.getArmor();
		entity.getInventory().setArmor(armor == null ? null : new ItemStack(armor.getMaterial()));
	}

	private ItemStack getArmorEditorItem() {
		HorseArmor armor = this.getArmor();
		ItemStack iconItem = new ItemStack(armor == null ? Material.BARRIER : armor.getMaterial());
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonHorseArmor, Messages.buttonHorseArmorLore);
		return iconItem;
	}

	private Button getArmorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getArmorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleArmor(backwards);
				return true;
			}
		};
	}
}
