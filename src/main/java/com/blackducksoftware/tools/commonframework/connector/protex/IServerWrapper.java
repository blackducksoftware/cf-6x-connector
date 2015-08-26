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

import java.util.List;

import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;

// TODO: Auto-generated Javadoc
/**
 * The Interface IServerWrapper.
 */
public interface IServerWrapper {

    /**
     * Gets the project by name.
     *
     * @param projectName
     *            the project name
     * @return the project by name
     * @throws Exception
     *             the exception
     */
    public ProjectPojo getProjectByName(String projectName) throws Exception;

    /**
     * Gets the project by id.
     *
     * @param projectID
     *            the project id
     * @return the project by id
     * @throws Exception
     *             the exception
     */
    public ProjectPojo getProjectByID(String projectID) throws Exception;

    /**
     * Gets the projects.
     * 
     * @param <T>
     *
     * @return the projects
     * @throws Exception
     *             the exception
     */
    public <T> List<T> getProjects(Class<T> classType) throws Exception;

    // Helper methods
    /**
     * Gets the implementation of the specific API Wrapper.
     *
     * @return the internal api wrapper
     */
    public APIWrapper getInternalApiWrapper();

    /**
     * Gets the implementation of the specific Configuration Manager.
     *
     * @return the config manager
     */
    public ConfigurationManager getConfigManager();
}
