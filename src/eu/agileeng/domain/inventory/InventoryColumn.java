package eu.agileeng.domain.inventory;

import eu.agileeng.domain.cash.CFCColumn;

public class InventoryColumn extends CFCColumn {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7993245265414548621L;
	
	private long saleAttrId;

	private long supplyAttrId;

	public long getSaleAttrId() {
		return saleAttrId;
	}

	public void setSaleAttrId(long saleAttrId) {
		this.saleAttrId = saleAttrId;
	}

	public long getSupplyAttrId() {
		return supplyAttrId;
	}

	public void setSupplyAttrId(long supplyAttrId) {
		this.supplyAttrId = supplyAttrId;
	}
}
