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
package com.blackducksoftware.tools.commonframework.standard.codecenter;

import com.blackducksoftware.sdk.codecenter.application.ApplicationApi;
import com.blackducksoftware.sdk.codecenter.attribute.AttributeApi;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV6_6_0;
import com.blackducksoftware.sdk.codecenter.cola.ColaApi;
import com.blackducksoftware.sdk.codecenter.request.RequestApi;
import com.blackducksoftware.sdk.codecenter.settings.SettingsApi;
import com.blackducksoftware.sdk.codecenter.user.UserApi;
import com.blackducksoftware.sdk.codecenter.vulnerability.VulnerabilityApi;
import com.blackducksoftware.tools.commonframework.connector.protex.APIWrapper;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;


/**
 * The Class CodeCenterAPIWrapper.
 */
public class CodeCenterAPIWrapper extends APIWrapper {


    /** The cc proxy. */
    private CodeCenterServerProxyV6_6_0 ccProxy;

    /** The vulnerability api. */
    private VulnerabilityApi vulnerabilityApi;

    /** The application api. */
    private ApplicationApi applicationApi;

    /** The user api. */
    private UserApi userApi;

    /** The cola api. */
    private ColaApi colaApi;

    /** The attribute api. */
    private AttributeApi attributeApi;

    private RequestApi requestApi;

    private SettingsApi settingsApi;

    public CodeCenterAPIWrapper(ServerBean bean, ConfigurationManager manager)
	    throws Exception {
	super(manager);
	getAllApis(bean.getServerName(), bean.getUserName(), bean.getPassword());
    }

    /**
     * Instantiates a new code center api wrapper.
     *
     * @param configManager
     *            the config manager
     * @throws Exception
     *             the exception
     */

    public CodeCenterAPIWrapper(ConfigurationManager configManager)
	    throws Exception {
	super(configManager);
	getAllApis(configManager.getServerBean().getServerName(), configManager
		.getServerBean().getUserName(), configManager.getServerBean()
		.getPassword());

    }

    private void getAllApis(String server, String user, String password)
	    throws Exception {
	String errorMessage = "";
	try {
	    ccProxy = new CodeCenterServerProxyV6_6_0(server, user, password);

	    vulnerabilityApi = super.disableCertificateCheck(
		    ccProxy.getVulnerabilityApi(), VulnerabilityApi.class);
	    applicationApi = super.disableCertificateCheck(
		    ccProxy.getApplicationApi(), ApplicationApi.class);
	    userApi = super.disableCertificateCheck(ccProxy.getUserApi(),
		    UserApi.class);
	    colaApi = super.disableCertificateCheck(ccProxy.getColaApi(),
		    ColaApi.class);
	    attributeApi = super.disableCertificateCheck(
		    ccProxy.getAttributeApi(), AttributeApi.class);
	    requestApi = super.disableCertificateCheck(ccProxy.getRequestApi(),
		    RequestApi.class);
	    settingsApi = super.disableCertificateCheck(
		    ccProxy.getSettingsApi(), SettingsApi.class);

	} catch (Exception e) {
	    errorMessage = e.getMessage();
	    if (e.getCause() != null) {
		errorMessage += ": " + e.getCause().getMessage();
	    }
	    throw new Exception(errorMessage);
	}
    }

    public CodeCenterServerProxyV6_6_0 getProxy() {
	return ccProxy;
    }

    public VulnerabilityApi getVulnerabilityApi() {
	return vulnerabilityApi;
    }

    public ApplicationApi getApplicationApi() {
	return applicationApi;
    }

    public UserApi getUserApi() {
	return userApi;
    }

    public ColaApi getColaApi() {
	return colaApi;
    }

    public AttributeApi getAttributeApi() {
	return attributeApi;
    }

    public RequestApi getRequestApi() {
	return requestApi;
    }

    public SettingsApi getSettingsApi() {
	return settingsApi;
    }

}
