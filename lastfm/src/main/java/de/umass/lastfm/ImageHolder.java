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

import java.util.*;

import de.umass.xml.DomElement;

/**
 * Abstract superclass for all items that may contain images (such as {@link Artist}s, {@link Album}s or {@link Track}s).
 *
 * @author Janni Kovacs
 */
public abstract class ImageHolder {

	protected Map<ImageSize, String> imageUrls = new HashMap<ImageSize, String>();

	/**
	 * Returns a Set of all {@link ImageSize}s available.
	 *
	 * @return all sizes
	 */
	public Set<ImageSize> availableSizes() {
		return imageUrls.keySet();
	}

	/**
	 * Returns the URL of the image in the specified size, or <code>null</code> if not available.
	 *
	 * @param size The preferred size
	 * @return an image URL
	 */
	public String getImageURL(ImageSize size) {
		return imageUrls.get(size);
	}

	protected static void loadImages(ImageHolder holder, DomElement element) {
		Collection<DomElement> images = element.getChildren("image");
		for (DomElement image : images) {
			String attribute = image.getAttribute("size");
			ImageSize size = null;
			if (attribute == null) {
				size = ImageSize.MEDIUM; // workaround for image responses without size attr.
			} else {
				try {
					size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH));
				} catch (IllegalArgumentException e) {
					// if they suddenly again introduce a new image size
				}
			}
			if (size != null)
				holder.imageUrls.put(size, image.getText());
		}
	}
}
