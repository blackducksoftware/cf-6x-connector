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
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ApplicationPojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ApplicationPojoImpl;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentPojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentUsePojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.VulnerabilityPojo;

@SuppressWarnings("deprecation")
public class CodeCenter6_6_1Dao implements ApplicationData6_6_1Dao {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private static final int EST_NUM_COMPONENTS_PER_APP = 30; // TODO: these are
							      // duplicated in
							      // the SdkDao, and
    private static final int EST_NUM_VULNS_PER_COMPONENT = 40; // probably
							       // shouldn't be
							       // hard coded at
							       // all
    private final CodeCenter6_6_1SdkDao ccSdkDao;
   
    private final CodeCenter6_6_1DbDao ccDbDao;
    private Map<String, List<VulnerabilityPojo>> compVulnCache; // cache of
								// vulnerabilities
								// per Component
								// ID


    public CodeCenter6_6_1Dao(CodeCenterDaoConfigManager config)
	    throws Exception {
	ccSdkDao = new CodeCenter6_6_1SdkDao(config);
	ccDbDao = new CodeCenter6_6_1DbDao(config);
	init(config.getEstNumApps());
    }

    /**
     * Use this constructor for SDK-only access, when you'll use this DAO object
     * for a single app.
     *
     * @param ccServerWrapper
     * @throws Exception
     */
    public CodeCenter6_6_1Dao(CodeCenterServerWrapper ccServerWrapper)
	    throws SdkFault {
	ccSdkDao = new CodeCenter6_6_1SdkDao(ccServerWrapper);
	ccDbDao = null;
	init(1);
    }

    /**
     * This constructor should only be used by unit tests
     *
     * @throws Exception
     */
    CodeCenter6_6_1Dao() throws Exception {
	ccSdkDao = null;
	ccDbDao = null;
	init(5);
    }

    public void setSkipNonKbComponents(boolean skipNonKbComponents) {
	ccSdkDao.setSkipNonKbComponents(skipNonKbComponents);
    }

    private void init(int estNumApps) {
	compVulnCache = new HashMap<String, List<VulnerabilityPojo>>(estNumApps
		* EST_NUM_COMPONENTS_PER_APP * EST_NUM_VULNS_PER_COMPONENT);
    }

    @Override
    public void limitNumberOfApplications(int maxNumberOfApplications) {
	ccSdkDao.limitNumberOfApplications(maxNumberOfApplications);
    }

    @Override
    public List<ApplicationPojo> getApplications() throws Exception {
	return ccSdkDao.getApplications();
    }

    @Override
    public ApplicationPojo getApplication(String appName, String version)
	    throws Exception {
	return ccSdkDao.getApplication(appName, version);
    }

    @Override
    public List<ComponentUsePojo> getComponentUses(ApplicationPojo appPojo)
	    throws SdkFault {
	return ccSdkDao.getComponentUses(appPojo);
    }

    @Override
    public SortedSet<ComponentPojo> getComponents(String appId) throws SdkFault {
	ApplicationPojo appPojo = new ApplicationPojoImpl(appId);
	List<ComponentPojo> pojoList = getComponents(appPojo);
	SortedSet<ComponentPojo> pojoSortedSet = new TreeSet<ComponentPojo>();
	pojoSortedSet.addAll(pojoList);
	return pojoSortedSet;
    }

    @Override
    public List<ComponentPojo> getComponents(ApplicationPojo appPojo)
	    throws SdkFault {
	List<ComponentPojo> compPojos = new ArrayList<ComponentPojo>(
		EST_NUM_COMPONENTS_PER_APP);
	List<ComponentUsePojo> compUses = getComponentUses(appPojo);
	for (ComponentUsePojo compUse : compUses) {
	    compPojos.add(getComponent(compUse));
	}
	return compPojos;
    }

    @Override
    public ComponentPojo getComponent(ComponentUsePojo componentUse)
	    throws SdkFault {
	return ccSdkDao.getComponent(componentUse);
    }


    @Override
    public List<VulnerabilityPojo> getVulnerabilities(ComponentPojo compPojo,
	    ComponentUsePojo compUsePojo) throws Exception {
	List<VulnerabilityPojo> vulnPojos;

	// First check the cache; if it's there, pull from there
	vulnPojos = getVulnListFromCache(compPojo.getId());
	if (vulnPojos != null) {
	    // The vuln status fields vary per-use, so need to be set every time
	    for (VulnerabilityPojo vuln : vulnPojos) {
		ccDbDao.setVulnStatusFields(vuln, compUsePojo);
	    }
	    return vulnPojos;
	}

	vulnPojos = ccSdkDao.getVulnerabilitiesSdkFields(compPojo, compUsePojo);
	for (VulnerabilityPojo vuln : vulnPojos) {
	    ccDbDao.setDbFields(vuln, compUsePojo);
	}
	log.info("Vulnerabilities retrieved from Code Center for component: "
		+ compPojo.getName() + " / " + compPojo.getVersion());

	// Cache it
	compVulnCache.put(compPojo.getId(), vulnPojos);
	return vulnPojos;
    }

    List<VulnerabilityPojo> getVulnListFromCache(String componentId) {
	List<VulnerabilityPojo> vulnPojos;
	if (compVulnCache.containsKey(componentId)) {
	    vulnPojos = compVulnCache.get(componentId);
	    log.info("Vulnerabilities retrieved from cache for component: "
		    + componentId);
	    return vulnPojos;
	} else {
	    return null;
	}
    }

 
    @Override
    public void close() throws Exception {
	ccDbDao.close();
    }

    /**
     * This method should only be used by unit tests.
     *
     * @param componentId
     * @param vulnList
     */
    void putVulnerabilityList(String componentId,
	    List<VulnerabilityPojo> vulnList) {
	compVulnCache.put(componentId, vulnList);
    }

    @Override
    public void updateCompUseVulnData(ComponentUsePojo compUse,
	    VulnerabilityPojo vuln) throws Exception {
	ccDbDao.updateCompUseVulnData(compUse, vuln);

    }
}
