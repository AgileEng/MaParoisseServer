package eu.agileeng.domain.acc.cashbasis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEList;

public class BordereauParoissesList extends ArrayList<BordereauParoisse> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3899336086616880252L;
	
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (BordereauParoisse item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			BordereauParoisse bp = new BordereauParoisse();
			bp.create(jsonItem);
			
			add(bp);
		}
	}
	
	public void setYear(int year) {
		for (BordereauParoisse bp : this) {
			bp.setYear(year);
		}
	}
	
	public void setCompany(AEDescriptor company) {
		for (BordereauParoisse bp : this) {
			bp.setCompany(company);
		}
	}
	
	public BordereauParoisse getByCode(String code) {
		BordereauParoisse res = null;
		for (Iterator<BordereauParoisse> iterator = this.iterator(); iterator.hasNext();) {
			BordereauParoisse _next = (BordereauParoisse) iterator.next();
			if(code.equalsIgnoreCase(_next.getCode())) {
				res = _next;
				break;
			}
		}
		return res;
	}
	
	public Map<String, BordereauParoisse> toMap() {
		Map<String, BordereauParoisse> map = new HashMap<String, BordereauParoisse>(this.size());
		for (BordereauParoisse bp : this) {
			map.put(bp.getCode(), bp);
		}
		return map;
	}
	
//	public static BordereauParoissesList getAll() {
//		BordereauParoissesList all = new BordereauParoissesList();
//		all.add(new BordereauParoisse("Missions d’Afrique", "07"));
//		all.add(new BordereauParoisse("Sainte Enfance", "11"));
//		all.add(new BordereauParoisse("Grande Quête Diocésaine", "01"));
//		all.add(new BordereauParoisse("Terre Sainte", "09"));
//		all.add(new BordereauParoisse("Denier de St Pierre", "08"));
//		all.add(new BordereauParoisse("Communication diocésaine – Alsace Media", "13"));
//		all.add(new BordereauParoisse("Apostolat des Laïcs et Catéchèse", "03"));
//		all.add(new BordereauParoisse("Dimanche des Missions", "06"));
//		all.add(new BordereauParoisse("Propagation de la Foi", "10"));
//		all.add(new BordereauParoisse("Saint Pierre Apôtre", "12"));
//		all.add(new BordereauParoisse("Liturgie, Musique et Art Sacrés", "24"));
//		all.add(new BordereauParoisse("Taxes mariages et enterrements (16,- €), confirmations, dispenses…", "14"));
//		all.add(new BordereauParoisse("2% sur les revenus de la Fabrique d’église de l’année dernière", "15"));
//		all.add(new BordereauParoisse("20% sur quêtes de mariages et enterrements réalisés au cours de l’année courante", "16"));
//		all.add(new BordereauParoisse("Décorations (diocésaines ou romaines)", "18"));
//		all.add(new BordereauParoisse("Binages versés par la paroisse", "19"));
//		all.add(new BordereauParoisse("Prélèvements sur messes versés par la paroisse (2,- €)", "20"));
//		all.add(new BordereauParoisse("Honoraires de messes à faire célébrer", "22"));
//		all.add(new BordereauParoisse("Pastorale des Jeunes, …", "17"));
//		return all;
//	}
}
