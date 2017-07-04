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
import java.util.*;

import de.umass.util.StringUtilities;
import de.umass.xml.DomElement;

/**
 * Bean for Tag data and provides methods for global tags.
 *
 * @author Janni Kovacs
 */
public class Tag implements Comparable<Tag> {

	/**
	 * Implementation of {@link ItemFactory} for this class
	 */
	static final ItemFactory<Tag> FACTORY = new TagFactory();

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ", Locale.ENGLISH);

	private String name;
	private String url;
	private int count;

	private boolean streamable;
	private int reach;

	private Date wikiLastChanged;
	private String wikiSummary;
	private String wikiText;

	private Tag(String name) {
		this.name = name;
	}

	public int getCount() {
		return count;
	}

	/**
	 * Returns the number of taggings of this specific tag. Alias for {@link #getCount()}.
	 *
	 * @return Number of Taggings
	 * @see Tag#getInfo(String, String)
	 */
	public int getTaggings() {
		return count;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public boolean isStreamable() {
		return streamable;
	}

	public int getReach() {
		return reach;
	}

	public Date getWikiLastChanged() {
		return wikiLastChanged;
	}

	public String getWikiSummary() {
		return wikiSummary;
	}

	public String getWikiText() {
		return wikiText;
	}

	/**
	 * Returns the sum of all <code>count</code> elements in the results.
	 *
	 * @param tags a list of tags
	 * @return the total count of all tags
	 */
	public static long getTagCountSum(Collection<Tag> tags) {
		long total = 0;
		for (Tag topTag : tags) {
			total += topTag.count;
		}
		return total;
	}

	/**
	 * Filters tags from the given list; retains only those tags with a count
	 * higher than the given percentage of the total sum as from
	 * {@link #getTagCountSum(Collection)}.
	 *
	 * @param tags list of tags
	 * @param percentage cut off percentage
	 * @return the filtered list of tags
	 */
	public static List<Tag> filter(Collection<Tag> tags, double percentage) {
		ArrayList<Tag> tops = new ArrayList<Tag>();
		long total = getTagCountSum(tags);
		double cutOff = total / 100.0 * percentage;
		for (Tag tag : tags) {
			if (tag.count > cutOff) {
				tops.add(tag);
			}
		}
		return tops;
	}

	/**
	 * Search for tags similar to this one. Returns tags ranked by similarity, based on listening data.
	 *
	 * @param tag The tag name
	 * @param apiKey A Last.fm API key
	 * @return a List of <code>Tag</code>s
	 */
	public static Collection<Tag> getSimilar(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getSimilar", apiKey, "tag", tag);
		return ResponseBuilder.buildCollection(result, Tag.class);
	}

	public static Collection<Tag> getTopTags(String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopTags", apiKey);
		return ResponseBuilder.buildCollection(result, Tag.class);
	}

	public static Collection<Album> getTopAlbums(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopAlbums", apiKey, "tag", tag);
		return ResponseBuilder.buildCollection(result, Album.class);
	}

	public static Collection<Track> getTopTracks(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopTracks", apiKey, "tag", tag);
		return ResponseBuilder.buildCollection(result, Track.class);
	}

	public static Collection<Artist> getTopArtists(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopArtists", apiKey, "tag", tag);
		return ResponseBuilder.buildCollection(result, Artist.class);
	}

	public static Collection<Tag> search(String tag, String apiKey) {
		return search(tag, 30, apiKey);
	}

	public static Collection<Tag> search(String tag, int limit, String apiKey) {
		Result result = Caller.getInstance().call("tag.search", apiKey, "tag", tag, "limit", String.valueOf(limit));
		Collection<DomElement> children = result.getContentElement().getChild("tagmatches").getChildren("tag");
		List<Tag> tags = new ArrayList<Tag>(children.size());
		for (DomElement s : children) {
			tags.add(FACTORY.createItemFromElement(s));
		}
		return tags;
	}

	public static Chart<Artist> getWeeklyArtistChart(String tag, String apiKey) {
		return getWeeklyArtistChart(tag, null, null, -1, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String tag, int limit, String apiKey) {
		return getWeeklyArtistChart(tag, null, null, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String tag, String from, String to, int limit, String apiKey) {
		return Chart.getChart("tag.getWeeklyArtistChart", "tag", tag, "artist", from, to, limit, apiKey);
	}

	public static LinkedHashMap<String, String> getWeeklyChartList(String tag, String apiKey) {
		return Chart.getWeeklyChartList("tag.getWeeklyChartList", "tag", tag, apiKey);
	}

	public static Collection<Chart> getWeeklyChartListAsCharts(String tag, String apiKey) {
		return Chart.getWeeklyChartListAsCharts("tag", tag, apiKey);
	}

	/**
	 * Gets the metadata for a tag.
	 *
	 * @param tag The tag name
	 * @param apiKey A Last.fm API key
	 * @return Tag metdata such as Wiki Text, reach and tag count
	 */
	public static Tag getInfo(String tag, String apiKey) {
		return getInfo(tag, null, apiKey);
	}

	/**
	 * Gets the metadata for a tag.
	 *
	 * @param tag The tag name
	 * @param locale The language to fetch info in, or <code>null</code>
	 * @param apiKey A Last.fm API key
	 * @return Tag metdata such as Wiki Text, reach and tag count
	 */
	public static Tag getInfo(String tag, Locale locale, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("tag", tag);
		if (locale != null && locale.getLanguage().length() != 0) {
			params.put("lang", locale.getLanguage());
		}
		Result result = Caller.getInstance().call("tag.getInfo", apiKey, params);
		return ResponseBuilder.buildItem(result, Tag.class);
	}

	public int compareTo(Tag o) {
		// descending order
		return Double.compare(o.getCount(), this.getCount());
	}

	/**
	 * This implementation of {@link ItemFactory} creates {@link Tag} objects based on the passed xml element.
	 *
	 * @see Tag
	 * @see Tag#FACTORY
	 */
	private static class TagFactory implements ItemFactory<Tag> {
		public Tag createItemFromElement(DomElement element) {
			Tag t = new Tag(element.getChildText("name"));
			t.url = element.getChildText("url");

			if (element.hasChild("count"))
				t.count = Integer.parseInt(element.getChildText("count"));
			else if (element.hasChild("taggings"))
				t.count = Integer.parseInt(element.getChildText("taggings"));

			if (element.hasChild("reach"))
				t.reach = Integer.parseInt(element.getChildText("reach"));
			if (element.hasChild("streamable"))
				t.streamable = StringUtilities.convertToBoolean(element.getChildText("streamable"));

			// wiki
			DomElement wiki = element.getChild("wiki");
			if (wiki != null) {
				String publishedText = wiki.getChildText("published");
				try {
					t.wikiLastChanged = DATE_FORMAT.parse(publishedText);
				} catch (ParseException e) {
					// try parsing it with current locale
					try {
						DateFormat clFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ", Locale.getDefault());
						t.wikiLastChanged = clFormat.parse(publishedText);
					} catch (ParseException e2) {
						// cannot parse date, wrong locale. wait for last.fm to fix.
					}
				}
				t.wikiSummary = wiki.getChildText("summary");
				t.wikiText = wiki.getChildText("content");
			}
			return t;
		}
	}
}
