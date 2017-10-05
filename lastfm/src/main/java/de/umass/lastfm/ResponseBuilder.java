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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.umass.xml.DomElement;

/**
 * This utility class can be used to generically generate Result objects (usually Lists or {@link PaginatedResult}s) from an XML response
 * using {@link ItemFactory ItemFactories}.
 *
 * @author Janni Kovacs
 */
public final class ResponseBuilder {

	private ResponseBuilder() {
	}

	private static <T> ItemFactory<T> getItemFactory(Class<T> itemClass) {
		return ItemFactoryBuilder.getFactoryBuilder().getItemFactory(itemClass);
	}

	public static <T> Collection<T> buildCollection(Result result, Class<T> itemClass) {
		return buildCollection(result, getItemFactory(itemClass));
	}

	public static <T> Collection<T> buildCollection(Result result, ItemFactory<T> factory) {
		if (!result.isSuccessful())
			return Collections.emptyList();
		return buildCollection(result.getContentElement(), factory);
	}

	public static <T> Collection<T> buildCollection(DomElement element, Class<T> itemClass) {
		return buildCollection(element, getItemFactory(itemClass));
	}

	public static <T> Collection<T> buildCollection(DomElement element, ItemFactory<T> factory) {
		if (element == null)
			return Collections.emptyList();
		Collection<DomElement> children = element.getChildren();
		Collection<T> items = new ArrayList<T>(children.size());
		for (DomElement child : children) {
			items.add(factory.createItemFromElement(child));
		}
		return items;
	}

	public static <T> PaginatedResult<T> buildPaginatedResult(Result result, Class<T> itemClass) {
		return buildPaginatedResult(result, getItemFactory(itemClass));
	}

	public static <T> PaginatedResult<T> buildPaginatedResult(Result result, ItemFactory<T> factory) {
		if (!result.isSuccessful()) {
			return new PaginatedResult<T>(0, 0, Collections.<T>emptyList());
		}

		DomElement contentElement = result.getContentElement();
		return buildPaginatedResult(contentElement, contentElement, factory);
	}

	public static <T> PaginatedResult<T> buildPaginatedResult(DomElement contentElement, DomElement childElement, Class<T> itemClass) {
		return buildPaginatedResult(contentElement, childElement, getItemFactory(itemClass));
	}

	public static <T> PaginatedResult<T> buildPaginatedResult(DomElement contentElement, DomElement childElement, ItemFactory<T> factory) {
		Collection<T> items = buildCollection(childElement, factory);

		String totalPagesAttribute = contentElement.getAttribute("totalPages");
		if (totalPagesAttribute == null)
			totalPagesAttribute = contentElement.getAttribute("totalpages");

		int page = Integer.parseInt(contentElement.getAttribute("page"));
		int totalPages = Integer.parseInt(totalPagesAttribute);

		return new PaginatedResult<T>(page, totalPages, items);
	}

	public static <T> T buildItem(Result result, Class<T> itemClass) {
		return buildItem(result, getItemFactory(itemClass));
	}

	public static <T> T buildItem(Result result, ItemFactory<T> factory) {
		if (!result.isSuccessful())
			return null;
		return buildItem(result.getContentElement(), factory);
	}

	public static <T> T buildItem(DomElement element, Class<T> itemClass) {
		return buildItem(element, getItemFactory(itemClass));
	}

	private static <T> T buildItem(DomElement element, ItemFactory<T> factory) {
		return factory.createItemFromElement(element);
	}
}
