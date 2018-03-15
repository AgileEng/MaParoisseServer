/**
 * 
 */
package eu.agileeng.services.social;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.social.SocialTimeSheet;
import eu.agileeng.domain.document.social.SocialTimeSheetEntry;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

/**
 * @author vvatov
 *
 */
public interface AESocialService extends AEService {
	public AEResponse save(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse load(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse delete(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadEmployees(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse print(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse saveSalaryGrid(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadSalaryGridsAllLazy(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadSalaryGrid(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse calculateSalary(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadSocialInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadActualAttestation(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse saveTimeSheet(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadTimeSheet(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse pushWeek(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse popWeek(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse generateSchedule(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse closePeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse openPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadTimeSheetInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadTimeSheetActualInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadPrintTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse sendEMail(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse transferFromMonBureau(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public SocialTimeSheet loadTemplateTimeSheet(long employeeId, AEInvocationContext invContext) throws AEException;
	public void eliorFtpTransfer(AEInvocationContext invContext) throws AEException;
	public AEResponse loadUniqEmployees(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse saveEmployeeFtpId(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse generateEmplFtpId(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadDailyPlanning(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse reportPeriodPlanning(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public SocialTimeSheet reportPeriodActual(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse importSchedule(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse importScheduleExt(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse updateTimeSheetEntryActual(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse updateDailyTimeSheetActual(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
}
