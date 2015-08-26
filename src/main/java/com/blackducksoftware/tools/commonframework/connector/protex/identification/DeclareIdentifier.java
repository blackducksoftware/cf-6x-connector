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

package com.blackducksoftware.tools.commonframework.connector.protex.identification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.BomRefreshMode;
import com.blackducksoftware.sdk.protex.common.UsageLevel;
import com.blackducksoftware.sdk.protex.license.License;
import com.blackducksoftware.sdk.protex.license.LicenseInfo;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.CodeMatchDiscovery;
import com.blackducksoftware.sdk.protex.project.codetree.identification.DeclaredIdentificationRequest;

public class DeclareIdentifier implements Identifier {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private String programName;
    private ProtexIdUtils protexUtils;

    public DeclareIdentifier(String programName) {
	programName = this.programName;
    }

    @Override
    public void setProtexUtils(ProtexIdUtils protexUtils) {
	this.protexUtils = protexUtils;
    }

    @Override
    public void makeIdentificationOnFile(String path, CodeMatchDiscovery target)
	    throws SdkFault {
	DeclaredIdentificationRequest declRequest = new DeclaredIdentificationRequest();

	LicenseInfo lic = new LicenseInfo();

	String licenseId = target.getMatchingLicenseInfo().getLicenseId();
	License thisLicense = protexUtils.getProtexServerWrapper()
		.getInternalApiWrapper().getLicenseApi()
		.getLicenseById(licenseId);
	if (thisLicense != null) {
	    log.debug(target.getMatchingComponentId() + ": License: "
		    + thisLicense.getName());
	    lic.setLicenseId(thisLicense.getLicenseId());
	    lic.setName(thisLicense.getName());
	}

	declRequest.setIdentifiedComponentId(target.getMatchingComponentId());
	declRequest.setIdentifiedVersionId(null);

	if (thisLicense != null) {
	    declRequest.setIdentifiedLicenseInfo(lic);
	} else {
	    declRequest.setIdentifiedLicenseInfo(null);
	}

	declRequest.setIdentifiedUsageLevel(UsageLevel.COMPONENT);

	declRequest.setComment("Declare Id-ed by " + programName
		+ " at \" + new Date()");

	log.info("Adding Declaration for " + path + ": "
		+ target.getMatchingComponentId());
	protexUtils
		.getProtexServerWrapper()
		.getInternalApiWrapper()
		.getIdentificationApi()
		.addDeclaredIdentification(protexUtils.getProjectId(), path,
			declRequest, BomRefreshMode.SKIP);
    }

    @Override
    public boolean isFinalBomRefreshRequired() {
	// Using this strategy, BOM must be refreshed at the end
	return true;
    }

    @Override
    public boolean isMultiPassIdStrategy() {
	return false;
    }
}
