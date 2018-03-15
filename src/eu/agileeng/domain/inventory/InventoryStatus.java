package eu.agileeng.domain.inventory;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.cash.CFC;
import eu.agileeng.domain.cash.CFCCell;
import eu.agileeng.domain.cash.CFCColumn;
import eu.agileeng.domain.cash.CFCRow;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValue;

public class InventoryStatus extends CFC {

	private int month;
	
	private int year;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3907883426289884432L;

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		json.put("month", month);
		json.put("year", year);
		return json;
	}
	
	@Override
	public JSONObject getColumn1Definition() throws JSONException {
		JSONObject column1 = new JSONObject();
		
		column1.put("header", "Date");
		column1.put("width", 180);
		column1.put("sortable", false);
		column1.put("dataIndex", "date");
		column1.put("hideable", false);
		column1.put("hidden", false);
		column1.put("locked", true);
		
		return column1;
	}
	
	@Override
	public JSONArray dataToJSONArray() throws JSONException {
		JSONArray jsonRows = new JSONArray();
		// for every row add its definition
		if(getCfcData() != null) {
			for (CFCRow cfcRow : getCfcData()) {
				JSONObject jsonRow = new JSONObject();
				
				// sysId
				jsonRow.put("sysId", cfcRow.getSysId());

				// code (group name)
				if(cfcRow.getSysId() < InventoryRow.Type.FREE_1.getSysId()) {
					jsonRow.put("code", "A/ Suivi des ecarts de stocks");
				} else if(cfcRow.getSysId() >= InventoryRow.Type.FREE_1.getSysId() && cfcRow.getSysId() < InventoryRow.Type.FREE_SUM.getSysId()) {
					jsonRow.put("code", "B/ Controle des ventes");
				} else if(cfcRow.getSysId() == InventoryRow.Type.FREE_SUM.getSysId()) {
					jsonRow.put("code", "B/ Somme");
				} else if(cfcRow.getSysId() > InventoryRow.Type.FREE_SUM.getSysId()) {
					jsonRow.put("code", "Situation annuelle des stocks de ");
				}
				
				// date (row header)
				if(cfcRow instanceof InventoryRow) {
					jsonRow.put(
							"date", 
							((InventoryRow) cfcRow).getDescription());
				} 
				
				// dynamic fields
				for (CFCCell cfcCell : cfcRow) {
					String valuePropertyName = "value" + "_" + cfcCell.getColumn().getColIndex();
					if(!AEValue.isNull(cfcCell.getValue())) {
						jsonRow.put(
								valuePropertyName, 
								cfcCell.getValue().toString());
					} else {
						jsonRow.put(
								valuePropertyName, 
								AEStringUtil.EMPTY_STRING);
					}
				}

				jsonRows.put(jsonRow);
			}
		}
		return jsonRows;
	}
	
	@Override
	public JSONObject getColumnDefinition(CFCColumn cfcColumn) throws JSONException {
		JSONObject jsonColumn = super.getColumnDefinition(cfcColumn);
		
		jsonColumn.put("editable", true);
		jsonColumn.put("align", "right");
		jsonColumn.put("xtype", "numbercolumn");
		jsonColumn.put("_editor", "NumberField");
//		jsonColumn.put("css", "background-color:#FFFFCC;");
		
		return jsonColumn;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}
}
