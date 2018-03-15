/**
 * 
 */
package eu.agileeng.domain.cash;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEValue;

/**
 * @author vvatov
 *
 */
public class CFCCell extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3805427005505770300L;

	private AEDescriptive toCFC;
	
	private Date entryDate;
	
	private int rowIndex;
	
	private long columnId;
	
	private CFCColumn column;
	
	private AEValue value;
	
	private boolean paid;
	
	private long sysId = CFCRow.DATE_ENTRY;
	
	private AEDescriptor accBank;
	
	private long gridId;
	
	public CFCCell() {
		super(DomainClass.CFCCell);
	}
	
	public CFCCell(long sysId) {
		this();
		this.sysId = sysId;
	}
	
	public CFCCell(CFCColumn cfcColumn, Date entryDate, int rowIndex, AEValue value) {
		this();
		this.toCFC = cfcColumn.getToCFC();
		this.entryDate = entryDate;
		this.rowIndex = rowIndex;
		this.columnId = cfcColumn.getID();
		this.column = cfcColumn;
		this.value = value;
	}
	
	public CFCCell(CFCColumn cfcColumn, Date entryDate, int rowIndex, AEValue value, long gridId) {
		this();
		this.toCFC = cfcColumn.getToCFC();
		this.entryDate = entryDate;
		this.rowIndex = rowIndex;
		this.columnId = cfcColumn.getID();
		this.column = cfcColumn;
		this.value = value;
		this.gridId = gridId;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put("date", AEDateUtil.convertToString(getEntryDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		if(!AEValue.isNull(getValue())) {
			json.put("amount", getValue().optDouble());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		
	}

	public AEDescriptive getToCFC() {
		return toCFC;
	}

	public void setToCFC(AEDescriptive toCFC) {
		this.toCFC = toCFC;
	}

	public Date getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(Date entryDate) {
		this.entryDate = entryDate;
	}

	public CFCColumn getColumn() {
		return column;
	}

	public void setColumn(CFCColumn cfcColumn) {
		this.column = cfcColumn;
	}

	public AEValue getValue() {
		return value;
	}

	public void setValue(AEValue value) {
		this.value = value;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	public void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}

	public long getColumnId() {
		return columnId;
	}

	public void setColumnId(long columnId) {
		this.columnId = columnId;
	}
	
	public void addDouble(AEValue value) {
		if(!AEValue.isNull(this.value)) {
			this.value.set(this.value.optDouble() + value.optDouble());
		} else {
			this.value = new AEValue();
			this.value.set(value.optDouble());
		}
	}

	public long getSysId() {
		return sysId;
	}

	public void setSysId(long sysId) {
		this.sysId = sysId;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}
	
	public static AEDescriptor lazyDescriptorCFC(long id) {
		return new AEDescriptorImp(id, DomainClass.CFCCell);
	}

	public static AEDescriptor lazyDescriptorMandat(long id) {
		return new AEDescriptorImp(id, DomainClass.MandatCell);
	}
	
	public AEDescriptor getAccBank() {
		return accBank;
	}

	public void setAccBank(AEDescriptor accBank) {
		this.accBank = accBank;
	}

	public long getGridId() {
		return gridId;
	}

	public void setGridId(long gridId) {
		this.gridId = gridId;
	}
}
