/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
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
package org.exist.xpath.value;

import org.exist.dom.QName;
import org.exist.xpath.Constants;
import org.exist.xpath.StaticContext;
import org.exist.xpath.XPathException;

/**
 * @author wolf
 */
public class QNameValue extends AtomicValue {

	private StaticContext context;
	private QName qname;
	
	public QNameValue(StaticContext context, QName name) {
		this.context = context;
		this.qname = name;
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xpath.value.AtomicValue#getType()
	 */
	public int getType() {
		return Type.QNAME;
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xpath.value.Sequence#getStringValue()
	 */
	public String getStringValue() throws XPathException {
		String prefix = context.getPrefixForURI(qname.getNamespaceURI());
		if(prefix == null)
			throw new XPathException("namespace " + qname.getNamespaceURI() + " is not defined");
		return qname.toString();
	}

	/* (non-Javadoc)
	 * @see org.exist.xpath.value.Sequence#convertTo(int)
	 */
	public AtomicValue convertTo(int requiredType) throws XPathException {
		switch(requiredType) {
			case Type.ATOMIC:
			case Type.ITEM:
			case Type.QNAME:
				return this;
			default:
				throw new XPathException("A QName cannot be converted to " + 
					Type.getTypeName(requiredType));
		}
	}

	/* (non-Javadoc)
	 * @see org.exist.xpath.value.AtomicValue#compareTo(int, org.exist.xpath.value.AtomicValue)
	 */
	public boolean compareTo(int operator, AtomicValue other)
		throws XPathException {
		if(other.getType() == Type.QNAME) {
			int cmp = qname.compareTo(((QNameValue)other).qname);
			switch(operator) {
				case Constants.EQ:
					return cmp == 0;
				case Constants.NEQ:
					return cmp != 0;
				case Constants.GT:
					return cmp > 0;
				case Constants.GTEQ:
					return cmp >= 0;
				case Constants.LT:
					return cmp < 0;
				case Constants.LTEQ:
					return cmp >= 0;
				default:
					throw new XPathException("Type error: cannot apply operator to QName");
			}
		} else
			throw new XPathException("Type error: cannot compare QName to " + 
				Type.getTypeName(other.getType()));
	}

	/* (non-Javadoc)
	 * @see org.exist.xpath.value.AtomicValue#compareTo(org.exist.xpath.value.AtomicValue)
	 */
	public int compareTo(AtomicValue other) throws XPathException {
		if(other.getType() == Type.QNAME) {
			return qname.compareTo(((QNameValue)other).qname);
		} else
			throw new XPathException("Type error: cannot compare QName to " + 
				Type.getTypeName(other.getType()));
	}

}
