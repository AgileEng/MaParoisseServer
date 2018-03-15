/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.06.2010 12:29:34
 */
package eu.agileeng.domain.business;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class ContractDeTravailReason extends EnumeratedType {

//	<option value="0">Choisissez un motif d'absence</option>
//	<option value="Congés payés">Congés payés</option>
//	<option value="Maladie">Maladie</option>
//	<option value="Accident du travail">Accident du travail</option>
//	<option value="Maternité">Maternité</option>
//	<option value="Paternité">Paternité</option>
//	<option value="Congé parental">Congé parental</option>
//	<option value="Congés sans solde">Congés sans solde</option>
//	<option value="Absence injustifiée">Absence injustifiée</option>
//	<option value="Congés évenements familiaux">Congés évenements familiaux</option>
//	<option value="Formation professionnelle continue">Formation professionnelle continue</option>
//	<option value="Mise à pied">Mise à pied</option>
//	<option value="Autres">Autres</option>
	
//	reasons : [{
//		name : 'Congés payés',
//		id : '50'
//	}, {
//		name : 'Maladie',
//		id : '60'
//	}, {
//		name : 'Accident du travail',
//		id : '70'
//	}, {
//		name : 'Congé parental',
//		id : '80'
//	}, {
//		name : 'Maternité',
//		id : '90'
//	}, {
//		name : 'Paternité',
//		id : '100'
//	}, {
//		name : 'Congés sans solde',
//		id : '110'
//	}, {
//		name : 'Absence injustifiée',
//		id : '120'
//	}, {
//		name : 'Congés évenements familiaux',
//		id : '130'
//	}, {
//		name : 'Formation professionnelle continue',
//		id : '140'
//	}, {
//		name : 'Mise à pied',
//		id : '150'
//	}, {
//		name : 'Autres',
//		id : '1'
//	}]
	
	static public enum System {
		NA(0L, "N/A"),
		OTHER(1, "Autres"),
		USER_DEFINED(5, ""),
		_50(50, "Congés payés"),
		_60(60, "Maladie"),
		_70(70, "Accident du travail"),
		_80(80, "Congé parental"),
		_90(90, "Maternité"),
		_100(100, "Paternité"),
		_110(110, "Congés sans solde"),
		_120(120, "Absence injustifiée"),
		_130(130, "Congés évenements familiaux"),
		_140(140, "Formation professionnelle continue"),
		_150(150, "Mise à pied");
		
		private long systemID;
		
		private String descr;
		
		private System(long systemID, String descr) {
			this.systemID = systemID;
			this.descr = descr;
		}
		
		public final long getSystemID() {
			return this.systemID;
		}
		
		public final String getDescr() {
			return this.descr;
		}
		
		public static System valueOf(long systemID) {
			System ret = null;
			for (System inst : System.values()) {
				if(inst.getSystemID() == systemID) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
		
		public static System valueOfDescr(String descr) {
			System ret = null;
			for (System inst : System.values()) {
				if(inst.getDescr().equalsIgnoreCase(descr)) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
	}
	
	/**
	 * @param clazz
	 */
	public ContractDeTravailReason() {
		super(DomainClass.ContractDeTravailReason);
	}
	
	public ContractDeTravailReason(ContractDeTravailReason.System system) {
		this();
		setSystemID(system.getSystemID());
	}
}
