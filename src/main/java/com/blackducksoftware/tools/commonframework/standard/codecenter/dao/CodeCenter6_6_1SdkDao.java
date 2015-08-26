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
package com.blackducksoftware.tools.commonframework.standard.codecenter.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.ApplicationApi;
import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.codecenter.attribute.AttributeApi;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeIdToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeNameToken;
import com.blackducksoftware.sdk.codecenter.cola.ColaApi;
import com.blackducksoftware.sdk.codecenter.cola.data.Component;
import com.blackducksoftware.sdk.codecenter.cola.data.ComponentIdToken;
import com.blackducksoftware.sdk.codecenter.cola.data.KbComponentIdToken;
import com.blackducksoftware.sdk.codecenter.cola.data.KbComponentReleaseIdToken;
import com.blackducksoftware.sdk.codecenter.cola.data.KbComponentReleaseNameVersionOrIdToken;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.codecenter.request.data.RequestSummary;
import com.blackducksoftware.sdk.codecenter.vulnerability.VulnerabilityApi;
import com.blackducksoftware.sdk.codecenter.vulnerability.data.VulnerabilityPageFilter;
import com.blackducksoftware.sdk.codecenter.vulnerability.data.VulnerabilitySummary;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ApplicationPojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ApplicationPojoImpl;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentPojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentPojoImpl;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentUsePojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentUsePojoImpl;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.VulnerabilityPojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.VulnerabilityPojoImpl;

public class CodeCenter6_6_1SdkDao {
    private static final int EST_NUM_COMPONENTS_PER_APP = 30;
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private ApplicationApi applicationApi;
    private ColaApi colaApi;
    private VulnerabilityApi vulnApi;
    private AttributeApi attrApi;
    private int applicationsLastRowIndex = Integer.MAX_VALUE;
    private int vulnerabilityLastRowIndex = Integer.MAX_VALUE;
    private Map<ApplicationPojo, Application> appCache; // cache, so we don't
							// have to keep going
							// back to protex
    private Map<String, Component> compCache; // cache, so we don't have to keep
					      // going back to protex
    private Map<ComponentUsePojo, RequestSummary> compUseCache;
    private boolean skipNonKbComponents = true;

    // Attribute IDs for the attributes we'll need to collect values for
    private Map<String, String> appAttrNameIdMap = new HashMap<String, String>(
	    8);

    /**
     * Use this constructor if the CodeCenterServerWrapper has already been
     * initialized, there is no need to read a custom attribute list from the
     * config file, and you're going to use this DAO for a single application.
     *
     * @param ccServerWrapper
     * @throws Exception
     */
    public CodeCenter6_6_1SdkDao(CodeCenterServerWrapper ccServerWrapper)
	    throws SdkFault {
	initCaches(1);
	initCodeCenterApis(ccServerWrapper);
    }

    /**
     * Use this constructor if the CodeCenterServerWrapper has not yet been
     * initialized.
     *
     * @param config
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public CodeCenter6_6_1SdkDao(CodeCenterDaoConfigManager config)
	    throws Exception {
	ServerBean serverBean = config.getServerBean();
	initCodeCenterApis(serverBean, (ConfigurationManager) config);
	initCaches(config.getEstNumApps());

	// Collect the attr IDs for the custom attributes we will need
	for (String attrName : config.getApplicationAttributeNames()) {
	    AttributeNameToken attrNameObj = new AttributeNameToken();
	    attrNameObj.setName(attrName);
	    String attrId = attrApi.getAttribute(attrNameObj).getId().getId();
	    appAttrNameIdMap.put(attrName, attrId);
	}
    }

    public void setSkipNonKbComponents(boolean skipNonKbComponents) {
	this.skipNonKbComponents = skipNonKbComponents;
    }

    private void initCaches(int estNumApps) {
	appCache = new HashMap<ApplicationPojo, Application>(estNumApps);
	compUseCache = new HashMap<ComponentUsePojo, RequestSummary>(estNumApps
		* EST_NUM_COMPONENTS_PER_APP);
	compCache = new HashMap<String, Component>(estNumApps
		* EST_NUM_COMPONENTS_PER_APP);
    }

    /**
     * This constructor is only useful for unit testing
     */
    CodeCenter6_6_1SdkDao() {
	initCaches(5);
    }

    public void limitNumberOfApplications(int maxNumberOfApplications) {
	applicationsLastRowIndex = maxNumberOfApplications;
    }

    public List<ApplicationPojo> getApplications() throws Exception {

	ApplicationPageFilter filter = new ApplicationPageFilter();
	filter.setFirstRowIndex(0);
	filter.setLastRowIndex(applicationsLastRowIndex);
	List<Application> applications = applicationApi.searchApplications("",
		filter);

	List<ApplicationPojo> appPojos = new ArrayList<ApplicationPojo>(
		applications.size());
	for (Application app : applications) {
	    log.debug("Application name: " + app.getName());

	    Map<String, String> appAttrNameValueMap = new HashMap<String, String>(
		    8);
	    collectCustomAttrs(appAttrNameValueMap, app);

	    ApplicationPojo appPojo = new ApplicationPojoImpl(app.getId()
		    .getId(), app.getName(), app.getVersion(),
		    app.getDescription(), appAttrNameValueMap);
	    appPojos.add(appPojo);
	    appCache.put(appPojo, app); // cache it

	    log.debug("Application ID: " + app.getId().getId());
	    log.debug("Application Component ID: "
		    + app.getApplicationComponentId().getId());
	}
	return appPojos;
    }

    public ApplicationPojo getApplication(String appName, String version)
	    throws Exception {

	ApplicationPojo appPojo = null;
	ApplicationNameVersionToken token = new ApplicationNameVersionToken();
	token.setName(appName);
	token.setVersion(version);
	Application app = applicationApi.getApplication(token);

	Map<String, String> appAttrNameValueMap = new HashMap<String, String>(8);
	collectCustomAttrs(appAttrNameValueMap, app);

	appPojo = new ApplicationPojoImpl(app.getId().getId(), app.getName(),
		app.getVersion(), app.getDescription(), appAttrNameValueMap);
	appCache.put(appPojo, app); // cache it

	log.debug("Application ID: " + app.getId().getId());
	log.debug("Application Component ID: "
		+ app.getApplicationComponentId().getId());

	return appPojo;
    }

    private Application getApplicationById(String id) throws SdkFault {
	ApplicationIdToken idToken = new ApplicationIdToken();
	idToken.setId(id);
	return applicationApi.getApplication(idToken);
    }

    public List<ComponentUsePojo> getComponentUses(ApplicationPojo appPojo)
	    throws SdkFault {
	Application app = appCache.get(appPojo); // get the Code Center app from
						 // the cache

	if (app == null) {
	    app = getApplicationById(appPojo.getId());
	}
	String applicationName = app.getName();

	log.debug("Getting requests for app " + app.getName());
	List<RequestSummary> requests = applicationApi
		.getApplicationRequests(app.getId());
	List<ComponentUsePojo> compUsePojos = new ArrayList<ComponentUsePojo>(
		requests.size());
	for (RequestSummary request : requests) {

	    if (skipNonKbComponents) {
		// Peek ahead at the component: Is comp in KB? If not, skip
		log.debug("Looking up catalog component for app");
		ComponentIdToken componentIdToken = request.getComponentId();
		Component component = colaApi
			.getCatalogComponent(componentIdToken);
		KbComponentReleaseIdToken kbComponentReleaseIdToken1 = component
			.getKbReleaseId();
		if (kbComponentReleaseIdToken1 == null) {
		    log.debug("\tSkipping " + applicationName + ": "
			    + component.getName() + " "
			    + component.getVersion()
			    + "; The KB Component Release ID token is null");
		    continue;
		}
		log.debug("Found catalog component for app");
	    }

	    ComponentUsePojo compUsePojo = new ComponentUsePojoImpl(request
		    .getId().getId());
	    compUsePojos.add(compUsePojo);
	    compUseCache.put(compUsePojo, request);
	}
	return compUsePojos;
    }

    public ComponentPojo getComponent(ComponentUsePojo compUsePojo)
	    throws SdkFault {
	log.debug("Getting component for compUse ID: " + compUsePojo.getId());
	RequestSummary request = compUseCache.get(compUsePojo);

	ComponentIdToken componentIdToken = request.getComponentId();
	Component component = getComponent(componentIdToken.getId());

	KbComponentIdToken kbCompIdToken = component.getKbComponentId();
	String kbComponentId;
	if (kbCompIdToken == null) {
	    kbComponentId = null;
	} else {
	    kbComponentId = kbCompIdToken.getId();
	}

	ComponentPojo compPojo = new ComponentPojoImpl(
		componentIdToken.getId(), component.getName(),
		component.getVersion(), kbComponentId);
	return compPojo;
    }

    /**
     * Gets component from cache if it's there, or SDK otherwise.
     *
     * @param id
     * @return
     * @throws Exception
     */
    Component getComponent(String id) throws SdkFault {
	log.debug("Getting component with ID: " + id);
	Component component;

	if (compCache.containsKey(id)) {

	    component = compCache.get(id);
	    log.debug("Component retrieved from cache: " + component.getName()
		    + " v" + component.getVersion() + " [" + id + "]");
	} else {
	    ComponentIdToken compIdToken = new ComponentIdToken();
	    compIdToken.setId(id);
	    component = colaApi.getCatalogComponent(compIdToken);
	    log.debug("Component retrieved from Code Center: "
		    + component.getName() + " v" + component.getVersion());
	    compCache.put(id, component); // cache it
	}

	return component;
    }

    /**
     * Put a component in the cache, for unit test use only.
     *
     * @param compPojo
     * @return
     * @throws Exception
     */
    void putComponent(String id, Component component) {
	log.debug("Caching component id: " + id + ", name: "
		+ component.getName() + ", version: " + component.getVersion());
	compCache.put(id, component); // cache it
    }

    public List<VulnerabilityPojo> getVulnerabilitiesSdkFields(
	    ComponentPojo compPojo, ComponentUsePojo compUsePojo)
	    throws Exception {
	log.debug("Getting vulnerabilities SDK fields for component: "
		+ compPojo.getName() + ", v" + compPojo.getVersion());
	List<VulnerabilityPojo> vulnPojos = fetchVulnerabilitiesSdkFields(
		compPojo, compUsePojo);
	return vulnPojos;
    }

    // private methods

    /**
     * Gets the vulnerability list for the given component from the SDK.
     *
     * @param componentId
     * @return
     * @throws Exception
     */
    private List<VulnerabilityPojo> fetchVulnerabilitiesSdkFields(
	    ComponentPojo compPojo, ComponentUsePojo compUsePojo)
	    throws Exception {
	List<VulnerabilityPojo> vulnPojos;

	Component comp = compCache.get(compPojo.getId()); // get the Code Center
							  // component from the
							  // cache
	String componentName = comp.getNameVersion().getName();
	String componentVersion = comp.getNameVersion().getVersion();
	log.debug("\tComponent: " + componentName + " version "
		+ componentVersion);

	VulnerabilityPageFilter vFilter = new VulnerabilityPageFilter();
	vFilter.setFirstRowIndex(0);
	vFilter.setLastRowIndex(vulnerabilityLastRowIndex);
	KbComponentReleaseNameVersionOrIdToken kbCompRelToken = colaApi
		.getKbComponentRelease(comp.getKbReleaseId()).getId();
	List<VulnerabilitySummary> vSums = vulnApi
		.searchDirectMatchedVulnerabilitiesByKBComponentReleaseId(
			kbCompRelToken, vFilter);
	vulnPojos = new ArrayList<VulnerabilityPojo>(vSums.size());
	for (VulnerabilitySummary vSum : vSums) {
	    log.debug("vulnApi.searchDirectMatchedVulnerabilitiesByKBComponentReleaseId returned: "
		    + vSum.getName());

	    String vulnIdString = vSum.getId().toString();
	    VulnerabilityPojo vulnPojo = new VulnerabilityPojoImpl(
		    vulnIdString, compUsePojo.getId(), vSum.getName(),
		    vSum.getDescription(), vSum.getSeverity(), vSum
			    .getPublished().toGregorianCalendar().getTime(),
		    null, 0L, null, null, null, null); // the null fields get
						       // filled in later by the
						       // DbDao
	    vulnPojos.add(vulnPojo);
	}

	return vulnPojos;
    }

    private void collectCustomAttrs(Map<String, String> appAttrNameValueMap,
	    Application app) {
	List<AttributeValue> attrValues = app.getAttributeValues();
	for (AttributeValue attrValue : attrValues) {
	    AttributeIdToken attrId = (AttributeIdToken) (attrValue
		    .getAttributeId());
	    if ((attrValue.getValues() != null)
		    && (attrValue.getValues().size() > 0)) {
		for (String appAttrName : appAttrNameIdMap.keySet()) {
		    if (appAttrNameIdMap.get(appAttrName)
			    .equals(attrId.getId())) {
			if (attrValue.getValues().size() > 1) {
			    log.warn("*** WARNING: app attr " + appAttrName
				    + " has " + attrValue.getValues().size()
				    + " values; only using the first one");
			}
			String attrValueString = attrValue.getValues().get(0);
			log.debug("Putting app attr " + appAttrName + " value "
				+ attrValueString);
			appAttrNameValueMap.put(appAttrName, attrValueString);
		    }
		}
	    }
	}
    }

    private void initCodeCenterApis(ServerBean serverBean,
	    ConfigurationManager config) throws Exception {
	CodeCenterServerWrapper ccServerWrapper = new CodeCenterServerWrapper(
		serverBean, config);
	initCodeCenterApis(ccServerWrapper);
    }

    private void initCodeCenterApis(CodeCenterServerWrapper ccServerWrapper) {
	applicationApi = ccServerWrapper.getInternalApiWrapper()
		.getApplicationApi();
	colaApi = ccServerWrapper.getInternalApiWrapper().getColaApi();
	vulnApi = ccServerWrapper.getInternalApiWrapper().getVulnerabilityApi();
	attrApi = ccServerWrapper.getInternalApiWrapper().getAttributeApi();
    }

}
