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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.license.LicenseCategory;
import com.blackducksoftware.sdk.protex.project.Project;
import com.blackducksoftware.sdk.protex.project.ProjectApi;
import com.blackducksoftware.sdk.protex.project.ProjectInfo;
import com.blackducksoftware.sdk.protex.project.ProjectRequest;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

/**
 * Wrapper class around the Protex Server that provides common methods. This is
 * the primary class for SDK access.
 *
 * @author akamen
 *
 */
public class ProtexServerWrapper<T extends ProtexProjectPojo> implements
	IServerWrapper {

    /** The log. */
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    /** The api wrapper. */
    private ProtexAPIWrapper apiWrapper;

    /** The config manager. */
    private ConfigurationManager configManager;
    
    // This holds the connection information 
    private ServerBean serverBean;

    public ProtexServerWrapper(ServerBean bean, ConfigurationManager manager,
	    boolean validate) throws Exception {
	serverBean = bean;
	configManager = manager;
	apiWrapper = new ProtexAPIWrapper(bean, manager, validate);
    }

    /**
     * Returns a pojo based on name. Throws exception if name does not produce
     * anything
     *
     * @param projectName
     *            the project name
     * @return the project by name
     * @throws Exception
     *             the exception
     */
    @Override
    public ProjectPojo getProjectByName(String projectName)
	    throws CommonFrameworkException {
	ProtexProjectPojo pojo = null;
	try {
	    ProjectApi projectAPI = getInternalApiWrapper().getProjectApi();
	    Project proj = projectAPI.getProjectByName(projectName.trim());

	    if (proj == null) {
		throw new Exception(
			"Project name specified, resulted in empty project object:"
				+ projectName);
	    }

	    pojo = populateProjectBean(proj);

	} catch (Exception e) {

	    throw new CommonFrameworkException(configManager,
		    "Unable to find project by the name of: " + projectName);
	}

	return pojo;
    }

    /**
     * Returns project POJO based on ID.
     *
     * @param projectID
     *            the project id
     * @return the project by id
     * @throws Exception
     *             the exception
     */
    @Override
    public ProjectPojo getProjectByID(String projectID)
	    throws CommonFrameworkException {
	ProjectPojo pojo = null;
	try {
	    ProjectApi projectAPI = apiWrapper.getProjectApi();
	    Project proj = projectAPI.getProjectById(projectID);

	    if (proj == null) {
		throw new Exception(
			"Project ID specified, resulted in empty project object:"
				+ projectID);
	    }

	    pojo = populateProjectBean(proj);

	} catch (Exception e) {
	    throw new CommonFrameworkException(configManager,
		    "Unable to find project by the ID of: " + projectID);
	}

	return pojo;
    }

    private ProtexProjectPojo populateProjectBean(Project proj) {
	ProtexProjectPojo pojo = new ProtexProjectPojo(proj.getProjectId(),
		proj.getName());

	try {
	    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
	    String prettyAnalyzedDate = df.format(proj.getLastAnalyzedDate());
	    pojo.setAnalyzedDate(prettyAnalyzedDate);
	    log.debug("Set project last analyzed date: " + prettyAnalyzedDate);
	} catch (Exception e) {
	}

	return pojo;
    }

    /**
     * Returns a list of ProtexProjectPojos populated with necessary date.
     *
     * @param <T>
     *            Your pojo (can be a default ProtexProjectPojo).
     *
     * @return the projects
     * @throws Exception
     *             the exception
     */


    @Override
    public <T> List<T> getProjects(Class<T> theProjectClass) throws Exception {
	ArrayList<T> projectList = new ArrayList<T>();

	try {
	    ProjectApi projectAPI = apiWrapper.getProjectApi();

	    String userName = configManager.getServerBean().getUserName();
	    if (userName == null || userName.length() == 0) {
		userName = serverBean.getUserName();
	    }

	    List<ProjectInfo> project_list_info = projectAPI
		    .getProjectsByUser(userName);

	    for (ProjectInfo project : project_list_info) {
		if (project != null) {
		    String projName = project.getName();
		    String projID = project.getProjectId();
		   
		    T projPojo = (T) generateNewInstance(theProjectClass);

		    // Set the basic
		    ((ProtexProjectPojo) projPojo).setProjectKey(projID);
		    ((ProtexProjectPojo) projPojo).setProjectName(projName);

		    projectList.add(projPojo);
		}
	    }
	} catch (SdkFault sf) {
	    // Try to explain why this has failed...messy, but could save time
	    // and aggravation

	    String message = sf.getMessage();
	    if (message != null) {
		if (message.contains("role")) {
		    throw new Exception(
			    "You do not have enough permission to list projects, you must be at least a 'Manager' to perform this task");
		}
	    } else {
		throw new Exception("Error getting project list", sf);
	    }
	} catch (Throwable t) {

	    if (t instanceof javax.xml.ws.soap.SOAPFaultException) {
		throw new Exception("There *may* be problem with SDK", t);
	    } else if (t instanceof javax.xml.ws.WebServiceException) {
		throw new Exception(
			"There *may* be problem with the connection.  The URL specified cannot be reached!",
			t);
	    } else {
		throw new Exception("General error, cannot continue! Error: ",
			t);
	    }
	}

	return projectList;
    }

    /**
     * Creates the project.
     *
     * @param projectName
     *            the project name
     * @param description
     *            the description
     * @return the string
     * @throws Exception
     *             the exception
     */
    public String createProject(String projectName, String description)
	    throws Exception {
	String projectID = "";
	ProjectRequest projectRequest = new ProjectRequest();
	projectRequest.setName(projectName);

	if (description != null) {
	    projectRequest.setDescription(description);
	}

	try {
	    ProjectApi projectAPI = apiWrapper.getProjectApi();
	    projectID = projectAPI.createProject(projectRequest,
		    LicenseCategory.PROPRIETARY);
	} catch (SdkFault e) {
	    throw new Exception(e.getMessage());
	}

	return projectID;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.blackducksoftware.tools.commonframework.standard.common.IServerWrapper
     * # getInternalApiWrapper()
     */
    @Override
    public ProtexAPIWrapper getInternalApiWrapper() {
	return apiWrapper;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.blackducksoftware.tools.commonframework.standard.common.IServerWrapper
     * # getConfigManager()
     */
    @Override
    public ConfigurationManager getConfigManager() {
	return this.configManager;
    }

    @SuppressWarnings("unchecked")
    private T generateNewInstance(Class<?> theProjectClass) throws Exception {
	T pojo = null;
	Constructor<?> constructor = null;
	;
	try {
	    constructor = theProjectClass.getConstructor();
	} catch (SecurityException e) {
	    throw new Exception(e.getMessage());
	} catch (NoSuchMethodException e) {
	    throw new Exception(e.getMessage());
	}

	try {
	    pojo = (T) constructor.newInstance();
	} catch (IllegalArgumentException e) {
	    throw new Exception(e.getMessage());
	} catch (InstantiationException e) {
	    throw new Exception(e.getMessage());
	} catch (IllegalAccessException e) {
	    throw new Exception(e.getMessage());
	} catch (InvocationTargetException e) {
	    throw new Exception(e.getMessage());
	}

	return pojo;
    }
}
