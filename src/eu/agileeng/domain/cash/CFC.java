/**
 * 
 */
package eu.agileeng.domain.cash;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValue;

/**
 * @author vvatov
 *
 */
public class CFC extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3617647350214144042L;

	private CFCModel cfcModel;
	
	private CFCData cfcData;
	
	private long mapId;
	
	/**
	 * @param clazz
	 */
	public CFC() {
		super(DomainClass.CFC);
	}

	public CFCModel getCfcModel() {
		return cfcModel;
	}

	public void setCfcModel(CFCModel cfcModel) {
		this.cfcModel = cfcModel;
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.CFC);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// model
		if(this.cfcModel != null) {
			json.put("cfcModel", this.cfcModel.toJSONArray());
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
			if(this.cfcModel != null) {
				for (CFCColumn cfcColumn : this.cfcModel) {
					/**
					 * add column definition
					 */
					JSONObject jsonColumn = getColumnDefinition(cfcColumn);
					
					columns.put(jsonColumn);
					
					/**
					 * add field definition
					 */
					JSONObject field = new JSONObject();
					String valuePropertyName = "value" + "_" + cfcColumn.getColIndex();
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
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("cfcModel")) {
			this.cfcModel = new CFCModel();
			cfcModel.create(jsonObject.getJSONArray("cfcModel"));
		}
	}

	public CFCData getCfcData() {
		return cfcData;
	}

	public void setCFCData(CFCData data) {
		this.cfcData = data;
	}
	
	public void addRow(CFCRow cfcRow) {
		if(this.cfcData == null) {
			this.cfcData = new CFCData();
		}
		cfcData.add(cfcRow);
	}
	
	public CFCColumn getColumn(long id) {
		CFCColumn col = null;
		if(this.cfcModel != null) {
			col = this.cfcModel.getColumn(id);
		}
		return col;
	}

	public long getMapId() {
		return mapId;
	}

	public void setMapId(long mapId) {
		this.mapId = mapId;
	}
	
	/**
	 * Encodes the CFCData to JSONArray
	 * 
	 * @return
	 * @throws JSONException
	 */
	public JSONArray dataToJSONArray() throws JSONException {
		JSONArray jsonRows = new JSONArray();
		// for every row add its definition
		if(this.cfcData != null) {
			for (CFCRow cfcRow : this.cfcData) {
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
	
	public JSONObject getColumn0Definition() throws JSONException {
		JSONObject column0 = new JSONObject();
		
		column0.put("header", "Code");
		column0.put("width", 100);
		column0.put("sortable", true);
		column0.put("dataIndex", "code");
		column0.put("hideable", true);
		column0.put("hidden", true);
		column0.put("locked", true);
		
		return column0;
	}
	
	public JSONObject getColumn1Definition() throws JSONException {
		JSONObject column1 = new JSONObject();
		
		column1.put("header", "Date");
		column1.put("width", 100);
		column1.put("sortable", true);
		column1.put("dataIndex", "date");
		column1.put("hideable", false);
		column1.put("hidden", false);
		column1.put("locked", true);
		
		return column1;
	}
	
	public JSONObject getColumnDefinition(CFCColumn cfcColumn) throws JSONException {
		JSONObject jsonColumn = new JSONObject();
		
		String valuePropertyName = "value" + "_" + cfcColumn.getColIndex();
		
		jsonColumn.put("header", cfcColumn.getName());
		jsonColumn.put("dataIndex", valuePropertyName);
		jsonColumn.put("width", 80);
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
	
	protected JSONObject superToJSONObject() throws JSONException {
		return super.toJSONObject();
	}
}
