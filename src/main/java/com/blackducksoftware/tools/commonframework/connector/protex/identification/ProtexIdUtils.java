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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.component.version.ComponentVersion;
import com.blackducksoftware.sdk.protex.project.Project;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeType;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeWithCount;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTreeWithCount;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.CodeMatchDiscovery;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.CodeMatchType;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.Discovery;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.IdentificationStatus;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.identification.IdentificationMade;

public class ProtexIdUtils {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Identifier identifier;
    private static ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper = null;
    private final String projectId;
    private final List<CodeTreeNodeType> nodeTypesToIncludeFiles;
    private final List<CodeTreeNodeType> nodeTypesToIncludeAll;
    private final Collection<IdentificationMade> identificationsMade = new ArrayList<IdentificationMade>();
    private boolean doRefresh;

    /**
     * Constructor
     *
     * @param config
     *            a ConfigurationManager with Protex server/username/password
     *            set.
     * @throws Exception
     *             upon error connecting to or using Protex
     */
    public ProtexIdUtils(ConfigurationManager config, Identifier identifier,
	    String protexProjectName, boolean doRefresh) throws Exception {
	this.doRefresh = doRefresh;

	log.debug("Creating ProtexServerWrapper");
	protexServerWrapper = new ProtexServerWrapper<ProtexProjectPojo>(config.getServerBean(),
		config, true);
	log.debug("Created ProtexServerWrapper");

	log.debug("Loading project " + protexProjectName);
	Project project = loadProject(protexProjectName);
	log.debug("Loaded project");
	projectId = project.getProjectId();

	nodeTypesToIncludeFiles = getNodeTypesToIncludeFiles();
	nodeTypesToIncludeAll = getNodeTypesToIncludeAll();

	this.identifier = identifier;
    }

    /**
     * Returns the list of identifications made so far
     *
     * @return the list of identifications made so far
     */
    public Collection<IdentificationMade> getIdentificationsMade() {
	return identificationsMade;
    }

    /**
     * Get the PartialCodeTree for a given path/CodeTreeNode
     *
     * @param path
     * @param node
     * @return
     */
    public PartialCodeTree getThisNodesTree(String path, CodeTreeNode node) {
	PartialCodeTree thisFilesTree = new PartialCodeTree();
	thisFilesTree.setParentPath(path);
	thisFilesTree.getNodes().add(node);
	return thisFilesTree;
    }

    /**
     * Set up the filter for getting the contents of a dir
     *
     * @return
     */
    private List<CodeTreeNodeType> getNodeTypesToIncludeFiles() {
	List<CodeTreeNodeType> nodeTypesToInclude = new ArrayList<CodeTreeNodeType>();
	nodeTypesToInclude.add(CodeTreeNodeType.FILE);
	return nodeTypesToInclude;
    }

    /**
     * Set up a filter for getting a dir node. TODO: Seems like this could be
     * narrowed
     *
     * @return
     */
    private List<CodeTreeNodeType> getNodeTypesToIncludeAll() {
	List<CodeTreeNodeType> nodeTypesToInclude = new ArrayList<CodeTreeNodeType>();
	nodeTypesToInclude.add(CodeTreeNodeType.FILE);
	nodeTypesToInclude.add(CodeTreeNodeType.FOLDER);
	nodeTypesToInclude.add(CodeTreeNodeType.EXPANDED_ARCHIVE);
	return nodeTypesToInclude;
    }

    public String getProjectId() {
	return projectId;
    }

    /**
     * Performs a Code Match identification Code Match attempts to do a code
     * match identification, as opposed to declare file.
     *
     * @param path
     *            the path to the file
     * @param target
     *            the discovery to make the identification with
     * @throws SdkFault
     */
    public void makeId(String path, Discovery discoveryTarget) throws SdkFault {
	CodeMatchDiscovery target = (CodeMatchDiscovery) discoveryTarget;
	log.debug("Making match for: " + target.getFilePath() + ": "
		+ target.getMatchingComponentId() + ", type: "
		+ target.getDiscoveryType());

	identifier.makeIdentificationOnFile(path, target);

	IdentificationMade idMade = new IdentificationMade(path, target
		.getMatchingSourceInfo().getFirstLine(), target
		.getMatchingSourceInfo().getLineCount(),
		target.getMatchingComponentId(), target.getMatchingVersionId(),
		getComponentVersionString(target),
		target.getMatchRatioAsPercent());

	identificationsMade.add(idMade);
	log.debug("Added Identification for " + idMade);
    }

    /**
     * Load the project of the given name from Protex
     *
     * @param projectName
     * @return
     * @throws SdkFault
     *             if project doesn't exist
     */
    private Project loadProject(String projectName) throws SdkFault {
	Project project = protexServerWrapper.getInternalApiWrapper()
		.getProjectApi().getProjectByName(projectName);
	return project;
    }

    public ProtexServerWrapper<ProtexProjectPojo> getProtexServerWrapper() {
	return protexServerWrapper;
    }

    /**
     * Get a PartialCodeTree for the given dir (just the directory)
     *
     * @param path
     * @return
     * @throws SdkFault
     */
    public PartialCodeTree getCodeTreeDir(String path) throws SdkFault {
	PartialCodeTree thisDir = protexServerWrapper
		.getInternalApiWrapper()
		.getCodeTreeApi()
		.getCodeTreeByNodeTypes(projectId, path, 0, true,
			nodeTypesToIncludeAll);
	return thisDir;
    }

    /**
     * Get a PartialCodeTree for the files/dirs in the given dir.
     *
     * @param path
     * @return
     * @throws SdkFault
     */
    public PartialCodeTree getCodeTreeDirContents(String path) throws SdkFault {
	PartialCodeTree dirContents = protexServerWrapper
		.getInternalApiWrapper()
		.getCodeTreeApi()
		.getCodeTreeByNodeTypes(projectId, path, 1, false,
			nodeTypesToIncludeAll);
	return dirContents;
    }

    /**
     * Get a PartialCodeTree for the files in given dir and its children
     *
     * @param path
     * @return
     * @throws SdkFault
     */
    public PartialCodeTree getAllCodeTreeFiles(String path) throws SdkFault {
	PartialCodeTree files = protexServerWrapper
		.getInternalApiWrapper()
		.getCodeTreeApi()
		.getCodeTreeByNodeTypes(projectId, path, -1, false,
			nodeTypesToIncludeFiles);
	return files;
    }

    /**
     * Get a PartialCodeTreeWithCount for the files in a given PartialCodeTree
     *
     * @param tree
     * @return
     * @throws SdkFault
     */
    public PartialCodeTreeWithCount getPartialCodeTreeWithCount(
	    PartialCodeTree tree) throws SdkFault {
	return protexServerWrapper.getInternalApiWrapper().getDiscoveryApi()
		.getCodeMatchPendingIdFileCount(projectId, tree);
    }

    /**
     * Get the "best" match out of the given list of code match discoveries.
     * Best = most (highest %) of code coverage.
     *
     * @param codeMatchDiscoveries
     * @return
     * @throws SdkFault
     */
    public CodeMatchDiscovery bestMatch(
	    List<CodeMatchDiscovery> codeMatchDiscoveries) throws SdkFault {
	int maxScore = 0;
	CodeMatchDiscovery bestCodeMatchDiscovery = null;
	for (CodeMatchDiscovery match : codeMatchDiscoveries) {
	    int thisScore = match.getMatchRatioAsPercent();
	    String versionString = getComponentVersionString(match);

	    log.debug("Code Match Discovery: " + match.getMatchingComponentId()
		    + "/" + versionString + "; score: " + thisScore
		    + "; ID status: "
		    + match.getIdentificationStatus().toString());

	    if (match.getIdentificationStatus() == IdentificationStatus.PENDING_IDENTIFICATION) {

		if (thisScore > maxScore) {
		    log.debug("\tThis one is the best so far");
		    bestCodeMatchDiscovery = match;
		    maxScore = thisScore;
		} else {
		    log.debug("\tThis one is NOT the best so far; ignoring it");
		}
	    } else {
		log.debug("\tThis match identification status was not pending; ignoring it");
	    }
	}
	return bestCodeMatchDiscovery;
    }

    /**
     * Get the version string for a match. This is SLOW.
     *
     * @param match
     * @return
     */
    public static String getComponentVersionString(CodeMatchDiscovery match) {
	String versionString = "unknown";
	if (protexServerWrapper != null) {
	    try {
		ComponentVersion version = protexServerWrapper
			.getInternalApiWrapper()
			.getComponentVersionApi()
			.getComponentVersionById(
				match.getMatchingComponentId(),
				match.getMatchingVersionId());
		versionString = version.getVersionName();
	    } catch (Exception e) {
	    }
	}
	return versionString;
    }

    /**
     * Get the code match discoveries for a file.
     *
     * @param tree
     * @return
     * @throws SdkFault
     */
    public List<CodeMatchDiscovery> getCodeMatchDiscoveries(PartialCodeTree tree)
	    throws SdkFault {
	List<CodeMatchType> codeMatchTypes = new ArrayList<CodeMatchType>();
	codeMatchTypes.add(CodeMatchType.PRECISION);
	// codeMatchTypes.add(CodeMatchType.GENERIC); // Precision matches are
	// better;
	List<CodeMatchDiscovery> codeMatchDiscoveries = protexServerWrapper
		.getInternalApiWrapper().getDiscoveryApi()
		.getCodeMatchDiscoveries(projectId, tree, codeMatchTypes);
	return codeMatchDiscoveries;
    }

    /**
     * Get the code match discoveries for a list of files.
     *
     * @param tree
     * @return
     * @throws SdkFault
     */
    public List<CodeMatchDiscovery> getCodeMatchDiscoveries(String path,
	    List<CodeTreeNode> files) throws SdkFault {
	PartialCodeTree tree = new PartialCodeTree();
	tree.setParentPath(path);
	tree.getNodes().addAll(files);
	return getCodeMatchDiscoveries(tree);
    }

    /**
     * Refresh the BOM
     *
     * @throws SdkFault
     */
    public void refreshBom() throws SdkFault {
	if (!doRefresh) {
	    log.info("Skipping BOM refresh as requested");
	    return;
	}
	if (identifier.isFinalBomRefreshRequired()) {
	    log.info("Refreshing BOM.");
	    protexServerWrapper.getInternalApiWrapper().getBomApi()
		    .refreshBom(projectId, true, false);
	}
    }

    /**
     * Find out if the identifier being used requires multiple passes on the
     * file tree to make all identifications
     *
     * @return true if multiple passes are required to process all pending IDs
     */
    public boolean isMultiPassIdStrategy() {
	return identifier.isMultiPassIdStrategy();
    }

    /**
     * Find out if the given code tree has pending IDs.
     *
     * @param tree
     * @return
     * @throws SdkFault
     */
    public boolean hasPendingIds(PartialCodeTree tree) throws SdkFault {
	try {
	    PartialCodeTreeWithCount treeWithCount = getPartialCodeTreeWithCount(tree);
	    List<CodeTreeNodeWithCount> nodes = treeWithCount.getNodes();
	    log.debug("hasPendingIds(" + tree.getParentPath() + ") found "
		    + nodes.size() + " nodes");
	    return nodes.get(0).getCount() > 0;
	} catch (Exception e) {
	    return false;
	}
    }
}
