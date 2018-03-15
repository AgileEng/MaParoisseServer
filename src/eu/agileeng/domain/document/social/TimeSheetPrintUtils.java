package eu.agileeng.domain.document.social;

import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;

public class TimeSheetPrintUtils {
	public static String[] daysInFrench = {
			"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"
	};
	public static String[] daysInFrenchStartWithCapital = {
		"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
	};
	public static String printTimeSheetHours(SocialTimeSheet timeSheet) {
		String strBody = "";
		Map<Integer, List<SocialTimeSheetEntry>> weeks = timeSheet.toWeeks();
		Set<Integer> weeksSet = weeks.keySet();
		strBody += "<p><ul style='list-style: none;'>";
		for (Integer weekNumber : weeksSet) {
			List<SocialTimeSheetEntry> week = weeks.get(weekNumber);
			String hoursInWeek = AEMath.toPrice2String(SocialTimeSheet.getPlanHours(week));
			strBody += "<li>"+hoursInWeek+" heures la semaine "+weekNumber+"</li><li><ul>";
			for (SocialTimeSheetEntry entry : week) {
				if (entry.getWorkingHours() != null && entry.getWorkingHours() > 0 && entry.isReal()) {
					strBody += "<li>" + entry.getWorkingHours() + " heures, le " + 
					daysInFrench[entry.getDayOfWeek()-1];
					boolean hasTime1 = false;
					if (entry.getFromTime_1() != null) {
						hasTime1 = true;
						strBody += " de " + 
						AEDateUtil.formatTimeToSystem(entry.getFromTime_1()) + "h а " + 
						AEDateUtil.formatTimeToSystem(entry.getToTime_1()) + "h";
					}
					if (entry.getFromTime_2() != null) {
						strBody += (hasTime1 ? "," : "") + " et de " + 
						AEDateUtil.formatTimeToSystem(entry.getFromTime_2()) + "h а " + 
						AEDateUtil.formatTimeToSystem(entry.getToTime_2()) + "h";
					}
					strBody += "</li>";
				}
			}
			strBody += "</ul></li>";
		}
		
		return strBody+"</ul></p>";
	}
	
	public static String printTimeSheetHoursHRC(SocialTimeSheet timeSheet) {
		StringBuilder strBody = new StringBuilder();
		strBody.append("");
		Map<Integer, List<SocialTimeSheetEntry>> weeks = timeSheet.toWeeks();
		Set<Integer> weeksSet = weeks.keySet();
		for (Integer weekNumber : weeksSet) {
			List<SocialTimeSheetEntry> week = weeks.get(weekNumber);
			String hoursInWeek = AEMath.toPrice2String(SocialTimeSheet.getPlanHours(week));
			strBody.append("<p class=3DMsoNormal style=3D'margin-right:16.75pt;text-align:justify;mso-layout-grid-align:none;text-autospace:none'><span lang=3DFR style=3D'font-size:10.0pt;font-family:\"Calibri\",\"sans-serif\";mso-ascii-theme-font:minor-latin;mso-hansi-theme-font:minor-latin;mso-bidi-font-family:\"Lucida Sans Unicode\";color:black;mso-bidi-font-weight:bold;page-break-after:auto'>");
			strBody.append("Semaine " + weekNumber);
			strBody.append("&nbsp;:<o:p></o:p></span></p>");
			strBody.append("<p class=3DMsoNormal style=3D'margin-right:16.75pt;text-align:justify;mso-layout-grid-align:none;text-autospace:none'><i><span lang=3DFR style=3D'font-size:10.0pt;font-family:\"Calibri\",\"sans-serif\";mso-ascii-theme-font:minor-latin;mso-hansi-theme-font:minor-latin;mso-bidi-font-weight:bold'><span style=3D'mso-spacerun:yes'>&nbsp;</span></span></i>");
			strBody.append("<span lang=3DFR style=3D'font-size:10.0pt;font-family:\"Calibri\",\"sans-serif\";mso-ascii-theme-font:minor-latin;mso-hansi-theme-font:minor-latin;mso-bidi-font-family:\"Lucida Sans Unicode\";color:black;mso-bidi-font-weight:bold'><o:p></o:p></span></p>");
			strBody.append("<p class=3DMsoBlockText style=3D'margin-left:0cm;mso-pagination:lines-together'><span lang=3DFR style=3D'font-size:10.0pt;font-family:\"Calibri\",\"sans-serif\";mso-ascii-theme-font:minor-latin;mso-hansi-theme-font:minor-latin;mso-bidi-font-weight:bold'>");
			for (SocialTimeSheetEntry entry : week) {
				if (entry.getWorkingHours() != null && entry.getWorkingHours() > 0 && entry.isReal()) {
					strBody.append(daysInFrenchStartWithCapital[entry.getDayOfWeek()-1]);
					boolean hasTime1 = false;
					if (entry.getFromTime_1() != null) {
						hasTime1 = true;
						strBody.append(" de "); 
						strBody.append(AEDateUtil.formatTimeToSystem(entry.getFromTime_1()));
						strBody.append(" à ");
						strBody.append(AEDateUtil.formatTimeToSystem(entry.getToTime_1()));
					}
					if (entry.getFromTime_2() != null) {
						//strBody.append(hasTime1 ? "," : "");
						strBody.append(" et de ");
						strBody.append(AEDateUtil.formatTimeToSystem(entry.getFromTime_2()));
						strBody.append(" à ");
						strBody.append(AEDateUtil.formatTimeToSystem(entry.getToTime_2()));
					}
					strBody.append(", soit ");
					strBody.append(entry.getWorkingHours());
					strBody.append(" heures; ");
				}
			}
			strBody.append("</span></p>");
			strBody.append("<p class=3DMsoBlockText style=3D'margin-left:0cm;page-break-after:avoid'><span lang=3DFR style=3D'font-size:10.0pt;font-family:\"Calibri\",\"sans-serif\";mso-ascii-theme-font:minor-latin;mso-hansi-theme-font:minor-latin;mso-bidi-font-weight:bold'><o:p>&nbsp;</o:p></span></p>");
		}
		
		return strBody.toString();
	}
	
	public static String printTimeSheetDays(SocialTimeSheet timeSheet) {
		String strBody = "";
		
		for (SocialTimeSheetEntry entry : timeSheet) {
			strBody += "<li>" + entry + "</li>";
		}
		
		return "<p><ul>"+strBody+"</ul></p>";
	}
	
	public static String printTimeSheetWeeks(SocialTimeSheet timeSheet) {
		String strBody = "";
		
		for (SocialTimeSheetEntry entry : timeSheet) {
			strBody += "<li>" + entry + "</li>";
		}
		
		return "<p><ul>"+strBody+"</ul></p>";
	}
}
