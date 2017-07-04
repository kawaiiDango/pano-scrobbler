/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.umass.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <code>DomElement</code> wraps around an {@link Element} and provides convenience methods.
 *
 * @author Janni Kovacs
 */
public class DomElement {
	private Element e;

	/**
	 * Creates a new wrapper around the given {@link Element}.
	 *
	 * @param elem An w3c Element
	 */
	public DomElement(Element elem) {
		this.e = elem;
	}

	/**
	 * @return the original Element
	 */
	public Element getElement() {
		return e;
	}

	/**
	 * Tests if this element has an attribute with the specified name.
	 *
	 * @param name Name of the attribute.
	 * @return <code>true</code> if this element has an attribute with the specified name.
	 */
	public boolean hasAttribute(String name) {
		return e.hasAttribute(name);
	}
	
	/**
	 * Returns the attribute value to a given attribute name or <code>null</code> if the attribute doesn't exist.
	 *
	 * @param name The attribute's name
	 * @return Attribute value or <code>null</code>
	 */
	public String getAttribute(String name) {
		return e.hasAttribute(name) ? e.getAttribute(name) : null;
	}

	/**
	 * @return the text content of the element
	 */
	public String getText() {
		// XXX e.getTextContent() doesn't exsist under Android (Lukasz Wisniewski)
		/// getTextContent() is now available in at least Android 2.2 if not earlier, so we'll keep using that
		// return e.hasChildNodes() ? e.getFirstChild().getNodeValue() : null;
		return e.getTextContent();
	}

	/**
	 * Checks if this element has a child element with the given name.
	 *
	 * @param name The child's name
	 * @return <code>true</code> if this element has a child element with the given name
	 */
	public boolean hasChild(String name) {
		NodeList list = e.getElementsByTagName(name);
		for (int i = 0, j = list.getLength(); i < j; i++) {
			Node item = list.item(i);
			if (item.getParentNode() == e)
				return true;
		}
		return false;
	}

	/**
	 * Returns the child element with the given name or <code>null</code> if it doesn't exist.
	 *
	 * @param name The child's name
	 * @return the child element or <code>null</code>
	 */
	public DomElement getChild(String name) {
		NodeList list = e.getElementsByTagName(name);
		if (list.getLength() == 0)
			return null;
		for (int i = 0, j = list.getLength(); i < j; i++) {
			Node item = list.item(i);
			if (item.getParentNode() == e)
				return new DomElement((Element) item);
		}
		return null;
	}

	/**
	 * Returns the text content of a child node with the given name. If no such child exists or the child
	 * does not have text content, <code>null</code> is returned.
	 *
	 * @param name The child's name
	 * @return the child's text content or <code>null</code>
	 */
	public String getChildText(String name) {
		DomElement child = getChild(name);
		return child != null ? child.getText() : null;
	}

	/**
	 * @return all children of this element
	 */
	public List<DomElement> getChildren() {
		return getChildren("*");
	}

	/**
	 * Returns all children of this element with the given tag name.
	 *
	 * @param name The children's tag name
	 * @return all matching children
	 */
	public List<DomElement> getChildren(String name) {
		List<DomElement> l = new ArrayList<DomElement>();
		NodeList list = e.getElementsByTagName(name);
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getParentNode() == e)
				l.add(new DomElement((Element) node));
		}
		return l;
	}

	/**
	 * Returns this element's tag name.
	 *
	 * @return the tag name
	 */
	public String getTagName() {
		return e.getTagName();
	}
}
