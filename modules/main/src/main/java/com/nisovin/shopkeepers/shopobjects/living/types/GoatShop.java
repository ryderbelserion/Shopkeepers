package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// TODO Use actual Goal type once we only support Bukkit 1.17 upwards
public class GoatShop extends BabyableShop<Animals> {

	private final Property<Boolean> screamingProperty = new BooleanProperty()
			.key("screaming")
			.defaultValue(false)
			.build(properties);

	public GoatShop(LivingShops livingShops, SKLivingShopObjectType<GoatShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		screamingProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		screamingProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn(Animals entity) {
		super.onSpawn(entity);
		this.applyScreaming(entity);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		// The screaming option is hidden if shopkeeper mobs are silent.
		if (!Settings.silenceLivingShopEntities) {
			editorButtons.add(this.getScreamingEditorButton());
		}
		return editorButtons;
	}

	// SCREAMING

	public boolean isScreaming() {
		return screamingProperty.getValue();
	}

	public void setScreaming(boolean screaming) {
		screamingProperty.setValue(screaming);
		this.applyScreaming(this.getEntity()); // Null if not spawned
	}

	public void cycleScreaming(boolean backwards) {
		this.setScreaming(!this.isScreaming());
	}

	private void applyScreaming(Animals entity) {
		if (entity == null) return;
		NMSManager.getProvider().setScreamingGoat(this.getEntity(), this.isScreaming());
	}

	private ItemStack getScreamingEditorItem() {
		ItemStack iconItem;
		if (this.isScreaming()) {
			iconItem = new ItemStack(Material.CARVED_PUMPKIN);
		} else {
			iconItem = new ItemStack(Material.PUMPKIN);
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonGoatScreaming, Messages.buttonGoatScreamingLore);
		return iconItem;
	}

	private Button getScreamingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getScreamingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleScreaming(backwards);
				return true;
			}
		};
	}
}
