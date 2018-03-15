package eu.agileeng.domain.facturation;

import eu.agileeng.util.AEStringUtil;

public class AEFacturePrintTemplateView {
	private AEFacturePrintTemplate printTemplate;
	private long issuer = 0x00;
	private long body = 0x00;
	private long client = 0x00;
	
	public AEFacturePrintTemplateView(AEFacturePrintTemplate template) {
		this.printTemplate = template;
		this.issuer = template.getIssuer();
		this.body = template.getBody();
		this.client = template.getClient();
	}
	
	public boolean getBodyProp(int index) {
		long mask = (1 << index);
		return ((this.body & mask) == mask);
	}
	
	public boolean getHeaderProp(int index) {
		long mask = (1 << index);
		return ((this.issuer & mask) == mask);
	}
	
	public boolean getFooterProp(int index) {
		long mask = (1 << index);
		return ((this.client & mask) == mask);
	}
	
	public String getModelDescription() {
		String description = AEStringUtil.EMPTY_STRING;
		if (this.printTemplate != null && !AEStringUtil.isEmpty(this.printTemplate.getDescription())) {
			description = this.printTemplate.getDescription();
		}
		return description;
	}
}
