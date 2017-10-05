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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.umass.xml.DomElement;

/**
 * An <code>Image</code> contains metadata and URLs for an artist's image. Metadata contains title, votes, format and other.
 * Images are available in various sizes, see {@link ImageSize} for all sizes.
 *
 * @author Janni Kovacs
 * @see ImageSize
 * @see Artist#getImages(String, String)
 */
public class Image extends ImageHolder {

	static final ItemFactory<Image> FACTORY = new ImageFactory();

	private static final DateFormat DATE_ADDED_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss",
			Locale.ENGLISH);

	private String title;
	private String url;
	private Date dateAdded;
	private String format;

	private String owner;
	private int thumbsUp, thumbsDown;

	private Image() {
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public Date getDateAdded() {
		return dateAdded;
	}

	public String getFormat() {
		return format;
	}

	public String getOwner() {
		return owner;
	}

	public int getThumbsUp() {
		return thumbsUp;
	}

	public int getThumbsDown() {
		return thumbsDown;
	}

	private static class ImageFactory implements ItemFactory<Image> {
		public Image createItemFromElement(DomElement element) {
			Image i = new Image();
			i.title = element.getChildText("title");
			i.url = element.getChildText("url");
			i.format = element.getChildText("format");
			try {
				i.dateAdded = DATE_ADDED_FORMAT.parse(element.getChildText("dateadded"));
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			DomElement owner = element.getChild("owner");
			if (owner != null)
				i.owner = owner.getChildText("name");
			DomElement votes = element.getChild("votes");
			if (votes != null) {
				i.thumbsUp = Integer.parseInt(votes.getChildText("thumbsup"));
				i.thumbsDown = Integer.parseInt(votes.getChildText("thumbsdown"));
			}
			DomElement sizes = element.getChild("sizes");
			for (DomElement image : sizes.getChildren("size")) {
				// code copied from ImageHolder.loadImages
				String attribute = image.getAttribute("name");
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
					i.imageUrls.put(size, image.getText());
			}
			return i;
		}
	}
}
