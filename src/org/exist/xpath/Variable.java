/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
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
package org.exist.xpath;

import org.exist.dom.QName;
import org.exist.xpath.value.Sequence;

/**
 * An XQuery/XPath variable, consisting of a QName and a value.
 * 
 * @author wolf
 */
public class Variable {

	private QName qname;
	private Sequence value = null;
	
	/**
	 * 
	 */
	public Variable(QName qname) {
		this.qname = qname;
	}

	public void setValue(Sequence value) {
		this.value = value;
	}
	
	public Sequence getValue() {
		return value;
	}
	
	public QName getQName() {
		return qname;
	}
	
	public String toString() {
		return "$" + qname.toString();
	}
}
