package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.EditorHandler;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// Using Babyable as common super type of all sittable mobs for now.
public class SittableShop<E extends Ageable & Sittable> extends BabyableShop<E> {

	private final Property<Boolean> sittingProperty = new BooleanProperty(shopkeeper, "sitting", false);

	public SittableShop(LivingShops livingShops, SKLivingShopObjectType<? extends SittableShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		sittingProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		sittingProperty.save(configSection);
	}

	@Override
	protected void onSpawn(E entity) {
		super.onSpawn(entity);
		this.applySitting(entity);
	}

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSittingEditorButton());
		return editorButtons;
	}

	// SITTING

	public boolean isSitting() {
		return sittingProperty.getValue();
	}

	public void setSitting(boolean sitting) {
		sittingProperty.setValue(sitting);
		shopkeeper.markDirty();
		this.applySitting(this.getEntity()); // Null if not spawned
	}

	public void cycleSitting() {
		this.setSitting(!this.isSitting());
	}

	private void applySitting(Sittable entity) {
		if (entity == null) return;
		entity.setSitting(this.isSitting());
	}

	private ItemStack getSittingEditorItem() {
		ItemStack iconItem = new ItemStack(Material.IRON_HORSE_ARMOR);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSitting, Messages.buttonSittingLore);
		return iconItem;
	}

	private EditorHandler.Button getSittingEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getSittingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleSitting();
				return true;
			}
		};
	}
}
