/*******************************************************************************
 * Copyright (C) 2015 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.commonframework.connector.protex;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.fault.SdkFaultDetails;
import com.blackducksoftware.sdk.protex.client.util.ProtexServerProxyV6_3;
import com.blackducksoftware.sdk.protex.client.util.ServerAuthenticationException;
import com.blackducksoftware.sdk.protex.component.standard.StandardComponentApi;
import com.blackducksoftware.sdk.protex.component.version.ComponentVersionApi;
import com.blackducksoftware.sdk.protex.license.LicenseApi;
import com.blackducksoftware.sdk.protex.policy.PolicyApi;
import com.blackducksoftware.sdk.protex.project.ProjectApi;
import com.blackducksoftware.sdk.protex.project.bom.BomApi;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeApi;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.DiscoveryApi;
import com.blackducksoftware.sdk.protex.project.codetree.identification.IdentificationApi;
import com.blackducksoftware.sdk.protex.project.localcomponent.LocalComponentApi;
import com.blackducksoftware.sdk.protex.report.ReportApi;
import com.blackducksoftware.sdk.protex.role.RoleApi;
import com.blackducksoftware.sdk.protex.user.UserApi;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;


/**
 * Primary authenticator and validator for the Protex SDKs.
 *
 * @author Ari Kamen
 */
@SuppressWarnings("restriction")
public class ProtexAPIWrapper extends APIWrapper {

    /** The log. */
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    /** The bom api. */
    private BomApi bomApi;

    /** The code tree api. */
    private CodeTreeApi codeTreeApi;

    /** The identification api. */
    private IdentificationApi identificationApi;

    /** The project api. */
    private ProjectApi projectApi;

    /** The discovery api. */
    private DiscoveryApi discoveryApi;

    /** The report api. */
    private ReportApi reportApi;

    /** The standard api. */
    private StandardComponentApi standardApi;

    /** The user api. */
    private UserApi userApi;

    /** The license api. */
    private LicenseApi licenseApi ;

    /** The standard component api. */
    private StandardComponentApi standardComponentApi ;

    /** The role api. */
    private RoleApi roleApi;

    /** The component version api. */
    private ComponentVersionApi componentVersionApi;

    private LocalComponentApi localComponentApi;
    
    private PolicyApi policyApi;

    /** The protex server. */
    private ProtexServerProxyV6_3 protexServer;

    /** The error message. */
    private String errorMessage;

    /**
     * Creates a proxy object with the proper credentials Credentials keyed off
     * the server bean.
     *
     * @param bean
     * @param configManager
     * @param validate
     * @throws Exception
     */
    public ProtexAPIWrapper(ServerBean bean,
	    ConfigurationManager configManager, boolean validate)
	    throws Exception {
	super(configManager);
	getAllApisAndValidate(bean.getServerName(), bean.getUserName(),
		bean.getPassword(), validate, configManager);

    }

    /**
     * Creates a proxy object with proper credentials.
     *
     * @param configManager
     *            the config manager
     * @param validate
     *            Flag to determine whether validation is required
     * @throws Exception
     *             the exception
     */
    protected ProtexAPIWrapper(ConfigurationManager configManager,
	    boolean validate) throws Exception {
	super(configManager);
	getAllApisAndValidate(configManager.getServerBean().getServerName(),
		configManager.getServerBean().getUserName(), configManager
			.getServerBean().getPassword(), validate, configManager);
    }

    /**
     * Handles initializing all APIs and performing user credential validation
     *
     * @param server
     *            the connection server
     * @param user
     *            the user information
     * @param password
     *            the user password
     * @param validate
     *            the validate flag
     * @param configManager
     *            the configuration manager
     * @throws Exception
     *             if error occurs during authentication or API calls
     */
    private void getAllApisAndValidate(String server, String user,
	    String password, boolean validate,
	    ConfigurationManager configManager) throws Exception {
	try {
	    protexServer = new ExtendedProxy(server, user, password,
		    configManager.getChildElementCount());
	    log.info("User Info: " + user);
	    if (validate) {
		validateCredentials(server);
	    }
	    bomApi = protexServer.getBomApi();
	    codeTreeApi = protexServer.getCodeTreeApi();
	    identificationApi = protexServer.getIdentificationApi();
	    projectApi = protexServer.getProjectApi();
	    discoveryApi = protexServer.getDiscoveryApi();
	    reportApi = protexServer.getReportApi();
	    standardApi = protexServer.getStandardComponentApi();
	    userApi = protexServer.getUserApi();
	    licenseApi = protexServer.getLicenseApi();
	    policyApi = protexServer.getPolicyApi();
	    standardComponentApi = protexServer.getStandardComponentApi();
	    componentVersionApi = protexServer.getComponentVersionApi();
	    localComponentApi = protexServer.getLocalComponentApi();

	    // if (validate){
	    // // Use ProtexServerProxyV6_3 validateCredentials() for Protex 7
	    // which is not functional
	    // // for Protex 6 which is why an internal implementation is used
	    // to indirectly check
	    // // if the user is authorized to access the system
	    // if (getProtexVersion().equals(ProtexVersion.PROTEX7)){
	    // protexServer.validateCredentials();
	    // }
	    // else {
	    // validateCredentials();
	    // }
	    // }

	} catch (ServerAuthenticationException sae) {
	    errorMessage = "Unable to log in: " + sae.getMessage();
	    throw new Exception(errorMessage);
	} catch (Exception e) {
	    errorMessage = e.getMessage();
	    if (e.getCause() != null) {
		errorMessage += ": " + e.getCause().getMessage();
	    }
	    throw new Exception(errorMessage);
	}

    }

    /**
     * Grabs one project to check if authentication worked.
     *
     * @param protex
     *            the protex
     * @param manager
     *            the manager
     * @throws Exception
     *             the exception
     */
 
    private void validateCredentials(String serverUrl) throws Exception {
	try {
	    UserApi userAPI = protexServer.getUserApi();
	    boolean currentUserHasServerFileAccess = userAPI
		    .getCurrentUserHasServerFileAccess();
	    log.info("User has server file access: "
		    + currentUserHasServerFileAccess);
	    log.info("User Authenticated");
	} catch (SdkFault sdk) {
	    SdkFaultDetails details = sdk.getFaultInfo();
	    if (details != null) {
		String msg = "Validation Failure: Error code: "
			+ details.getErrorCode() + "; Message: "
			+ details.getMessage();
		throw new Exception(msg);
	    }
	} catch (Exception e) {
	    String msg = e.getMessage();
	    if (e.getCause() != null) {
		msg += "; Caused by: " + e.getCause().getMessage();
	    }
	    // This is a hack; In 7.0 the SDK should throw a more helpful
	    // message when the SDK license is absent,
	    // but for now, this seems to be about the best we can do in terms
	    // of getting a helpful msg to the user. -- Steve Billings
	    if (e instanceof SOAPFaultException) {
		msg += ". Please check that the Protex server has the SDK license enabled.";
	    } else if (e instanceof WebServiceException) {
		msg += ". Please check that the server URL (" + serverUrl
			+ ") is correct.";
	    }
	    throw new Exception(msg);
	}
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getErrorMessage() {
	return errorMessage;
    }

    /**
     * Retrieves the interal proxy object
     *
     * @return
     */
    public ProtexServerProxyV6_3 getProxy() {
	return protexServer;
    }

    public BomApi getBomApi() {
	return bomApi;
    }

    public CodeTreeApi getCodeTreeApi() {
	return codeTreeApi;
    }

    public IdentificationApi getIdentificationApi() {
	return identificationApi;
    }

    public ProjectApi getProjectApi() {
	return projectApi;
    }

    public DiscoveryApi getDiscoveryApi() {
	return discoveryApi;
    }

    public ReportApi getReportApi() {
	return reportApi;
    }

    public StandardComponentApi getStandardApi() {
	return standardApi;
    }

    public UserApi getUserApi() {
	return userApi;
    }

    public LicenseApi getLicenseApi() {
	return licenseApi;
    }

    public StandardComponentApi getStandardComponentApi() {
	return standardComponentApi;
    }

    public RoleApi getRoleApi() {
	return roleApi;
    }

    public ComponentVersionApi getComponentVersionApi() {
	return componentVersionApi;
    }

    public LocalComponentApi getLocalComponentApi() {
	return localComponentApi;
    }

    public PolicyApi getPolicyApi() {
	return policyApi;
    }
}
