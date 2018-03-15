package eu.agileeng.domain.document.social;

public enum SocialPeriodType {
	NA(0, "N/A"),
	TEMPLATE(5, "T"),
	PLAN(10, "P"),
	ACTUAL(15, "R");

	private int id;
	
	private String code;

	private SocialPeriodType(int id, String code) {
		this.id = id;
		this.code = code;
	}

	public final int getId() {
		return this.id;
	}
	
	public final String getCode() {
		return this.code;
	}

	public static SocialPeriodType valueOf(int id) {
		SocialPeriodType ret = null;
		for (SocialPeriodType inst : SocialPeriodType.values()) {
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
}