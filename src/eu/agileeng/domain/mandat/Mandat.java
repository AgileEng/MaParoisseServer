package eu.agileeng.domain.mandat;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.cash.CFC;
import eu.agileeng.domain.cash.CFCCell;
import eu.agileeng.domain.cash.CFCColumn;
import eu.agileeng.domain.cash.CFCRow;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValue;

public class Mandat extends CFC {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4626664986089051539L;

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = superToJSONObject();
		
		// model
		if(getCfcModel() != null) {
			json.put("cfcModel", getCfcModel().toJSONArray());
		}
		
		// data
		
		/**
		 * metaData : {
		 *                root : cfcRows,
		 *                fields : {
		 *                    
		 *                }
		 *             }
		 */
		JSONObject jsonCFCData = new JSONObject();
		json.put("cfcData", jsonCFCData);
		
		JSONObject metaData = new JSONObject();
		jsonCFCData.put("metaData", metaData);
		metaData.put("totalProperty", "total");
		metaData.put("root", "cfcRows");
		metaData.put("idProperty", "id");
		
		JSONArray fieldsArray = new JSONArray();
		metaData.put("fields", fieldsArray);
		metaData.put("fields", fieldsArray);
		{
			JSONObject field1 = new JSONObject();
			field1.put("name", "date");
			field1.put("type", "string");
			fieldsArray.put(field1);

			JSONObject field2 = new JSONObject();
			field2.put("name", "value");
			fieldsArray.put(field2);

			JSONObject field3 = new JSONObject();
			field3.put("name", "id");
			field3.put("type", "int");
			fieldsArray.put(field3);

			JSONObject field4 = new JSONObject();
			field4.put("name", "sysId");
			field4.put("type", "long");
			fieldsArray.put(field4);

			JSONObject field5 = new JSONObject();
			field5.put("name", "code");
			field5.put("type", "string");
			fieldsArray.put(field5);

			JSONObject fieldSIndex = new JSONObject();
			fieldSIndex.put("name", "sIndex");
			fieldSIndex.put("type", "int");
			fieldsArray.put(fieldSIndex);

			JSONObject field6 = new JSONObject();
			field6.put("name", "description");
			field6.put("type", "string");
			fieldsArray.put(field6);

			JSONObject field12 = new JSONObject();
			field12.put("name", "ownerId");
			field12.put("type", "long");
			fieldsArray.put(field12);		
		}
		
		// columns static definition
		JSONArray columns = new JSONArray();
		jsonCFCData.put("columns", columns);	
		{
			JSONObject column0 = getColumn0Definition();
			columns.put(column0);
			
			JSONObject column1 = getColumn1Definition();
			columns.put(column1);
		
			// for every column add its definition
			if(getCfcModel() != null) {
				for (CFCColumn cfcColumn : getCfcModel()) {
					/**
					 * add column definition
					 */
					JSONObject jsonColumn = getColumnDefinition(cfcColumn);

					columns.put(jsonColumn);
					
					/**
					 * add field definition
					 */
					JSONObject field = new JSONObject();
					String valuePropertyName = "value" + "_" + cfcColumn.getID();
					field.put("name", valuePropertyName);
					if(!AEValue.isNull(cfcColumn.getValue()) 
							&& AEValue.XType.DOUBLE.equals(cfcColumn.getValue().getXType())) {
						
						field.put("type", "float");
					} else {
						field.put("type", "string");
					}
					fieldsArray.put(field);
				}
			}
		}
			
		// records static definition
		jsonCFCData.put("cfcRows", dataToJSONArray());
		
		return json;
	}
	
	@Override
	public JSONArray dataToJSONArray() throws JSONException {
		JSONArray jsonRows = new JSONArray();
		// for every row add its definition
		if(getCfcData() != null) {
			for (CFCRow cfcRow : getCfcData()) {
				JSONObject jsonRow = new JSONObject();
				jsonRow.put("sysId", cfcRow.getSysId());
				if(cfcRow.getSysId() == CFCRow.DATE_ENTRY) {
					jsonRow.put("code", "Entrées");
				} else if(cfcRow.getSysId() == CFCRow.SUM) {
					jsonRow.put("code", "Somme");
				} 
				if(cfcRow.getDate() != null) {
					jsonRow.put(
							"date", 
							AEDateUtil.convertToString(cfcRow.getDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
				} else if(cfcRow.getSysId() == CFCRow.SUM){
					jsonRow.put(
							"date", 
					"SOMME");
				} else if(cfcRow.getSysId() == CFCRow.SUM){
					jsonRow.put(
							"date", 
							AEStringUtil.EMPTY_STRING);
				}
				for (CFCCell cfcCell : cfcRow) {
					String valuePropertyName = "value" + "_" + cfcCell.getColumn().getID();
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
		JSONObject jsonColumn = new JSONObject();
		
		String valuePropertyName = "value" + "_" + cfcColumn.getID();
		
		jsonColumn.put("header", cfcColumn.getName());
		jsonColumn.put("dataIndex", valuePropertyName);
		jsonColumn.put("width", 100);
		jsonColumn.put("tooltip", cfcColumn.getName());
		jsonColumn.put("align", "right");
		jsonColumn.put("sortable", false);
		jsonColumn.put("hideable", false);
		jsonColumn.put("hidden", cfcColumn.isHidden());
		jsonColumn.put("cfcColumnNType", cfcColumn.getNType());

		// 
		if(CFCColumn.NType.ACCOUNT.equals(cfcColumn.getNType())) {
			// double and not editable
			jsonColumn.put("editable", false);
			jsonColumn.put("align", "right");
			jsonColumn.put("xtype", "numbercolumn");
		} else if(CFCColumn.NType.EXPRESSION.equals(cfcColumn.getNType())) {
			// double and not editable
			jsonColumn.put("editable", false);
			jsonColumn.put("align", "right");
		} else if(CFCColumn.NType.EXPRESSION_COB.equals(cfcColumn.getNType())) {
			jsonColumn.put("editable", true);
			jsonColumn.put("xtype", "numbercolumn");
			jsonColumn.put("_editor", "NumberField");
			jsonColumn.put("align", "right");
		} else if(CFCColumn.NType.VALUE.equals(cfcColumn.getNType())) {
			jsonColumn.put("editable", true);
			if(!AEValue.isNull(cfcColumn.getValue()) && AEValue.XType.DOUBLE.equals(cfcColumn.getValue().getXType())) {
				jsonColumn.put("xtype", "numbercolumn");
				jsonColumn.put("_editor", "NumberField");
				jsonColumn.put("align", "right");
				jsonColumn.put("css", "background-color:#FFFFCC;");
			} else if(!AEValue.isNull(cfcColumn.getValue()) && AEValue.XType.STRING.equals(cfcColumn.getValue().getXType())) {
				// xType by default
				jsonColumn.put("_editor", "TextField");
				jsonColumn.put("align", "left");
			}
		}
		
		return jsonColumn;
	}
}
