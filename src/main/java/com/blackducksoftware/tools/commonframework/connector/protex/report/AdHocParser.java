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

import java.util.ArrayList;

import org.jsoup.nodes.Document;

import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.report.HocElement;
import com.blackducksoftware.tools.commonframework.standard.protex.report.IAdHocParser;

/**
 * The main parsing class for the AdHoc SDK Report sections.
 *
 * @author sbillings
 * @param <T>
 *            the generic type
 */
public class AdHocParser<T extends HocElement> implements IAdHocParser<T> {

    private IAdHocParser<T> actualParser;

    /**
     * Instantiates a new ad hoc parser.
     */
    public AdHocParser(
	    ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper)
	    throws Exception {
	actualParser = new AdHocParserProtex6<T>();
    }

    /**
     * Parses the headers from doc.
     *
     * @param doc
     *            the doc
     * @return the hoc element
     * @throws Exception
     *             the exception
     */
    @Override
    public HocElement parseHeadersFromDoc(Document doc, int targetSectionIndex)
	    throws Exception {
	return actualParser.parseHeadersFromDoc(doc, targetSectionIndex);
    }

    /**
     * Parse report section table rows from the Protex report, returning the
     * data in a list of HocElements. nonConforming = report sections of the
     * form: Label: value. Normal (conforming) = report sections that contain
     * columns of data, each with a header (label) at the top (first row).
     *
     * @param doc
     *            the doc
     * @param adHocHeader
     *            the ad hoc header
     * @param returnRawHtml
     *            the return raw html
     * @param hocElementClass
     *            the hoc element class
     * @return the array list
     * @throws Exception
     */
    @Override
    public ArrayList<T> parseRows(Document doc,
	    HocElement adHocHeader, boolean returnRawHtml,
	    Class<T> hocElementClass, int targetSectionIndex) throws Exception 
 {
	return actualParser.parseRows(doc, adHocHeader, returnRawHtml,
		hocElementClass, targetSectionIndex);
    }
}
