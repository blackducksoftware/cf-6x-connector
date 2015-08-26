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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.ApplicationApi;
import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationClone;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationUpdate;
import com.blackducksoftware.sdk.codecenter.application.data.InheritApprovalsTypeEnum;
import com.blackducksoftware.sdk.codecenter.application.data.Project;
import com.blackducksoftware.sdk.codecenter.application.data.ProjectIdToken;
import com.blackducksoftware.sdk.codecenter.approval.data.WorkflowNameToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AbstractAttribute;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeNameToken;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.dao.CodeCenterDaoConfigManager;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectOrApp;

public class CcApp implements ProjectOrApp {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private CodeCenterDaoConfigManager config;
    private Application app;
    private String version;
    private String workflowName;
    private CodeCenterServerWrapper ccServerWrapper;

    public CcApp(CodeCenterDaoConfigManager config, String name,
	    String version, String workflowName)
	    throws CommonFrameworkException {
	this.config = config;
	this.version = version;
	this.workflowName = workflowName;

	try {
	    log.debug("Constructing CodeCenterServerWrapper");
	    ccServerWrapper = new CodeCenterServerWrapper(
		    config.getServerBean(), (ConfigurationManager) config);
	} catch (Exception e) {
	    throw new CommonFrameworkException(
		    (ConfigurationManager) this.config,
		    "Error constructing CodeCenterServerWrapper: "
			    + e.getMessage());
	}
	app = loadApp(name, version);
    }

    public CcApp(CodeCenterServerWrapper ccServerWrapper,
	    CodeCenterDaoConfigManager config, String name, String version,
	    String workflowName) throws CommonFrameworkException {
	this.config = config;
	this.version = version;
	this.workflowName = workflowName;

	this.ccServerWrapper = ccServerWrapper;
	app = loadApp(name, version);
    }

    private Application loadApp(String name, String version)
	    throws CommonFrameworkException {
	ApplicationNameVersionToken appToken = new ApplicationNameVersionToken();
	appToken.setName(name);
	appToken.setVersion(version);

	Application app;
	try {
	    log.debug("Getting application");
	    app = ccServerWrapper.getInternalApiWrapper().getApplicationApi()
		    .getApplication(appToken);
	} catch (Exception e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error getting project " + name + ": " + e.getMessage());
	}
	return app;
    }

    @Override
    public ProjectOrApp clone(String newName) throws CommonFrameworkException {
	return cloneImpl(newName, null, null);
    }

    @Override
    public ProjectOrApp clone(String newName,
	    Map<String, String> appAttrUpdates, String associatedProjectId)
	    throws CommonFrameworkException {
	return cloneImpl(newName, appAttrUpdates, associatedProjectId);
    }

    private ProjectOrApp cloneImpl(String newName,
	    Map<String, String> appAttrUpdates, String associatedProjectId)
	    throws CommonFrameworkException {
	log.info("Cloning Code Center application " + getName() + " to "
		+ newName);

	log.info("Collecting vulnerability data");
	CcAppCompVuln vulnMetadataProcessor = null;
	try {
	    vulnMetadataProcessor = new CcAppCompVuln();
	    vulnMetadataProcessor.collectVulnerabilityMetadata(config,
		    getName(), app.getVersion());
	} catch (Exception e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error processing orig app component/vulnerabilities: "
			    + e.getMessage());
	}

	log.info("Finished collecting vulnerability data");

	Project associatedProtexProject = null;
	if (associatedProjectId != null) {
	    try {
		log.info("Getting associated Protex project");
		associatedProtexProject = getAssociatedProject(); // Get the
								  // orig app
								  // associated
								  // server/project
		ProjectIdToken projectToken = associatedProtexProject.getId();
		projectToken.setId(associatedProjectId); // Change the project,
							 // leave the server the
							 // same
		associatedProtexProject.setId(projectToken);
	    } catch (SdkFault e) {
		String msg = "Error getting Protex project associated with app "
			+ getName() + ": " + e.getMessage();
		log.warn(msg); // this could be normal, so log and keep going
	    }
	}

	// Code Center clone operation
	log.info("Performing Code Center application clone");
	ApplicationIdToken newAppIdToken = doCcClone(newName);

	// Copy those SDK-accessible fields that we need that clone does not
	// copy
	log.info("Copying other app metadata");
	copyOtherAppMetadata(newAppIdToken, newName, appAttrUpdates);

	try {
	    log.info("Applying vulnerability metadata");
	    vulnMetadataProcessor.applyVulnerabilityMetadata(config, newName,
		    app.getVersion());
	} catch (Exception e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error processing orig app component/vulnerabilities: "
			    + e.getMessage());
	}

	if (associatedProtexProject != null) {
	    log.info("Associating Protex project");
	    associateProject(newAppIdToken, associatedProtexProject);
	}

	ProjectOrApp clone = new CcApp(config, newName, version, workflowName);
	log.info("Finished cloning Code Center application " + getName()
		+ " to " + newName);
	return clone;
    }

    private void associateProject(ApplicationIdToken newAppIdToken,
	    Project associatedProtexProject) throws CommonFrameworkException {

	try {
	    ccServerWrapper
		    .getInternalApiWrapper()
		    .getApplicationApi()
		    .associateProtexProject(newAppIdToken,
			    associatedProtexProject.getId());
	} catch (SdkFault e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error making Protex project association on clone of app "
			    + getName() + ": " + e.getMessage());
	}
    }

    private Project getAssociatedProject() throws SdkFault {
	Project associatedProtexProject = null;
	associatedProtexProject = ccServerWrapper.getInternalApiWrapper()
		.getApplicationApi().getAssociatedProtexProject(app.getId());
	return associatedProtexProject;
    }

    private void copyOtherAppMetadata(ApplicationIdToken newAppIdToken,
	    String newName, Map<String, String> appAttrUpdates)
	    throws CommonFrameworkException {
	ApplicationUpdate appUpdate = new ApplicationUpdate();
	appUpdate.setId(newAppIdToken);
	appUpdate.setUseProtexstatus(app.isUseProtexstatus());
	appUpdate.setObligationFulFillment(app.isObligationFulFillment()); // This
									   // doesn't
									   // actually
									   // work

	// If we've been asked to set any app custom attrs on new app, do it
	// here:
	if (appAttrUpdates != null) {
	    for (String attrName : appAttrUpdates.keySet()) {
		log.debug("Setting custom attribute: " + attrName);
		AbstractAttribute abstractAttr = getAttributeIdToken(attrName,
			newName);

		AttributeValue attrValueObject = new AttributeValue();
		attrValueObject.setAttributeId(abstractAttr.getId());
		attrValueObject.getValues().add(appAttrUpdates.get(attrName));
		appUpdate.getAttributeValues().add(attrValueObject);
	    }
	}

	try {
	    log.debug("Updating application metadata");
	    ccServerWrapper.getInternalApiWrapper().getApplicationApi()
		    .updateApplication(appUpdate);
	} catch (SdkFault e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error updating cloned app " + newName + ": "
			    + e.getMessage());
	}
    }

    private AbstractAttribute getAttributeIdToken(String attrName,
	    String appName) throws CommonFrameworkException {
	AttributeNameToken attrToken = new AttributeNameToken();
	attrToken.setName(attrName);
	try {
	    return ccServerWrapper.getInternalApiWrapper().getAttributeApi()
		    .getAttribute(attrToken);
	} catch (SdkFault e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error looking up attr " + attrName + " for app " + appName
			    + ": " + e.getMessage());
	}
    }

    private ApplicationIdToken doCcClone(String newName)
	    throws CommonFrameworkException {

	ApplicationClone cloneRequest = new ApplicationClone();
	ApplicationIdToken appToken = new ApplicationIdToken();
	appToken.setId(getId());

	cloneRequest.setApplicationId(appToken);
	cloneRequest.setName(newName);
	cloneRequest.setDescription(app.getDescription());
	cloneRequest.setVersion(version);
	cloneRequest
		.setInheritApprovalsType(InheritApprovalsTypeEnum.INHERIT_EXISTING_REQUEST_WORKFLOW_VERSION);
	cloneRequest.setUseProtexstatus(app.isUseProtexstatus()); // This
								  // doesn't
								  // actually
								  // work
	cloneRequest.setObligationFulFillment(app.isObligationFulFillment()); // This
									      // doesn't
									      // actually
									      // work
	cloneRequest.setInheritAttachments(true);
	WorkflowNameToken workflowToken = new WorkflowNameToken();
	workflowToken.setName(workflowName);
	cloneRequest.setWorkflowId(workflowToken);

	ApplicationIdToken newAppIdToken;
	try {
	    log.debug("Calling applicationApi to perform clone");
	    newAppIdToken = ccServerWrapper.getInternalApiWrapper()
		    .getApplicationApi().clone(cloneRequest);
	} catch (SdkFault e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error cloning app " + getName() + ": " + e.getMessage());
	}

	return newAppIdToken;
    }

    @Override
    public void rename(String newName) throws CommonFrameworkException {
	log.info("Renaming app from: " + getName() + " to: " + newName);
	ApplicationApi applicationApi = ccServerWrapper.getInternalApiWrapper()
		.getApplicationApi();
	ApplicationUpdate applicationUpdate = new ApplicationUpdate();
	ApplicationIdToken idToken = new ApplicationIdToken();
	idToken.setId(getId());
	applicationUpdate.setId(idToken);
	applicationUpdate.setName(newName);
	try {
	    applicationApi.updateApplication(applicationUpdate);
	} catch (SdkFault e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error renaming app " + getName() + ": " + e.getMessage());
	}
	// Update the app object
	app = loadApp(newName, app.getVersion());
    }

    @Override
    public void lock() throws CommonFrameworkException {
	log.info("Locking application");
	ApplicationApi applicationApi = ccServerWrapper.getInternalApiWrapper()
		.getApplicationApi();
	ApplicationIdToken appToken = new ApplicationIdToken();
	appToken.setId(getId());
	try {
	    applicationApi.lockApplication(appToken, true);
	} catch (SdkFault e) {
	    throw new CommonFrameworkException((ConfigurationManager) config,
		    "Error locking app " + getName() + ": " + e.getMessage());
	}
    }

    @Override
    public String getName() {
	return app.getName();
    }

    @Override
    public String getId() {
	return app.getId().getId();
    }

    /**
     * Get the ID of a different project/app, or null if it doesn't exist. For
     * Code Center apps, the version used is the same as this app's version
     *
     * @param targetAppName
     * @return
     */
    @Override
    public String getId(String targetAppName) {
	log.info("Fetching the ID of application: " + targetAppName
		+ "; version: " + version);
	ApplicationApi applicationApi = ccServerWrapper.getInternalApiWrapper()
		.getApplicationApi();
	ApplicationNameVersionToken appToken = new ApplicationNameVersionToken();
	appToken.setName(targetAppName);
	appToken.setVersion(version);
	String targetAppId = null;
	try {
	    Application targetApp = applicationApi.getApplication(appToken);
	    targetAppId = targetApp.getId().getId();
	} catch (SdkFault e) {
	    log.info("Application not found: " + targetAppName + "; version: "
		    + version + ": " + e.getMessage());
	}
	return targetAppId;
    }
}
