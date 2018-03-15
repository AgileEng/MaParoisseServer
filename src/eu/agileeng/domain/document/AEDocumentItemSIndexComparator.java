package eu.agileeng.domain.document;

import java.util.Comparator;

import eu.agileeng.domain.document.trade.AEDocumentItem;

public class AEDocumentItemSIndexComparator implements Comparator<AEDocumentItem> {

	@Override
	public int compare(AEDocumentItem item1, AEDocumentItem item2) {
		int res = 0;
		if(item1 != null && item2 != null) {
			if(item1.getSequenceNumber() < item2.getSequenceNumber()) {
				res = -1;
			} else if(item1.getSequenceNumber() > item2.getSequenceNumber()) {
				res = 1;
			}
		}
		return res;
	}
}
