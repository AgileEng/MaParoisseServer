package eu.agileeng.services.timer;

import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.security.AuthLoginToken;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthService;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.ServiceLocator;
import eu.agileeng.services.ServiceLocator.Services;
import eu.agileeng.services.imp.AEInvocationContextImp;
import eu.agileeng.services.social.AESocialService;


// @Singleton
public class EliorFtpTransferJob {

	private static Logger logger = Logger.getLogger(EliorFtpTransferJob.class);
	
	// String cronSchedule = "0 5 4 ? * *"; 
	// triger that fires at 4:05am every day
    // @Schedule(persistent=false, second="0", minute="5", hour="4")
    public void execute() {
		try {
			logger.info("EliorFtpTransfer job execution start");
			
			// login
			AuthService authService = (AuthService) ServiceLocator.getInstance().getService(Services.AUTH);
			AuthLoginToken authToken = AEApp.getInstance().getSystemToken();
			AuthPrincipal authPrincipal = authService.login(authToken);
			AEInvocationContext invContext = new AEInvocationContextImp(authPrincipal);
			
			// execute the task
			AESocialService socialService = 
				(AESocialService) ServiceLocator.getInstance().getService(Services.SOCIAl);
			socialService.eliorFtpTransfer(invContext);
			
			logger.info("EliorFtpTransfer job execution succeeded");
		} catch (Exception e) {
			logger.error("EliorFtpTransfer job execution failed. ", e);
		}
    }
}
