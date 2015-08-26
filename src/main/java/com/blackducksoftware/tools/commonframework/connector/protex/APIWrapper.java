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

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.http.HTTPConduit;

import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;

// TODO: Auto-generated Javadoc
/**
 * Standard SDK wrapper.
 *
 * @author akamen
 */
public abstract class APIWrapper {

    /** The https string. */
    private static String HTTPS_STRING = "https://";

    /** The http string. */
    private static String HTTP_STRING = "http";

    private TLSClientParameters tlsClientParameters;

    /**
     * Instantiates a new API wrapper.
     *
     * @param configManager
     *            the config manager
     */
    public APIWrapper(ConfigurationManager configManager) {
	setupProxyInformation(configManager);
	determineServerURL(configManager);

	// For cert bypass
	tlsClientParameters = new TLSClientParameters();
	tlsClientParameters.setDisableCNCheck(true);
    }

    /**
     * This wraps the api in a HTTPconduit and sets the switch to disable the
     * certificate check Useful for those clients where the javacerts are out of
     * date/wrong/etc.
     *
     * @param api
     * @param theClass
     * @return
     */
    public <T> T disableCertificateCheck(T api, Class<T> theClass) {
	org.apache.cxf.endpoint.Client client = org.apache.cxf.frontend.ClientProxy
		.getClient(api);
	HTTPConduit http = (HTTPConduit) client.getConduit();
	http.setTlsClientParameters(tlsClientParameters);

	return api;
    }

    /**
     * Determine server url.
     *
     * @param configManager
     *            the config manager
     */
    protected void determineServerURL(ConfigurationManager configManager) {
	// AK: This allows a bit more flexibility on the server URLs. If the
	// user manually specifies an HTTP(S) address, then we allow it.
	// Otherwise, everything else is prepended with an HTTPS
	String serverURL = null;

	String serverName = configManager.getServerBean().getServerName();

	if (serverName.contains(HTTP_STRING)) {
	    serverURL = serverName;
	} else {
	    serverURL = HTTPS_STRING + serverName;
	}

	configManager.setServerURL(serverURL);
    }

    /**
     * Sets up the System property for proxy information.
     *
     * @param configManager
     *            the new up proxy information
     */
    private void setupProxyInformation(ConfigurationManager configManager) {
	String proxyServer = configManager.getProxyServer();
	String proxyPort = configManager.getProxyPort();

	String httpsProxyServer = configManager.getProxyServerHttps();
	String httpsProxyPort = configManager.getProxyPortHttps();

	// TODO: Intelligently determine whether http or https.
	// Only do something if user provided legitimate data
	if (proxyServer != null && proxyPort != null) {
	    System.setProperty("http.proxyHost", proxyServer.trim());
	    System.setProperty("http.proxyPort", proxyPort);
	} else if (httpsProxyServer != null && httpsProxyPort != null) {

	    System.setProperty("https.proxyHost", httpsProxyServer.trim());
	    System.setProperty("https.proxyPort", httpsProxyPort);
	}
    }

}
