package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
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
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class RabbitShop extends BabyableShop<Rabbit> {

	private static final Property<Rabbit.Type> PROPERTY_RABBIT_TYPE = new EnumProperty<>(Rabbit.Type.class, "rabbitType", Rabbit.Type.BROWN);

	private Rabbit.Type rabbitType = PROPERTY_RABBIT_TYPE.getDefaultValue();

	public RabbitShop(	LivingShops livingShops, SKLivingShopObjectType<RabbitShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.rabbitType = PROPERTY_RABBIT_TYPE.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_RABBIT_TYPE.save(shopkeeper, configSection, rabbitType);
	}

	@Override
	protected void onSpawn(Rabbit entity) {
		super.onSpawn(entity);
		this.applyRabbitType(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getRabbitTypeEditorButton());
		return editorButtons;
	}

	// RABBIT TYPE

	public void setRabbitType(Rabbit.Type rabbitType) {
		Validate.notNull(rabbitType, "rabbitType is null");
		this.rabbitType = rabbitType;
		shopkeeper.markDirty();
		this.applyRabbitType(this.getEntity()); // null if not active
	}

	private void applyRabbitType(Rabbit entity) {
		if (entity == null) return;
		if (rabbitType == Rabbit.Type.THE_KILLER_BUNNY) {
			// Special handling if the rabbit type is the killer rabbit:
			// If the entity's custom name is not empty, Minecraft applies the 'The Killer Rabbit' name. We therefore
			// temporarily set the custom name to something non-empty:
			String customName = entity.getCustomName(); // can be null
			entity.setCustomName(" "); // non-empty
			entity.setRabbitType(rabbitType);
			entity.setCustomName(customName); // reset previous name
			// CraftBukkit and Minecraft reset the rabbit's pathfinder goals, so we clear them again:
			this.overwriteAI();
			// Minecraft also sets the rabbit's armor attribute, but this doesn't affect us.
		} else {
			entity.setRabbitType(rabbitType);
		}
	}

	public void cycleRabbitType(boolean backwards) {
		this.setRabbitType(EnumUtils.cycleEnumConstant(Rabbit.Type.class, rabbitType, backwards));
	}

	private ItemStack getRabbitTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (rabbitType) {
		case BROWN:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(141, 118, 88));
			break;
		case WHITE:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case BLACK:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(31, 31, 31));
			break;
		case BLACK_AND_WHITE:
			ItemUtils.setLeatherColor(iconItem, Color.GRAY);
			break;
		case GOLD:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(246, 224, 136));
			break;
		case SALT_AND_PEPPER:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(138, 120, 98));
			break;
		case THE_KILLER_BUNNY:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(142, 11, 28));
			break;
		default:
			// unknown type:
			ItemUtils.setLeatherColor(iconItem, Color.PURPLE);
			break;
		}
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonRabbitVariant, Settings.msgButtonRabbitVariantLore);
		return iconItem;
	}

	private EditorHandler.Button getRabbitTypeEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getRabbitTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleRabbitType(backwards);
				return true;
			}
		};
	}
}
