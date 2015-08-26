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

package com.blackducksoftware.tools.commonframework.standard.codecenter.pojo;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.commonframework.standard.codecenter.dao.ApplicationData6_6_1Dao;
import com.blackducksoftware.tools.commonframework.standard.codecenter.dao.CodeCenter6_6_1Dao;
import com.blackducksoftware.tools.commonframework.standard.codecenter.dao.CodeCenterDaoConfigManager;

@SuppressWarnings("deprecation")
public class CcAppCompVuln {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private Map<String, VulnerabilityPojo> vulnMap;

    public CcAppCompVuln() {
	vulnMap = new HashMap<String, VulnerabilityPojo>(1000);
    }

    /**
     * Collect and remember componentuse vulnerability metadata from the given
     * app.
     *
     * @param config
     * @param appName
     * @param version
     * @throws Exception
     */
    public void collectVulnerabilityMetadata(CodeCenterDaoConfigManager config,
	    String appName, String version) throws Exception {
	processVulnerabilityMetadata(config, appName, version, false);
    }

    public boolean updateVulnerabilityMetadata(String compName,
	    String compVersion, String vulnName, Date targetRemediationDate,
	    Date actualRemediationDate, String statusIdString, String comment)
	    throws Exception {

	String key = generateKey(compName, compVersion, vulnName);

	if (vulnMap.containsKey(key)) {
	    VulnerabilityPojo vuln = vulnMap.get(key);
	    vuln.setTargetRemediationDate(targetRemediationDate);
	    vuln.setActualRemediationDate(actualRemediationDate);

	    if ((statusIdString != null) && (statusIdString.length() > 0)) {
		double statusIdDouble = Double.parseDouble(statusIdString);
		long statusId = (int) statusIdDouble; // truncate off the
						      // decimal part that Excel
						      // adds
		vuln.setStatusId(statusId);
	    }

	    if ((comment != null) && (comment.length() > 0)) {
		vuln.setStatusComment(comment);
	    }
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Apply the remembered componentuse vulnerability metadata to the given
     * app.
     *
     * @param config
     * @param appName
     * @param version
     * @throws Exception
     */
    public void applyVulnerabilityMetadata(CodeCenterDaoConfigManager config,
	    String appName, String version) throws Exception {
	processVulnerabilityMetadata(config, appName, version, true);
    }

    private Map<String, VulnerabilityPojo> processVulnerabilityMetadata(
	    CodeCenterDaoConfigManager config, String appName, String version,
	    boolean writeMode) throws Exception {

	ApplicationData6_6_1Dao dataSource = null;

	try {
	    log.info("Fetching application: " + appName + " version " + version);
	    dataSource = new CodeCenter6_6_1Dao(config);
	    ApplicationPojo app = dataSource.getApplication(appName, version);
	    if (app == null) {
		throw new Exception("Unable to load application " + appName
			+ " version " + version);
	    }

	    log.info("Fetching components and vulnerabilities");

	    collectDataApplication(dataSource, app, writeMode);
	} finally {
	    if (dataSource != null) {
		dataSource.close();
	    }
	}

	return vulnMap;
    }

    private void collectDataApplication(ApplicationData6_6_1Dao dataSource,
	    ApplicationPojo app, boolean writeMode) throws Exception {
	List<ComponentUsePojo> compUses = dataSource.getComponentUses(app);
	for (ComponentUsePojo compUse : compUses) {
	    collectDataComponentUse(dataSource, app, compUse, writeMode);
	}
    }

    private void collectDataComponentUse(ApplicationData6_6_1Dao dataSource,
	    ApplicationPojo app, ComponentUsePojo compUse, boolean writeMode)
	    throws Exception {
	ComponentPojo comp = dataSource.getComponent(compUse);
	collectDataComponent(dataSource, app, compUse, comp, writeMode);
    }

    private void collectDataComponent(ApplicationData6_6_1Dao dataSource,
	    ApplicationPojo app, ComponentUsePojo compUse, ComponentPojo comp,
	    boolean writeMode) throws Exception {

	List<VulnerabilityPojo> vulns = dataSource.getVulnerabilities(comp,
		compUse);
	for (VulnerabilityPojo vuln : vulns) {
	    log.debug("Vulnerability: " + vuln.getName());

	    log.debug("App: " + app.getName() + " / " + app.getVersion()
		    + "; comp: " + comp.getName() + " / " + comp.getVersion()
		    + "; vuln: " + vuln.getName());

	    String key = generateKey(comp.getName(), comp.getVersion(),
		    vuln.getName());
	    VulnerabilityPojo origVuln = null;

	    if (!writeMode) {

		log.debug("Read from Code Center: target: "
			+ vuln.getTargetRemediationDate() + " ("
			+ getTimeMillis(vuln.getTargetRemediationDate()) + ")"
			+ ", actual: " + vuln.getActualRemediationDate() + " ("
			+ getTimeMillis(vuln.getActualRemediationDate()) + ")"
			+ "; vuln status ID: " + vuln.getStatusId()
			+ "; vuln status comment: " + vuln.getStatusComment());
		vulnMap.put(key, vuln);
	    } else {
		if (!vulnMap.containsKey(key)) {
		    throw new Exception(key + " not found in original app");
		}

		origVuln = vulnMap.get(key);
		vuln.setTargetRemediationDate(origVuln
			.getTargetRemediationDate());
		vuln.setActualRemediationDate(origVuln
			.getActualRemediationDate());
		vuln.setStatusId(origVuln.getStatusId());
		vuln.setStatusComment(origVuln.getStatusComment());

		log.debug("Writing to Code Center: target: "
			+ vuln.getTargetRemediationDate() + " ("
			+ getTimeMillis(vuln.getTargetRemediationDate()) + ")"
			+ ", actual: " + vuln.getActualRemediationDate() + " ("
			+ getTimeMillis(vuln.getActualRemediationDate()) + ")"
			+ "; vuln status ID: " + vuln.getStatusId()
			+ "; vuln status comment: " + vuln.getStatusComment());

		dataSource.updateCompUseVulnData(compUse, vuln);
	    }
	}
    }

    private long getTimeMillis(Date date) {
	if (date == null) {
	    return 0L;
	}
	return date.getTime();
    }

    // TODO: This is a little weak
    private String generateKey(String compName, String compVersion,
	    String vulnName) {
	String delim = "|||";
	return compName + delim + compVersion + delim + vulnName;
    }
}
