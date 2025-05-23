<?xml version="1.0" encoding="UTF-8"?>
<!--
           

        Copyright 1990-2006 Sun Microsystems, Inc. All Rights Reserved.
        DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
-->
<!--
    This stylesheet generates source code of com.sun.cdc.config.PackageManager
    class. Output code is intended for temporary use only, until real
    PackageManager with capability of enumerating dynamically loaded
    packages is implemented.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- stylesheet parameter: space separated list of property initialization classes -->
<xsl:param name="propertyInitializers"></xsl:param>
<xsl:output method="text"/>


<xsl:template match="/">
<xsl:text>/*
 *
 *
 * Copyright 1990-2006 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 */

/*
 * This file is automatically generated. Do not edit.
 */

package com.sun.cdc.config;

public class PackageManager {
    static final String[] components = {
</xsl:text>

    <xsl:element name="configuration">
        <!-- process propertyInitializers if it isn't empty -->
        <xsl:if test="boolean($propertyInitializers)">
            <xsl:call-template name="addClasses">
                <xsl:with-param name="classesList" select="$propertyInitializers"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:element>

<xsl:text>    };

    public static String[] listComponents() {
        return components;
    }
}
</xsl:text>
</xsl:template>

<!-- list the classes as a String[] array -->
<xsl:template name="addClasses">
<!-- template parameter: space separated list of classes -->
<xsl:param name="classesList"/>
<!-- add first class name to the array -->
<xsl:text>        "</xsl:text>
    <xsl:choose>
        <!-- when there is more than one element in the list -->
        <xsl:when test="contains($classesList,' ')">
            <xsl:value-of select="substring-before($classesList,' ')"/>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$classesList"/>
        </xsl:otherwise>
    </xsl:choose>
<xsl:text>",
</xsl:text>
<!-- and call this template recursively to process the rest of class names -->
    <xsl:if test="contains($classesList,' ')">
        <xsl:call-template name="addClasses">
            <xsl:with-param name="classesList" select="substring-after($classesList,' ')"/>
        </xsl:call-template>
    </xsl:if>
</xsl:template>

</xsl:stylesheet>
