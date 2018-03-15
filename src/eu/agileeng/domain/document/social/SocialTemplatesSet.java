package eu.agileeng.domain.document.social;

public enum SocialTemplatesSet {
	NA(0, "N/A"),
	ROC_SGAR_SG2P(10, "ROC, SGAR, SG2P"),
	SET_2(20, "Templates Set 2"),
	SET_HRC(30, "contrats HRC");

	private int id;
	
	private String code;

	private SocialTemplatesSet(int id, String code) {
		this.id = id;
		this.code = code;
	}

	public final int getId() {
		return this.id;
	}
	
	public final String getCode() {
		return this.code;
	}

	public static SocialTemplatesSet valueOf(int id) {
		SocialTemplatesSet ret = null;
		for (SocialTemplatesSet inst : SocialTemplatesSet.values()) {
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
	
	public static boolean isDefined(SocialTemplatesSet inst) {
		return inst != null && !SocialTemplatesSet.NA.equals(inst);
	}
}