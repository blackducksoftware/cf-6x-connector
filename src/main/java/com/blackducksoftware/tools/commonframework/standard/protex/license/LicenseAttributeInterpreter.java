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
package com.blackducksoftware.tools.commonframework.standard.protex.license;

import com.blackducksoftware.sdk.protex.license.LicenseAttributes;
import com.blackducksoftware.sdk.protex.license.LicenseExtensionLevel;
import com.blackducksoftware.sdk.protex.license.PermittedOrRequired;
import com.blackducksoftware.sdk.protex.license.RestrictionType;
import com.blackducksoftware.sdk.protex.license.RightToDistributeBinaryForMaximumUsage;

// TODO: Auto-generated Javadoc
/**
 * Provides the description and text representation of a license.
 * 
 * @author sbillings
 *
 */
public enum LicenseAttributeInterpreter {

    /** The right to distribute binary. */
    RIGHT_TO_DISTRIBUTE_BINARY("Right to Distribute Binary"),

    /** The carries distribution obligations. */
    CARRIES_DISTRIBUTION_OBLIGATIONS("Carries Distribution Obligations"),

    /** The source code distribution. */
    SOURCE_CODE_DISTRIBUTION("Source Code Distribution"),

    /** The right to copy. */
    RIGHT_TO_COPY("Right to Copy"),

    /** The right to modify. */
    RIGHT_TO_MODIFY("Right to Modify"),

    /** The right to reverse engineer. */
    RIGHT_TO_REVERSE_ENGINEER("Right to Reverse Engineer"),

    /** The discriminatory restrictions. */
    DISCRIMINATORY_RESTRICTIONS("Discriminatory Restrictions"),

    /** The charging fees. */
    CHARGING_FEES("Charging Fees"),

    /** The patent retaliation. */
    PATENT_RETALIATION("Patent Retaliation"),

    /** The express patent license. */
    EXPRESS_PATENT_LICENSE("Express Patent License"),

    /** The anti drm provision. */
    ANTI_DRM_PROVISION("Anti DRM Provision"),

    /** The notice required. */
    NOTICE_REQUIRED("Notice Required"),

    /** The change notice. */
    CHANGE_NOTICE("Change Notice"),

    /** The license back. */
    LICENSE_BACK("License Back"),

    /** The warranty disclaimer. */
    WARRANTY_DISCLAIMER("Warranty Disclaimer"),

    /** The limitation of liability. */
    LIMITATION_OF_LIABILITY("Limitation of Liability"),

    /** The indemnification obligation. */
    INDEMNIFICATION_OBLIGATION("Indemnification Obligation"),

    /** The include license. */
    INCLUDE_LICENSE("Include License"),

    /** The promotion restriction. */
    PROMOTION_RESTRICTION("Promotion Restriction"),

    /** The reciprocity. */
    RECIPROCITY("Reciprocity"),

    /** The integration level. */
    INTEGRATION_LEVEL("Integration Level");

    /** The description. */
    private String description;

    /**
     * Instantiates a new license attribute interpreter.
     *
     * @param description
     *            the description
     */
    private LicenseAttributeInterpreter(String description) {
	this.description = description;
    }

    /**
     * Get the description of a license.
     *
     * @return the description
     */
    public String getDescription() {
	return description;
    }

    /**
     * Get a text representation of the value of a license attribute.
     *
     * @param licAttrs
     *            the lic attrs
     * @return the text value
     */
    public String getTextValue(LicenseAttributes licAttrs) {
	String textValue = "<unknown>";

	if (this.name().equals("RIGHT_TO_DISTRIBUTE_BINARY")) {
	    textValue = getRightToDistributeBinaryForMaximumUsageAttributeText(licAttrs);
	} else if (this.name().equals("CARRIES_DISTRIBUTION_OBLIGATIONS")) {
	    textValue = licAttrs.isCarriesDistributionObligations() ? "T" : "F";
	} else if (this.name().equals("SOURCE_CODE_DISTRIBUTION")) {
	    textValue = this.getPermOrReqAttributeText(licAttrs
		    .getSourceCodeDistribution());
	} else if (this.name().equals("RIGHT_TO_COPY")) {
	    textValue = this.getPermOrReqAttributeText(licAttrs
		    .getGrantRecipientRightToCopy());
	} else if (this.name().equals("RIGHT_TO_MODIFY")) {
	    textValue = this.getPermOrReqAttributeText(licAttrs
		    .getGrantRecipientRightToModify());
	} else if (this.name().equals("RIGHT_TO_REVERSE_ENGINEER")) {
	    textValue = this.getPermOrReqAttributeText(licAttrs
		    .getGrantRecipientRightToReverseEngineer());
	} else if (this.name().equals("DISCRIMINATORY_RESTRICTIONS")) {
	    textValue = this.getRestrictionTypeAttributeText(licAttrs
		    .getDiscriminatoryRestrictions());
	} else if (this.name().equals("CHARGING_FEES")) {
	    textValue = this.getPermOrReqAttributeText(licAttrs
		    .getChargingFees());
	} else if (this.name().equals("PATENT_RETALIATION")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isPatentRetaliation());
	} else if (this.name().equals("EXPRESS_PATENT_LICENSE")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isExpressPatentLicense());
	} else if (this.name().equals("ANTI_DRM_PROVISION")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isAntiDrmProvision());
	} else if (this.name().equals("NOTICE_REQUIRED")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs.isNotice());
	} else if (this.name().equals("CHANGE_NOTICE")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isChangeNoticeRequired());
	} else if (this.name().equals("LICENSE_BACK")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isLicenseBackRequired());
	} else if (this.name().equals("WARRANTY_DISCLAIMER")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isWarrantyDisclaimerRequired());
	} else if (this.name().equals("LIMITATION_OF_LIABILITY")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isLimitationOfLiabilityRequired());
	} else if (this.name().equals("INDEMNIFICATION_OBLIGATION")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isIndemnificationRequired());
	} else if (this.name().equals("INCLUDE_LICENSE")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isIncludeLicense());
	} else if (this.name().equals("PROMOTION_RESTRICTION")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isPromotionRestriction());
	} else if (this.name().equals("RECIPROCITY")) {
	    textValue = this.getTrueFalseAttributeText(licAttrs
		    .isShareAlikeReciprocity());
	} else if (this.name().equals("INTEGRATION_LEVEL")) {
	    textValue = this.getLicenseExtensionLevelText(licAttrs
		    .getIntegrationLevelForLicenseApplication());
	}
	return textValue;
    }

    /**
     * Gets the right to distribute binary for maximum usage attribute text.
     *
     * @param licAttrs
     *            the lic attrs
     * @return the right to distribute binary for maximum usage attribute text
     */
    private String getRightToDistributeBinaryForMaximumUsageAttributeText(
	    LicenseAttributes licAttrs) {

	RightToDistributeBinaryForMaximumUsage attr = licAttrs
		.getRightToDistributeBinaryForMaximumUsage();
	if (attr == RightToDistributeBinaryForMaximumUsage.ANY) {
	    return "ANY";
	} else if (attr == RightToDistributeBinaryForMaximumUsage.INTERNAL_EVALUATION) {
	    return "INTERNAL EVALUATION";
	} else if (attr == RightToDistributeBinaryForMaximumUsage.INTERNAL_PRODUCTION_USE) {
	    return "INTERNAL PRODUCTION USE";
	} else if (attr == RightToDistributeBinaryForMaximumUsage.NON_COMMERCIAL_OR_PERSONAL_USE) {
	    return "NON-COMMERCIAL OR PERSONAL USE";
	} else { // unrecognized
	    return "UNKNOWN_VALUE";
	}
    }

    /**
     * Gets the perm or req attribute text.
     *
     * @param permOrReq
     *            the perm or req
     * @return the perm or req attribute text
     */
    private String getPermOrReqAttributeText(PermittedOrRequired permOrReq) {
	if (permOrReq == PermittedOrRequired.NOT_PERMITTED) {
	    return "NOT_PERMITED";
	} else if (permOrReq == PermittedOrRequired.PERMITTED) {
	    return "PERMITED";
	} else if (permOrReq == PermittedOrRequired.REQUIRED) {
	    return "REQUIRED";
	} else {
	    return "UNKNOWN_VALUE";
	}
    }

    /**
     * Gets the restriction type attribute text.
     *
     * @param restrictionType
     *            the restriction type
     * @return the restriction type attribute text
     */
    private String getRestrictionTypeAttributeText(
	    RestrictionType restrictionType) {
	if (restrictionType == RestrictionType.HAS_NO_RESTRICTIONS) {
	    return "HAS_NO_RESTRICTIONS";
	} else if (restrictionType == RestrictionType.HAS_NO_RESTRICTIONS_AND_CAN_NOT_ADD_ANY) {
	    return "HAS_NO_RESTRICTIONS_AND_CAN_NOT_ADD_ANY";
	} else if (restrictionType == RestrictionType.HAS_RESTRICTIONS) {
	    return "HAS_RESTRICTIONS";
	} else {
	    return "UNKNOWN_VALUE";
	}
    }

    /**
     * Gets the true false attribute text.
     *
     * @param attributeValue
     *            the attribute value
     * @return the true false attribute text
     */
    private String getTrueFalseAttributeText(boolean attributeValue) {
	return attributeValue ? "T" : "F";
    }

    /**
     * Gets the license extension level text.
     *
     * @param licenseExtensionLevel
     *            the license extension level
     * @return the license extension level text
     */
    private String getLicenseExtensionLevelText(
	    LicenseExtensionLevel licenseExtensionLevel) {
	if (licenseExtensionLevel == LicenseExtensionLevel.ACCOMPANYING_SOFTWARE_USING_PER_SLEEPY_CAT) {
	    return "ACCOMPANYING_SOFTWARE_USING_PER_SLEEPY_CAT";
	} else if (licenseExtensionLevel == LicenseExtensionLevel.DYNAMIC_LIBRARY_PER_LGPL) {
	    return "DYNAMIC_LIBRARY_PER_LGPL";
	} else if (licenseExtensionLevel == LicenseExtensionLevel.FILE_PER_MPL) {
	    return "FILE_PER_MPL";
	} else if (licenseExtensionLevel == LicenseExtensionLevel.NON) {
	    return "NON";
	} else if (licenseExtensionLevel == LicenseExtensionLevel.WORK_BASED_ON_PER_GPL) {
	    return "WORK_BASED_ON_PER_GPL";
	} else if (licenseExtensionLevel == LicenseExtensionLevel.MODULE_PER_EPL_CPL) {
	    return "MODULE_PER_EPL_CPL";
	} else {
	    return "UNKNOWN_VALUE";
	}
    }
}
