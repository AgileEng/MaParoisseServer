/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.06.2010 14:35:08
 */
package eu.agileeng.domain.document.social.contractdetravail;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class ContractDeTravailAssistence extends EnumeratedType {

//	<option value="Aucun">Aucun</option>
//	<option value="CIE">CIE</option>
//	<option value="Apprentissage">Apprentissage</option>
//	
//	<!-- Ajouté par florian le 23.02.07 -->
//	<option value="Contrat de professionnalisation jeune">Contrat de professionnalisation jeune</option>
//	<option value="Contrat de professionnalisation adulte">Contrat de professionnalisation adulte</option>
//	<!-- fin ajout -->
//	
//	<option value="Contrat jeune entreprise">Contrat jeune entreprise</option>
//	<option value="ZRR">ZRR</option>
//	<option value="ZRU">ZRU</option>
//	<option value="ZFU">ZFU</option>
//	
//	<!--Modifié le 23.02.07 par florian
//	<option value="Contrat d'adaptation">Contrat d'adaptation</option>
//	<option value="Contrat d'orientation">Contrat d'orientation</option>-->
//	
//	<option value="CES">CES</option>
//	<option value="CEC">CEC</option>
//	<option value="Emploi-Jeunes">Emploi-Jeunes</option>
//	<option value="ACCRE">ACCRE</option>
//	<option value="Prime handicapé">Prime handicapé</option>
//	<option value="Autres">Autres</option>
	
	static public enum System {
		NA(0L),
		OTHER(1),
		USER_DEFINED(5),
		AUCUN(10L),
		CUI(20l),
		_50(50),
		_60(60),
		_70(70),
		_80(80),
		_90(90),
		_100(100),
		_110(110),
		_120(120),
		_130(130),
		_140(140),
		_150(150),
		_160(160),
		_170(170),
		_180(180),
		_190(190),
		_200(200);
		
		private long systemID;
		
		private System(long systemID) {
			this.systemID = systemID;
		}
		
		public final long getSystemID() {
			return this.systemID;
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
	}
	
	/**
	 * @param clazz
	 */
	public ContractDeTravailAssistence() {
		super(DomainClass.ContractDeTravailAssistence);
	}
	
	public ContractDeTravailAssistence(ContractDeTravailAssistence.System system) {
		this();
		setSystemID(system.getSystemID());
	}
	
	public ContractDeTravailAssistence.System getSystem() {
		return System.valueOf(getSystemID());
	}
}
