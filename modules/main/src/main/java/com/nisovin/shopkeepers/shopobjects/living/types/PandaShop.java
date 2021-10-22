package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

// TODO Pose (laying, sitting, eating, worried).
public class PandaShop extends BabyableShop<Panda> {

	private final Property<Panda.Gene> geneProperty = new EnumProperty<>(Panda.Gene.class)
			.key("gene")
			.defaultValue(Panda.Gene.NORMAL)
			.onValueChanged(this::applyGene)
			.build(properties);

	public PandaShop(	LivingShops livingShops, SKLivingShopObjectType<PandaShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		geneProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		geneProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyGene();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getGeneEditorButton());
		return editorButtons;
	}

	// GENE

	public Panda.Gene getGene() {
		return geneProperty.getValue();
	}

	public void setGene(Panda.Gene gene) {
		geneProperty.setValue(gene);
	}

	public void cycleGene(boolean backwards) {
		this.setGene(EnumUtils.cycleEnumConstant(Panda.Gene.class, this.getGene(), backwards));
	}

	private void applyGene() {
		Panda entity = this.getEntity();
		if (entity == null) return; // Not spawned
		Panda.Gene gene = this.getGene();
		entity.setMainGene(gene);
		entity.setHiddenGene(gene);
	}

	private ItemStack getGeneEditorItem() {
		ItemStack iconItem = new ItemStack(Material.PANDA_SPAWN_EGG);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonPandaVariant, Messages.buttonPandaVariantLore);
		return iconItem;
	}

	private Button getGeneEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getGeneEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleGene(backwards);
				return true;
			}
		};
	}
}
