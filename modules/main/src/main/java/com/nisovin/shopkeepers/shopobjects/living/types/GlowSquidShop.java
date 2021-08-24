package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Squid;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.MC_1_17_Utils;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// TODO Use actual GlowSquid type once we only support Bukkit 1.17 upwards
public class GlowSquidShop extends SKLivingShopObject<Squid> {

	private final Property<Boolean> darkGlowSquidProperty = new BooleanProperty(shopkeeper, "darkGlowSquid", false);

	public GlowSquidShop(	LivingShops livingShops, SKLivingShopObjectType<GlowSquidShop> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		darkGlowSquidProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		darkGlowSquidProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn(Squid entity) {
		super.onSpawn(entity);
		this.applyDark(entity);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getDarkEditorButton());
		return editorButtons;
	}

	// DARK

	public boolean isDark() {
		return darkGlowSquidProperty.getValue();
	}

	public void setDark(boolean dark) {
		darkGlowSquidProperty.setValue(dark);
		shopkeeper.markDirty();
		this.applyDark(this.getEntity()); // Null if not spawned
	}

	public void cycleDark(boolean backwards) {
		this.setDark(!this.isDark());
	}

	private void applyDark(Squid entity) {
		if (entity == null) return;
		NMSManager.getProvider().setGlowSquidDark(this.getEntity(), this.isDark());
	}

	private ItemStack getDarkEditorItem() {
		ItemStack iconItem;
		if (this.isDark()) {
			iconItem = new ItemStack(Material.INK_SAC);
		} else {
			iconItem = new ItemStack(MC_1_17_Utils.MATERIAL_GLOW_INK_SAC);
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonGlowSquidDark, Messages.buttonGlowSquidDarkLore);
		return iconItem;
	}

	private Button getDarkEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getDarkEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleDark(backwards);
				return true;
			}
		};
	}
}
