package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
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
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

// TODO pose (laying, sitting, eating, worried)
public class PandaShop extends BabyableShop<Panda> {

	private static final Property<Panda.Gene> PROPERTY_GENE = new EnumProperty<>(Panda.Gene.class, "gene", Panda.Gene.NORMAL);

	private Panda.Gene gene = PROPERTY_GENE.getDefaultValue();

	public PandaShop(	LivingShops livingShops, SKLivingShopObjectType<PandaShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.gene = PROPERTY_GENE.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_GENE.save(shopkeeper, configSection, gene);
	}

	@Override
	protected void onSpawn(Panda entity) {
		super.onSpawn(entity);
		this.applyGene(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getGeneEditorButton());
		return editorButtons;
	}

	// GENE

	public void setGene(Panda.Gene gene) {
		Validate.notNull(gene, "Gene is null!");
		this.gene = gene;
		shopkeeper.markDirty();
		this.applyGene(this.getEntity()); // null if not active
	}

	private void applyGene(Panda entity) {
		if (entity == null) return;
		entity.setMainGene(gene);
		entity.setHiddenGene(gene);
	}

	public void cycleGene() {
		this.setGene(Utils.cycleEnumConstant(Panda.Gene.class, gene));
	}

	private ItemStack getGeneEditorItem() {
		ItemStack iconItem = new ItemStack(Material.PANDA_SPAWN_EGG);
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getGeneEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getGeneEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleGene();
				return true;
			}
		};
	}
}
