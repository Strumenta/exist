/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2004-09 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  $Id$
 */
package org.exist.xquery.functions.util;

import org.exist.ExistSystemProperties;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;

import java.util.HashSet;
import java.util.Set;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.functions.util.UtilModule.functionSignature;

/**
 * Library function to retrieve the value of a system property.
 *
 * @author Wolfgang Meier
 * @author Loren Cahlander
 */
public class SystemProperty extends BasicFunction {

    private final static String FS_AVAILABLE_SYSTEM_PROPERTIES_NAME = "available-system-properties";
    public final static FunctionSignature FS_AVAILABLE_SYSTEM_PROPERTIES = functionSignature(
            FS_AVAILABLE_SYSTEM_PROPERTIES_NAME,
            "Returns a list of available system properties. " +
                    "Predefined properties are: vendor, vendor-url, product-name, product-version, product-build, and all Java " +
                    "System Properties.",
            returnsOptMany(Type.STRING, "The names of the available system properties")
    );

    private final static String FS_SYSTEM_PROPERTY_NAME = "system-property";
    public final static FunctionSignature FS_SYSTEM_PROPERTY = functionSignature(
            FS_SYSTEM_PROPERTY_NAME,
            "Returns the value of a system property. Similar to the corresponding XSLT function. " +
                    "Predefined properties are: vendor, vendor-url, product-name, product-version, product-build, and all Java " +
                    "System Properties.",
            returnsOpt(Type.STRING, "the value of the named system property"),
            param("property-name", Type.STRING, "The name of the system property to retrieve the value of.")
    );

    public SystemProperty(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        if (isCalledAs(FS_AVAILABLE_SYSTEM_PROPERTIES_NAME)) {

            final Set<String> availableProperties = new HashSet<>();
            availableProperties.addAll(ExistSystemProperties.getInstance().getAvailableExistSystemProperties());
            availableProperties.addAll(context.getJavaSystemProperties().keySet());

            final ValueSequence result = new ValueSequence(availableProperties.size());
            for (final String availableProperty : availableProperties) {
                result.add(new StringValue(availableProperty));
            }

            return result;

        } else {
            final String key = args[0].getStringValue();
            String value = ExistSystemProperties.getInstance().getExistSystemProperty(key, null);
            if (value == null) {
                value = context.getJavaSystemProperties().get(key);
            }
            return value == null ? Sequence.EMPTY_SEQUENCE : new StringValue(value);
        }
    }
}