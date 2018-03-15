package eu.agileeng.domain.inventory;

import java.util.Date;

import eu.agileeng.domain.cash.CFCRow;

public class InventoryRow extends CFCRow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7591654589610897768L;

	static public enum Type {
		NA(0),
		A(10),
		B(20),
		C(30),
		D(40),
		E(50),
		F(60),
		G(70),
		H(80),
		I(90),
		J(100),
		K(110),
		FREE_1(201),
		FREE_2(202),
		FREE_3(203),
		FREE_4(204),
		FREE_5(205),
		FREE_6(206),
		FREE_SUM(300),
		A_ANNUALLY(310),
		B_ANNUALLY(320),
		C_ANNUALLY(330),
		D_ANNUALLY(340),
		E_ANNUALLY(350),
		F_ANNUALLY(360),
		G_ANNUALLY(370),
		H_ANNUALLY(380),
		I_ANNUALLY(390),
		J_ANNUALLY(400),
		K_ANNUALLY(410);

		private long sysId;
		
		private Type(long sysId) {
			this.sysId = sysId;
		}
		
		public final long getSysId() {
			return this.sysId;
		}
		
		public static Type valueOf(long sysId) {
			Type found = NA;
			for (Type type : Type.values()) {
				if(type.getSysId() == sysId) {
					found = type;
					break;
				}
			}
			return found;
		}
	}
	
	private String description;
	
	/**
	 * 
	 */
	public InventoryRow(Date date, int index) {
		super(date, index);
	}
	
	/**
	 * 
	 */
	public InventoryRow(long sysId) {
		super(sysId);
	}

	public InventoryRow(InventoryRow.Type type) {
		super(type.getSysId());
		switch(type) {
			case A: {
				this.description = "Stocks jaugés début mois";
				break;
			}
			case B: {
				this.description = "Livraisons du mois";
				break;
			}
			case C: {
				this.description = "Ventes du mois";
				break;
			}
			case D: {
				this.description = "Stock final comptable du mois";
				break;
			}
			case E: {
				this.description = "Stocks jaugés fin de mois";
				break;
			}
			case F: {
				this.description = "Ecart du mois";
				break;
			}
			case G: {
				this.description = "Ecart cumulés mois précédent";
				break;
			}
			case H: {
				this.description = "Nouveaux écarts cumulés";
				break;
			}
			case I: {
				this.description = "Ecarts cumulés Etat de Stock";
				break;
			}
			case J: {
				this.description = "Différence";
				break;
			}
			case K: {
				this.description = "Ecart en 0/00";
				break;
			}
			case FREE_SUM: {
				this.description = "Somme";
				break;
			}
			case A_ANNUALLY: {
				this.description = "Stocks jaugés début période";
				break;
			}
			case B_ANNUALLY: {
				this.description = "Livraisons";
				break;
			}
			case C_ANNUALLY: {
				this.description = "Ventes";
				break;
			}
			case D_ANNUALLY: {
				this.description = "Stock final";
				break;
			}
			case E_ANNUALLY: {
				this.description = "Stocks jaugés fin de période";
				break;
			}
			case F_ANNUALLY: {
				this.description = "Ecart";
				break;
			}
			case G_ANNUALLY: {
				this.description = "Ecart cumulés mois précédent";
				break;
			}
			case H_ANNUALLY: {
				this.description = "Nouveaux écarts cumulés";
				break;
			}
			case I_ANNUALLY: {
				this.description = "Ecarts cumulés Etat de Stock";
				break;
			}
			case J_ANNUALLY: {
				this.description = "Différence";
				break;
			}
			case K_ANNUALLY: {
				this.description = "Ecart en 0/00";
				break;
			}
		}
	}
	
	/**
	 * 
	 */
	public InventoryRow() {
		super();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
