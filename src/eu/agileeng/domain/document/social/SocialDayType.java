package eu.agileeng.domain.document.social;

public enum SocialDayType {
	NA(0, "N/A", "Not Available"),
	WORK(10, "Travail", "Travail"),
	BREAK(15, "B", "Break"),

	HA(100, "HA", "Heures Avenant"),
	CP(110, "CP", "Congé Payés"),
	CEF(120, "CEF", "Congé évènement familial"),
	ANR(130, "ANR", "Absence Non Rémunérée"),
	AM(140, "AM", "Absence Maladie"),
	AAT(150, "AAT", "Absence Accident du Travail"),
	ACSS(160, "ACSS", "Absence Congés Sans Solde"),
	RTT(170, "RTT", "Réduction du Temps de Travail"),
	CM(180, "CM", "Congé de Maternité/Paternité"),
	CPE(190, "CPE", "Congé Parental d'Education "),
	JF(200, "JF", "Jour Férié non travaillé");
	
	private int id;
	
	private String code;
	
	private String name;

	private SocialDayType(int id, String code, String name) {
		this.id = id;
		this.code = code;
		this.name = name;
	}

	public final int getId() {
		return this.id;
	}
	
	public final String getCode() {
		return this.code;
	}

	public static SocialDayType valueOf(int id) {
		SocialDayType ret = null;
		for (SocialDayType inst : SocialDayType.values()) {
			if(inst.getId() == id) {
				ret = inst;
				break;
			}
		}
		if(ret == null) {
			ret = NA;
		}
		return ret;
	}

	public String getName() {
		return name;
	}
}