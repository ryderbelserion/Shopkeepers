package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// TODO Use actual Goal type once we only support Bukkit 1.17 upwards
public class GoatShop extends BabyableShop<@NonNull Animals> {

	public static final Property<@NonNull Boolean> SCREAMING = new BasicProperty<@NonNull Boolean>()
			.dataKeyAccessor("screaming", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<@NonNull Boolean> screamingProperty = new PropertyValue<>(SCREAMING)
			.onValueChanged(Unsafe.initialized(this)::applyScreaming)
			.build(properties);

	public GoatShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull GoatShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		screamingProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		screamingProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyScreaming();
	}

	@Override
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
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
	}

	public void cycleScreaming(boolean backwards) {
		this.setScreaming(!this.isScreaming());
	}

	private void applyScreaming() {
		Animals entity = this.getEntity();
		if (entity == null) return; // Not spawned
		NMSManager.getProvider().setScreamingGoat(entity, this.isScreaming());
	}

	private ItemStack getScreamingEditorItem() {
		ItemStack iconItem;
		if (this.isScreaming()) {
			iconItem = new ItemStack(Material.CARVED_PUMPKIN);
		} else {
			iconItem = new ItemStack(Material.PUMPKIN);
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonGoatScreaming,
				Messages.buttonGoatScreamingLore
		);
		return iconItem;
	}

	private Button getScreamingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getScreamingEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleScreaming(backwards);
				return true;
			}
		};
	}
}
