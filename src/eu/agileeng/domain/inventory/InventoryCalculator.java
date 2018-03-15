package eu.agileeng.domain.inventory;

import java.util.ArrayList;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.cash.CFCCell;
import eu.agileeng.domain.cash.CFCData;
import eu.agileeng.domain.cash.CFCRow;
import eu.agileeng.util.AEValue;

public class InventoryCalculator {

	/**
	 * rowD = rowA + rowB - rowC
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowD(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowA = invStatusData.getRow(InventoryRow.Type.A.getSysId());
		CFCRow rowB = invStatusData.getRow(InventoryRow.Type.B.getSysId());
		CFCRow rowC = invStatusData.getRow(InventoryRow.Type.C.getSysId());
		
		CFCCell cellA = null;
		if(rowA != null) {
			cellA = rowA.getCell(invColumn.getID());
		}
		
		CFCCell cellB = null;
		if(rowB != null) {
			cellB = rowB.getCell(invColumn.getID());
		}
		
		CFCCell cellC = null;
		if(rowC != null) {
			cellC = rowC.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellA != null && !AEValue.isNull(cellA.getValue())) {
			try {
				value += cellA.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		if(cellB != null && !AEValue.isNull(cellB.getValue())) {
			try {
				value += cellB.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		if(cellC != null && !AEValue.isNull(cellC.getValue())) {
			try {
				value -= cellC.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowD = rowA + rowB - rowC
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowDAnnually(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowA = invStatusData.getRow(InventoryRow.Type.A_ANNUALLY.getSysId());
		CFCRow rowB = invStatusData.getRow(InventoryRow.Type.B_ANNUALLY.getSysId());
		CFCRow rowC = invStatusData.getRow(InventoryRow.Type.C_ANNUALLY.getSysId());
		
		CFCCell cellA = null;
		if(rowA != null) {
			cellA = rowA.getCell(invColumn.getID());
		}
		
		CFCCell cellB = null;
		if(rowB != null) {
			cellB = rowB.getCell(invColumn.getID());
		}
		
		CFCCell cellC = null;
		if(rowC != null) {
			cellC = rowC.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellA != null && !AEValue.isNull(cellA.getValue())) {
			try {
				value += cellA.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		if(cellB != null && !AEValue.isNull(cellB.getValue())) {
			try {
				value += cellB.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		if(cellC != null && !AEValue.isNull(cellC.getValue())) {
			try {
				value -= cellC.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowF = rowE - rowD
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowF(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowE = invStatusData.getRow(InventoryRow.Type.E.getSysId());
		CFCRow rowD = invStatusData.getRow(InventoryRow.Type.D.getSysId());
		
		CFCCell cellE = null;
		if(rowE != null) {
			cellE = rowE.getCell(invColumn.getID());
		}
		
		CFCCell cellD = null;
		if(rowD != null) {
			cellD = rowD.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellE != null && !AEValue.isNull(cellE.getValue())) {
			try {
				value += cellE.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
			
		if(cellD != null && !AEValue.isNull(cellD.getValue())) {
			try {
				value -= cellD.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowF = rowE - rowD
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowFAnnually(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowE = invStatusData.getRow(InventoryRow.Type.E_ANNUALLY.getSysId());
		CFCRow rowD = invStatusData.getRow(InventoryRow.Type.D_ANNUALLY.getSysId());
		
		CFCCell cellE = null;
		if(rowE != null) {
			cellE = rowE.getCell(invColumn.getID());
		}
		
		CFCCell cellD = null;
		if(rowD != null) {
			cellD = rowD.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellE != null && !AEValue.isNull(cellE.getValue())) {
			try {
				value += cellE.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
			
		if(cellD != null && !AEValue.isNull(cellD.getValue())) {
			try {
				value -= cellD.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowH = rowG + rowF
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowH(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowG = invStatusData.getRow(InventoryRow.Type.G.getSysId());
		CFCRow rowF = invStatusData.getRow(InventoryRow.Type.F.getSysId());
		
		CFCCell cellG = null;
		if(rowG != null) {
			cellG = rowG.getCell(invColumn.getID());
		}
		
		CFCCell cellF = null;
		if(rowF != null) {
			cellF = rowF.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellG != null && !AEValue.isNull(cellG.getValue())) {
			try {
				value += cellG.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
			
		if(cellF != null && !AEValue.isNull(cellF.getValue())) {
			try {
				value += cellF.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowH = rowG + rowF
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowHAnnually(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowG = invStatusData.getRow(InventoryRow.Type.G_ANNUALLY.getSysId());
		CFCRow rowF = invStatusData.getRow(InventoryRow.Type.F_ANNUALLY.getSysId());
		
		CFCCell cellG = null;
		if(rowG != null) {
			cellG = rowG.getCell(invColumn.getID());
		}
		
		CFCCell cellF = null;
		if(rowF != null) {
			cellF = rowF.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellG != null && !AEValue.isNull(cellG.getValue())) {
			try {
				value += cellG.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
			
		if(cellF != null && !AEValue.isNull(cellF.getValue())) {
			try {
				value += cellF.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowJ = rowH - rowI
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowJ(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowH = invStatusData.getRow(InventoryRow.Type.H.getSysId());
		CFCRow rowI = invStatusData.getRow(InventoryRow.Type.I.getSysId());
		
		CFCCell cellH = null;
		if(rowH != null) {
			cellH = rowH.getCell(invColumn.getID());
		}
		
		CFCCell cellI = null;
		if(rowI != null) {
			cellI = rowI.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellH != null && !AEValue.isNull(cellH.getValue())) {
			try {
				value += cellH.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
			
		if(cellI != null && !AEValue.isNull(cellI.getValue())) {
			try {
				value -= cellI.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowJ = rowH - rowI
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowJAnnually(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowH = invStatusData.getRow(InventoryRow.Type.H_ANNUALLY.getSysId());
		CFCRow rowI = invStatusData.getRow(InventoryRow.Type.I_ANNUALLY.getSysId());
		
		CFCCell cellH = null;
		if(rowH != null) {
			cellH = rowH.getCell(invColumn.getID());
		}
		
		CFCCell cellI = null;
		if(rowI != null) {
			cellI = rowI.getCell(invColumn.getID());
		}
		
		// calculate
		if(cellH != null && !AEValue.isNull(cellH.getValue())) {
			try {
				value += cellH.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
			
		if(cellI != null && !AEValue.isNull(cellI.getValue())) {
			try {
				value -= cellI.getValue().getDouble();
			} catch (AEException e) {
				AEApp.logger().error("Cannot calculate value", e);
			}
		}
		
		return value;
	}
	
	/**
	 * rowK = rowF * 1000 / rowC
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowK(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowF = invStatusData.getRow(InventoryRow.Type.F.getSysId());
		CFCRow rowC = invStatusData.getRow(InventoryRow.Type.C.getSysId());
		
		CFCCell cellF = null;
		if(rowF != null) {
			cellF = rowF.getCell(invColumn.getID());
		}
		
		CFCCell cellC = null;
		if(rowC != null) {
			cellC = rowC.getCell(invColumn.getID());
		}
		
		// calculate
		try {
			if(cellC != null && !AEValue.isNull(cellC.getValue())) {
				double c = cellC.getValue().getDouble();
				if(cellF != null && !AEValue.isNull(cellF.getValue()) && c != 0.0) {
					value = cellF.getValue().getDouble() * 1000.0 / c;
				}
			}
		} catch (AEException e) {
			AEApp.logger().error("Cannot calculate value", e);
		}
		
		return value;
	}
	
	/**
	 * rowK = rowF * 1000 / rowC
	 * 
	 * @param invStatusData
	 * @param invColumn
	 * @return
	 */
	public static final double calculateRowKAnnually(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		CFCRow rowF = invStatusData.getRow(InventoryRow.Type.F_ANNUALLY.getSysId());
		CFCRow rowC = invStatusData.getRow(InventoryRow.Type.C_ANNUALLY.getSysId());
		
		CFCCell cellF = null;
		if(rowF != null) {
			cellF = rowF.getCell(invColumn.getID());
		}
		
		CFCCell cellC = null;
		if(rowC != null) {
			cellC = rowC.getCell(invColumn.getID());
		}
		
		// calculate
		try {
			if(cellC != null && !AEValue.isNull(cellC.getValue())) {
				double c = cellC.getValue().getDouble();
				if(cellF != null && !AEValue.isNull(cellF.getValue()) && c != 0.0) {
					value = cellF.getValue().getDouble() * 1000.0 / c;
				}
			}
		} catch (AEException e) {
			AEApp.logger().error("Cannot calculate value", e);
		}
		
		return value;
	}
	
	public static final CFCCell getCFCCell(ArrayList<CFCCell> savedCells, long rowSysId, InventoryColumn invColumn) {
		CFCCell found = null;
		for (CFCCell cfcCell : savedCells) {
			if(cfcCell.getColumnId() == invColumn.getID() && cfcCell.getRowIndex() == rowSysId) {
				found = cfcCell;
				break;
			}
		}
		return found;
	}
	
	public static final double calculateFreeSum(CFCData invStatusData, InventoryColumn invColumn) {
		double value = 0.0;
		
		for (CFCRow cfcRow : invStatusData) {
			if(cfcRow.getSysId() >= InventoryRow.Type.FREE_1.getSysId() 
					&& cfcRow.getSysId() < InventoryRow.Type.FREE_SUM.getSysId()) {
				
				CFCCell cell = cfcRow.getCell(invColumn.getID());
				if(cell != null && !AEValue.isNull(cell.getValue())) {
					try {
						value += cell.getValue().getDouble();
					} catch (AEException e) {
						AEApp.logger().error("Cannot calculate value", e);
					}
				}
			}
		}
		
		return value;
	}
}
