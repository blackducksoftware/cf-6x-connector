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
package com.blackducksoftware.tools.commonframework.connector.protex.report;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.report.HocElement;

/**
 * Parser for Protex HTML reports, used to extract report data from them.
 *
 * @author sbillings
 *
 */
public class ProtexReportHTMLProcessor {
    private static HocElement header = null;
    private static Document doc = null;

    /**
     * Gets the rows from buffer.
     *
     * @param <T>
     *            the generic type
     * @param returnRawHtml
     *            the return raw html
     * @param lnr
     *            the lnr
     * @param hocElementClass
     *            the hoc element class
     * @return the rows from buffer
     * @throws Exception
     *             the exception
     */
    public static <T extends HocElement> List<T> getRowsFromBuffer(
	    ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper,
	    boolean returnRawHtml, LineNumberReader lnr,
	    Class<T> hocElementClass) throws Exception {
	List<T> parsedRows = new ArrayList<T>();

	try {
	    StringBuffer buf = new StringBuffer();
	    String line = "";
	    String htmlBody = buf.toString();

	    while ((line = lnr.readLine()) != null) {
		buf.append(line);
	    }

	    htmlBody = buf.toString();

	    /**
	     * HACK TIME. Unfortunately while this looks messy, it is relatively
	     * harmless. Because JSoup insists on stripping out HTML tags for
	     * chunks of html it invariably breaks our entire parsing scheme. By
	     * wrapping each block in a known table tag we ensure that nothing
	     * gets stripped out. --AK
	     */
	    htmlBody = "<tbody><table class='reportTable'>" + htmlBody
		    + "</table></tbody>";

	    // Parse the number of HTML lines that we were able to get.
	    Document doc = Jsoup.parseBodyFragment(htmlBody);

	    buf = null;

	    AdHocParser<T> adHocParser = new AdHocParser<T>(protexServerWrapper);

	    // To enable this method to be called multiple times for different
	    // report sections, the header
	    // is initialized each time.
	    // If we develop the ability to generate multiple sections in
	    // one shot, might be able to go
	    // back to only initializing header if it's null
	    header = adHocParser.parseHeadersFromDoc(doc, 0);

	    /**
	     * Sometimes we want to keep the HTML intact, so lets stuff the
	     * datablock with the raw html and send it.
	     */
	    if (returnRawHtml) {
		parsedRows = adHocParser.parseRows(doc, header, returnRawHtml,
			hocElementClass, 0);
		return parsedRows;
	    }

	    parsedRows = adHocParser.parseRows(doc, header, false,
		    hocElementClass, 0);

	} catch (Exception e) {
	    throw new Exception("Error processing file chunk!", e);
	} finally {

	}
	return parsedRows;
    }

    /**
     * Initialize the parser for parsing multi-section reports.
     *
     * @param lnr
     * @param hocElementClass
     * @throws Exception
     */
    public static <T extends HocElement> void init(LineNumberReader lnr,
	    Class<T> hocElementClass) throws Exception {
	try {
	    String htmlBody = getHtmlBody(lnr);

	    /**
	     * HACK TIME. Unfortunately while this looks messy, it is relatively
	     * harmless. Because JSoup insists on stripping out HTML tags for
	     * chunks of html it invariably breaks our entire parsing scheme. By
	     * wrapping each block in a known table tag we ensure that nothing
	     * gets stripped out. --AK
	     */
	    htmlBody = "<tbody><table class='reportTable'>" + htmlBody
		    + "</table></tbody>";

	    // Parse the number of HTML lines that we were able to get.
	    doc = Jsoup.parseBodyFragment(htmlBody);

	} catch (Exception e) {
	    throw new Exception("Error processing file chunk! "
		    + e.getMessage(), e);
	} finally {

	}

    }

    /**
     * Get a given report section's worth of data (while parsing multi-section
     * reports).
     *
     * @param hocElementClass
     * @param targetSectionIndex
     * @param returnRawHtml
     * @return
     * @throws Exception
     */
    public static <T extends HocElement> List<T> getReportSectionData(
	    ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper,
	    Class<T> hocElementClass, int targetSectionIndex,
	    boolean returnRawHtml) throws Exception {
	AdHocParser<T> parser = new AdHocParser<T>(protexServerWrapper);

	// To enable this method to be called multiple times for different
	// report sections, the header
	// is initialized each time.
	header = parser.parseHeadersFromDoc(doc, targetSectionIndex);

	List<T> parsedRows = new ArrayList<T>();

	/**
	 * Sometimes we want to keep the HTML intact, so lets stuff the
	 * datablock with the raw html and send it.
	 */
	if (returnRawHtml) {
	    parsedRows = parser.parseRows(doc, header, returnRawHtml,
		    hocElementClass, targetSectionIndex);
	    return parsedRows;
	}

	parsedRows = parser.parseRows(doc, header, false, hocElementClass,
		targetSectionIndex);
	return parsedRows;
    }

    private static String getHtmlBody(LineNumberReader lnr) throws IOException {
	StringBuffer buf = new StringBuffer();
	String line = "";
	String htmlBody = buf.toString();

	while ((line = lnr.readLine()) != null) {
	    buf.append(line);
	}

	htmlBody = buf.toString();
	return htmlBody;
    }

}
