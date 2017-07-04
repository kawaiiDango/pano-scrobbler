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

package de.umass.lastfm;

import de.umass.xml.DomElement;

/**
 * A <code>BuyLink</code> contains information about places to buy an Album or Track. BuyLinks can point to physical
 * and digital music stores. Some suppliers have icons, some do have price information, others don't (eBay for example).
 * Common suppliers you will receive via the <code>getBuylinks()</code> methods are Amazon, Amazon MP3, iTunes and
 * 7digital. All stores but eBay do supply icons at the time of writing.
 *
 * @author Janni Kovacs
 * @see Album#getBuylinks(String, String, String, String)
 * @see Track#getBuylinks(String, String, String, String)
 */
public class BuyLink {

	public static enum StoreType {
		PHYSICAl,
		DIGITAL
	}

	private StoreType type;
	private String name;
	private String link;
	private String icon;
	private boolean search;

	private String currency;
	private double price;

	private BuyLink(String name, StoreType type, String link) {
		this.name = name;
		this.type = type;
		this.link = link;
	}

	public String getName() {
		return name;
	}

	public String getLink() {
		return link;
	}

	public StoreType getType() {
		return type;
	}

	/**
	 * Returns a url to a 16x16 pixel icon for the store, or <code>null</code> if no icon url was supplied.
	 *
	 * @return Icon URL or <code>null</code>
	 */
	public String getIcon() {
		return icon;
	}

	/**
	 * Returns <code>true</code> if this link points to a search page instead of an actual product page. Note that
	 * for search links there is no price information available.
	 *
	 * @return if this is a search link
	 */
	public boolean isSearch() {
		return search;
	}

	/**
	 * Returns the currency of the price of the item. Check if this is <code>null</code> to double-check if there is
	 * price information available
	 *
	 * @return currency
	 */
	public String getCurrency() {
		return currency;
	}

	/**
	 * Returns the price for the item, or 0.0 if no price information is available. Use {@link #getCurrency()} and
	 * {@link #isSearch()} to check if price information is available.
	 *
	 * @return price, if available
	 */
	public double getPrice() {
		return price;
	}

	static BuyLink linkFromElement(StoreType type, DomElement element) {
		BuyLink link = new BuyLink(element.getChildText("supplierName"), type, element.getChildText("buyLink"));
		link.search = "1".equals(element.getChildText("isSearch"));
		link.icon = element.getChildText("supplierIcon");
		if (link.icon != null && link.icon.length() == 0)
			link.icon = null;
		if (element.hasChild("price")) {
			DomElement child = element.getChild("price");
			link.currency = child.getChildText("currency");
			link.price = Double.parseDouble(child.getChildText("amount"));
		}
		return link;
	}
}
