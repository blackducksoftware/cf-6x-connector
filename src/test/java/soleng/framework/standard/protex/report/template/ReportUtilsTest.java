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
package soleng.framework.standard.protex.report.template;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.blackducksoftware.sdk.protex.policy.PolicyApi;
import com.blackducksoftware.sdk.protex.policy.ProtexSystemInformation;
import com.blackducksoftware.sdk.protex.project.ProjectApi;
import com.blackducksoftware.sdk.protex.report.Report;
import com.blackducksoftware.sdk.protex.report.ReportApi;
import com.blackducksoftware.sdk.protex.report.ReportFormat;
import com.blackducksoftware.sdk.protex.report.ReportSection;
import com.blackducksoftware.sdk.protex.report.ReportSectionType;
import com.blackducksoftware.sdk.protex.report.ReportTemplateRequest;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexAPIWrapper;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.connector.protex.report.ReportUtils;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.commonframework.test.TestProtexConfigurationManager;
import com.blackducksoftware.tools.commonframework.test.TestUtils;

/**
 * These tests make sure ReportUtils correctly generates excel reports from
 * saved reports (HTML files previously generated/saved).
 *
 * @author Steve Billings
 * @date Oct 7, 2014
 *
 */
public class ReportUtilsTest {
    private static final String MOCK_PROTEX6_VERSION_STRING = "6.0";
    private static final String PROTEX6_HTML_DIR = "protex6";
    private static final String EXPECTED_REPORT_PROTEX6 = "src/test/resources/expected_report_comprehensive_protex6_savedHtml.xlsx";

    private static final String SAVED_REPORT_DIR = "src/test/resources/savedreports";

    private static final String SERVER_NAME_PROTEX6 = "se-menger.blackducksoftware.com";

    private static final String PROTEX_USER = "unitTester@blackducksoftware.com";
    private static final String PROTEX_PASSWORD = "blackduck";

    private static final String TEMPLATE_FILE_PROTEX6 = "src/test/resources/real_excel_template_comprehensive_protex6_v02.xlsx";

    private static final String PROTEX_PROJECT_ID = "reporttest_id";
    private static final String PROJECT_NAME_PROTEX6 = "EndToEndTest";

    private static ProtexServerWrapper<ProtexProjectPojo> mockProtexServerWrapper;
    private static ProtexAPIWrapper mockApiWrapper;
    private static ReportApi mockReportApi;
    private static PolicyApi mockPolicyApi;
    private static ProjectApi mockProjectApi;

    /**
     * Create mocks for Protex
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

	// Mock wrappers and APIs
	mockProtexServerWrapper = mock(ProtexServerWrapper.class);
	mockApiWrapper = mock(ProtexAPIWrapper.class);
	mockReportApi = mock(ReportApi.class);
	mockPolicyApi = mock(PolicyApi.class);
	mockProjectApi = mock(ProjectApi.class);

	when(mockProtexServerWrapper.getInternalApiWrapper()).thenReturn(
		mockApiWrapper);
	when(mockApiWrapper.getReportApi()).thenReturn(mockReportApi);
	when(mockApiWrapper.getPolicyApi()).thenReturn(mockPolicyApi);
	when(mockApiWrapper.getProjectApi()).thenReturn(mockProjectApi);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * Test report generation from Protex 6 report HTML.
     *
     * @throws Exception
     */
    @Test
    public void testProtex6() throws Exception {
	test(MOCK_PROTEX6_VERSION_STRING, TEMPLATE_FILE_PROTEX6,
		PROTEX6_HTML_DIR, EXPECTED_REPORT_PROTEX6, PROJECT_NAME_PROTEX6);
    }

    private void test(String protexVersion, String templateFilename,
	    String htmlDir, String expectedReportFilename, String projectName)
	    throws Exception {

	// Mock the Protex server version
	ProtexSystemInformation protexInfo = new ProtexSystemInformation();
	protexInfo.setBdsServerLibraryVersion(protexVersion);
	when(mockPolicyApi.getSystemInformation()).thenReturn(protexInfo);

	// Create a mock project and tell the mock server wrapper to return it
	ProjectPojo expectedPojo = new ProtexProjectPojo(PROTEX_PROJECT_ID,
		projectName);
	when(mockProtexServerWrapper.getProjectByName(projectName)).thenReturn(
		expectedPojo);

	// Set up mocks so the Protex report HTML is read from files, not pulled
	// from Protex
	prepMockForSection(ReportSectionType.SUMMARY, htmlDir
		+ "/01_summary.html");
	prepMockForSection(
		ReportSectionType.CODE_MATCHES_PENDING_IDENTIFICATION_PRECISION,
		htmlDir + "/02_codeMatchesPendingId.html");
	prepMockForSection(ReportSectionType.STRING_SEARCH_PATTERNS, htmlDir
		+ "/03_stringSearchPatterns.html");
	prepMockForSection(ReportSectionType.CODE_MATCHES_ALL, htmlDir
		+ "/04_codeMatchesAll_reduced.html");

	prepMockForSection(ReportSectionType.ANALYSIS_SUMMARY, htmlDir
		+ "/05_analysisSummary.html");
	prepMockForSection(ReportSectionType.BILL_OF_MATERIALS, htmlDir
		+ "/06_bom.html");
	prepMockForSection(ReportSectionType.POTENTIAL_BILL_OF_MATERIALS,
		htmlDir + "/07_potentialBom.html");
	prepMockForSection(ReportSectionType.IP_ARCHITECTURE, htmlDir
		+ "/08_ipArchitecture.html");
	prepMockForSection(ReportSectionType.OBLIGATIONS, htmlDir
		+ "/09_obligations.html");
	prepMockForSection(ReportSectionType.FILE_INVENTORY, htmlDir
		+ "/10_fileInventory.html");
	prepMockForSection(ReportSectionType.IDENTIFIED_FILES, htmlDir
		+ "/11_identifiedFiles.html");
	prepMockForSection(ReportSectionType.EXCLUDED_COMPONENTS, htmlDir
		+ "/12_excludedComponents.html");
	prepMockForSection(ReportSectionType.STRING_SEARCHES, htmlDir
		+ "/13_stringSearches.html");
	prepMockForSection(ReportSectionType.STRING_SEARCH_HITS_PENDING_ID,
		htmlDir + "/14_stringSearchHitsPendingId.html");
	// 15 not used
	prepMockForSection(ReportSectionType.DEPENDENCIES_ALL, htmlDir
		+ "/16_dependenciesAll.html");
	prepMockForSection(
		ReportSectionType.DEPENDENCIES_JAVA_IMPORT_STATEMENTS, htmlDir
			+ "/17_dependenciesJavaImportStmts.html");
	prepMockForSection(
		ReportSectionType.DEPENDENCIES_JAVA_PACKAGE_STATEMENTS, htmlDir
			+ "/18_dependenciesJavaPackageStmts.html");
	prepMockForSection(ReportSectionType.DEPENDENCIES_NON_JAVA, htmlDir
		+ "/19_dependenciesNonJava.html");
	prepMockForSection(ReportSectionType.FILE_DISCOVERY_PATTERNS, htmlDir
		+ "/20_fileDiscoveryPatterns.html");
	prepMockForSection(ReportSectionType.RAPID_ID_CONFIGURATIONS, htmlDir
		+ "/21_rapidIdConfigurations.html");
	prepMockForSection(ReportSectionType.WORK_HISTORY_BILL_OF_MATERIALS,
		htmlDir + "/22_workHistoryBom.html");
	prepMockForSection(ReportSectionType.WORK_HISTORY_FILE_INVENTORY,
		htmlDir + "/23_workHistoryFileInventory.html");
	prepMockForSection(
		ReportSectionType.FILE_DISCOVERY_PATTERN_MATCHES_PENDING_IDENTIFICATION,
		htmlDir + "/23_fileDiscoveryPatternMatchesPendingId.html");
	prepMockForSection(ReportSectionType.CODE_MATCHES_PRECISION, htmlDir
		+ "/24_codeMatchesPrecision.html");
	// 25 not used
	prepMockForSection(ReportSectionType.LICENSE_TEXTS, htmlDir
		+ "/26_licenseText.html");
	prepMockForSection(ReportSectionType.CODE_LABEL, htmlDir
		+ "/27_codeLabel.html");
	prepMockForSection(ReportSectionType.COMPARE_CODE_MATCHES_PRECISION,
		htmlDir + "/28_compareCodeMatchesPrecision.html");
	prepMockForSection(ReportSectionType.COMPARE_CODE_MATCHES_ALL, htmlDir
		+ "/29_compareCodeMatchesAll.html");
	prepMockForSection(ReportSectionType.ANALYSIS_WARNINGS_AND_ERRORS,
		htmlDir + "/30_analysisWarningsAndErrors.html");
	prepMockForSection(ReportSectionType.IDENTIFICATION_AUDIT_TRAIL,
		htmlDir + "/31_identificationAuditTrail.html");
	prepMockForSection(ReportSectionType.LINK_TO_EXTERNAL_DOCUMENTS,
		htmlDir + "/32_linkToExternalDocuments.html");
	prepMockForSection(ReportSectionType.LICENSE_CONFLICTS, htmlDir
		+ "/33_licenseConflicts.html");
	prepMockForSection(ReportSectionType.LICENSES_IN_EFFECT, htmlDir
		+ "/34_licensesInEffect.html");
	prepMockForSection(
		ReportSectionType.CODE_MATCHES_PENDING_IDENTIFICATION_PRECISION,
		htmlDir + "/35_codeMatchesPendingIdPrecision.html");

	// The test: Use ReportUtils to generate report
	File templateFile = new File(templateFilename);
	ConfigurationManager config = initConfig(SERVER_NAME_PROTEX6);
	ReportUtils reportUtils = new ReportUtils();
	Workbook wb = reportUtils.getReportSectionBySection(
		mockProtexServerWrapper, projectName, templateFile, config);

	// Write the generated report to a file
	String reportFilename = TestUtils.getTempReportFilePath();
	OutputStream os = new FileOutputStream(reportFilename);
	wb.write(os);
	os.close();

	// Compare the generated file to the expected file (generated/saved
	// earlier)
	TestUtils.checkReport(expectedReportFilename, reportFilename, false,
		false);
    }

    private void prepMockForSection(ReportSectionType sectionType,
	    String htmlFilename) throws Exception {
	// Mock up a report section (HTML) by loading it from a file
	DataSource dataSource = new FileDataSource(SAVED_REPORT_DIR + "/"
		+ htmlFilename);
	DataHandler dataHandler = new DataHandler(dataSource);
	Report report = new Report();
	report.setFileContent(dataHandler);

	// Tell the report API to return the mock report section
	when(
		mockReportApi.generateAdHocProjectReport(eq(PROTEX_PROJECT_ID),
			argThat(new IsRequestForThisSection(sectionType)),
			eq(ReportFormat.HTML))).thenReturn(report);

    }

    private static ConfigurationManager initConfig(String protexServerName) {
	Properties props = new Properties();
	props.setProperty("protex.server.name", getUrl(protexServerName));
	props.setProperty("protex.user.name", PROTEX_USER);
	props.setProperty("protex.password", PROTEX_PASSWORD);
	ConfigurationManager config = new TestProtexConfigurationManager(props);
	return config;
    }

    private static String getUrl(String serverName) {
	return "http://" + serverName;
    }

    /**
     * A class for creating section-specific Mockito custom argument matchers.
     * Used to detect which report section is being processed, which is used to
     * decide which saved report HTML file should be used.
     *
     * @author Steve Billings
     * @date Oct 7, 2014
     *
     */
    class IsRequestForThisSection extends
	    ArgumentMatcher<ReportTemplateRequest> {
	private ReportSectionType expectedSectionType;

	public IsRequestForThisSection(ReportSectionType expectedSectionType) {
	    this.expectedSectionType = expectedSectionType;
	}

	@Override
	public boolean matches(Object request) {
	    ReportSection argSection = ((ReportTemplateRequest) request)
		    .getSections().get(0);
	    ReportSectionType argSectionType = argSection.getSectionType();
	    // System.out.println("IsRequestForThisSection.matches(): " +
	    // argSectionType.name() + " to " + expectedSectionType.name());
	    boolean result = argSectionType.name().equalsIgnoreCase(
		    expectedSectionType.name());
	    // System.out.println("IsRequestForThisSection.matches(): " +
	    // result);
	    return result;
	}
    }

}
