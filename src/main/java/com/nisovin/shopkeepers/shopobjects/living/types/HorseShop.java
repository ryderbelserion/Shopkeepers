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

	private static final Property<Horse.Color> PROPERTY_COLOR = new EnumProperty<>(Horse.Color.class, "color", Horse.Color.BROWN);
	private static final Property<Horse.Style> PROPERTY_STYLE = new EnumProperty<>(Horse.Style.class, "style", Horse.Style.NONE);
	private static final Property<HorseArmor> PROPERTY_ARMOR = new EnumProperty<HorseArmor>(HorseArmor.class, "armor", null) {
		@Override
		public boolean isNullable() {
			return true; // Null indicates 'no armor'
		}
	};

	public static enum HorseArmor {
		LEATHER(Material.LEATHER_HORSE_ARMOR),
		IRON(Material.IRON_HORSE_ARMOR),
		GOLD(Material.GOLDEN_HORSE_ARMOR),
		DIAMOND(Material.DIAMOND_HORSE_ARMOR);

		private final Material material;

		private HorseArmor(Material material) {
			this.material = material;
		}

		public Material getMaterial() {
			return material;
		}
	}

	private Horse.Color color = PROPERTY_COLOR.getDefaultValue();
	private Horse.Style style = PROPERTY_STYLE.getDefaultValue();
	private HorseArmor armor = PROPERTY_ARMOR.getDefaultValue();

	public HorseShop(	LivingShops livingShops, SKLivingShopObjectType<HorseShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.color = PROPERTY_COLOR.load(shopkeeper, configSection);
		this.style = PROPERTY_STYLE.load(shopkeeper, configSection);
		this.armor = PROPERTY_ARMOR.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_COLOR.save(shopkeeper, configSection, color);
		PROPERTY_STYLE.save(shopkeeper, configSection, style);
		PROPERTY_ARMOR.save(shopkeeper, configSection, armor);
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

	public void setColor(Horse.Color color) {
		this.color = color;
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // Null if not active
	}

	private void applyColor(Horse entity) {
		if (entity == null) return;
		entity.setColor(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(Horse.Color.class, color, backwards));
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (color) {
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

	public void setStyle(Horse.Style style) {
		this.style = style;
		shopkeeper.markDirty();
		this.applyStyle(this.getEntity()); // Null if not active
	}

	private void applyStyle(Horse entity) {
		if (entity == null) return;
		entity.setStyle(style);
	}

	public void cycleStyle(boolean backwards) {
		this.setStyle(EnumUtils.cycleEnumConstant(Horse.Style.class, style, backwards));
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

	public void setArmor(HorseArmor armor) {
		this.armor = armor;
		shopkeeper.markDirty();
		this.applyArmor(this.getEntity()); // Null if not active
	}

	private void applyArmor(Horse entity) {
		if (entity == null) return;
		entity.getInventory().setArmor(armor == null ? null : new ItemStack(armor.getMaterial()));
	}

	public void cycleArmor(boolean backwards) {
		this.setArmor(EnumUtils.cycleEnumConstantNullable(HorseArmor.class, armor, backwards));
	}

	private ItemStack getArmorEditorItem() {
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
