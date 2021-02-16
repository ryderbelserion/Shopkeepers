package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
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
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;

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

	private final Property<Horse.Color> colorProperty = new EnumProperty<>(shopkeeper, Horse.Color.class, "color", Horse.Color.BROWN);
	private final Property<Horse.Style> styleProperty = new EnumProperty<>(shopkeeper, Horse.Style.class, "style", Horse.Style.NONE);
	private final Property<HorseArmor> armorProperty = new EnumProperty<HorseArmor>(shopkeeper, HorseArmor.class, "armor", null) {
		@Override
		public boolean isNullable() {
			return true; // Null indicates 'no armor'
		}
	};

	public HorseShop(	LivingShops livingShops, SKLivingShopObjectType<HorseShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		colorProperty.load(configSection);
		styleProperty.load(configSection);
		armorProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		colorProperty.save(configSection);
		styleProperty.load(configSection);
		armorProperty.save(configSection);
	}

	@Override
	protected void onSpawn(Horse entity) {
		super.onSpawn(entity);
		this.applyColor(entity);
		this.applyStyle(entity);
		this.applyArmor(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
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
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // Null if not spawned
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(Horse.Color.class, this.getColor(), backwards));
	}

	private void applyColor(Horse entity) {
		if (entity == null) return;
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
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonHorseColor, Messages.buttonHorseColorLore);
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

	// STYLE

	public Horse.Style getStyle() {
		return styleProperty.getValue();
	}

	public void setStyle(Horse.Style style) {
		styleProperty.setValue(style);
		shopkeeper.markDirty();
		this.applyStyle(this.getEntity()); // Null if not spawned
	}

	public void cycleStyle(boolean backwards) {
		this.setStyle(EnumUtils.cycleEnumConstant(Horse.Style.class, this.getStyle(), backwards));
	}

	private void applyStyle(Horse entity) {
		if (entity == null) return;
		entity.setStyle(this.getStyle());
	}

	private ItemStack getStyleEditorItem() {
		ItemStack iconItem = new ItemStack(Material.WHITE_BANNER);
		BannerMeta meta = (BannerMeta) iconItem.getItemMeta();
		meta.addPattern(new Pattern(DyeColor.BROWN, PatternType.CURLY_BORDER));
		meta.addPattern(new Pattern(DyeColor.BROWN, PatternType.TRIANGLES_BOTTOM));
		meta.addPattern(new Pattern(DyeColor.BROWN, PatternType.TRIANGLES_TOP));
		iconItem.setItemMeta(meta);
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonHorseStyle, Messages.buttonHorseStyleLore);
		return iconItem;
	}

	private EditorHandler.Button getStyleEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
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
		shopkeeper.markDirty();
		this.applyArmor(this.getEntity()); // Null if not spawned
	}

	public void cycleArmor(boolean backwards) {
		this.setArmor(EnumUtils.cycleEnumConstantNullable(HorseArmor.class, this.getArmor(), backwards));
	}

	private void applyArmor(Horse entity) {
		if (entity == null) return;
		HorseArmor armor = this.getArmor();
		entity.getInventory().setArmor(armor == null ? null : new ItemStack(armor.getMaterial()));
	}

	private ItemStack getArmorEditorItem() {
		HorseArmor armor = this.getArmor();
		ItemStack iconItem = new ItemStack(armor == null ? Material.BARRIER : armor.getMaterial());
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonHorseArmor, Messages.buttonHorseArmorLore);
		return iconItem;
	}

	private EditorHandler.Button getArmorEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
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
