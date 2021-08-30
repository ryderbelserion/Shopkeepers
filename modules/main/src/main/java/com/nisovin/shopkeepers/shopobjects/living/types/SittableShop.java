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
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// Using Babyable as common super type of all sittable mobs for now.
public class SittableShop<E extends Ageable & Sittable> extends BabyableShop<E> {

	private final Property<Boolean> sittingProperty = new BooleanProperty()
			.key("sitting")
			.defaultValue(false)
			.build(properties);

	public SittableShop(LivingShops livingShops, SKLivingShopObjectType<? extends SittableShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		sittingProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		sittingProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn(E entity) {
		super.onSpawn(entity);
		this.applySitting(entity);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSittingEditorButton());
		return editorButtons;
	}

	// SITTING

	public boolean isSitting() {
		return sittingProperty.getValue();
	}

	public void setSitting(boolean sitting) {
		sittingProperty.setValue(sitting);
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

	private Button getSittingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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
