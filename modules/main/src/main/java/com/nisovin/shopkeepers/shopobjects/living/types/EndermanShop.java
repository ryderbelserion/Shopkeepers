package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Enderman;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopEquipment;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class EndermanShop extends SKLivingShopObject<Enderman> {

	public EndermanShop(
			LivingShops livingShops,
			SKLivingShopObjectType<? extends EndermanShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyCarriedBlock();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		return editorButtons;
	}

	// EQUIPMENT

	@Override
	protected void onEquipmentChanged() {
		super.onEquipmentChanged();

		// Apply the carried block based on the item in hand:
		this.applyCarriedBlock();
	}

	// CARRIED BLOCK (edited via the equipment editor)

	private void applyCarriedBlock() {
		@Nullable Enderman entity = this.getEntity();
		if (entity == null) return; // Not spawned

		@Nullable BlockData blockData = null;

		LivingShopEquipment equipment = this.getEquipment();
		@Nullable UnmodifiableItemStack item = equipment.getItem(EquipmentSlot.HAND);
		if (!ItemUtils.isEmpty(item)) {
			assert item != null;

			Material blockType = item.getType();

			// We allow players to specify the block via a placeholder item. If the block has no
			// corresponding item type, the placeholder item was not substituted yet and we retrieve
			// the substituted block type here instead:
			// We need to check if the item is a placeholder item first, because the placeholder
			// item might be configured to be a block type itself.
			@Nullable Material substitutedMaterial = PlaceholderItems.getSubstitutedMaterial(ItemUtils.asItemStack(item));
			if (substitutedMaterial != null && substitutedMaterial.isBlock()) {
				blockType = substitutedMaterial;
			}

			if (blockType.isBlock()) {
				// If the (possibly placeholder) item, contains block state data, apply it to the
				// carried block:
				@Nullable ItemMeta itemMeta = item.getItemMeta();
				if (itemMeta instanceof BlockDataMeta) {
					BlockDataMeta blockDataMeta = (BlockDataMeta) itemMeta;
					if (blockDataMeta.hasBlockData()) {
						// For placeholder items, the block data is applied to the substituted block
						// type:
						blockData = blockDataMeta.getBlockData(blockType);
					}
				}

				// Else: Use the default data for the block type:
				if (blockData == null) {
					blockData = blockType.createBlockData();
				}
			}
		}

		entity.setCarriedBlock(blockData);
	}
}
