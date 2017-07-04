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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.umass.util.MapUtilities;
import de.umass.xml.DomElement;

/**
 * Venue information bean.
 *
 * @author Janni Kovacs
 */
public class Venue extends ImageHolder {

	static final ItemFactory<Venue> FACTORY = new VenueFactory();
	
	private String name;
	private String url, website;
	private String city, country, street, postal, phonenumber;

	private float latitude, longitude;
	private String timezone;
	private String id;

	private Venue() {
	}

	public String getId() {
		return id;
	}

	/**
	 * Returns a last.fm URL to this venue, e.g.: http://www.last.fm/venue/&lt;id&gt;-&lt;venue name&gt;
	 *
	 * @return last.fm url
	 * @see #getWebsite()
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Returns an URL to the actual venue's website.
	 *
	 * @return website url
	 */
	public String getWebsite() {
		return website;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public float getLatitude() {
		return latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public String getName() {
		return name;
	}

	public String getPostal() {
		return postal;
	}

	public String getStreet() {
		return street;
	}

	public String getTimezone() {
		return timezone;
	}

	public String getPhonenumber() {
		return phonenumber;
	}

	/**
	 * Search for a venue by venue name.
	 *
	 * @param venue The venue name you would like to search for
	 * @param apiKey A Last.fm API key
	 * @return a list of venues
	 */
	public static Collection<Venue> search(String venue, String apiKey) {
		return search(venue, null, apiKey);
	}

	/**
	 * Search for a venue by venue name.
	 *
	 * @param venue The venue name you would like to search for
	 * @param country Filter your results by country. Expressed as an ISO 3166-2 code
	 * @param apiKey A Last.fm API key
	 * @return a list of venues
	 */
	public static Collection<Venue> search(String venue, String country, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("venue", venue);
		MapUtilities.nullSafePut(params, "country", country);
		Result result = Caller.getInstance().call("venue.search", apiKey, params);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement child = result.getContentElement().getChild("venuematches");
		return ResponseBuilder.buildCollection(child, Venue.class);
	}

	/**
	 * Get a list of upcoming events at this venue.
	 *
	 * @param venueId The venue id to fetch the events for
	 * @param apiKey A Last.fm API key
	 * @return a list of events
	 * @see #getPastEvents
	 */
	public static Collection<Event> getEvents(String venueId, String apiKey) {
		return getEvents(venueId, false, apiKey);
	}

	/**
	 * Get a list of upcoming events at this venue.
	 *
	 * @param venueId The venue id to fetch the events for
	 * @param festivalsOnly Whether only festivals should be returned, or all events
	 * @param apiKey A Last.fm API key
	 * @return a list of events
	 * @see #getPastEvents
	 */
	public static Collection<Event> getEvents(String venueId, boolean festivalsOnly, String apiKey) {
		Result result = Caller.getInstance().call("venue.getEvents", apiKey, "venue", venueId, "festivalsonly", festivalsOnly ? "1" : "0" );
		return ResponseBuilder.buildCollection(result, Event.class);
	}
	
	/**
	 * Get a paginated list of all the events held at this venue in the past.
	 *
	 * @param venueId The id for the venue you would like to fetch event listings for
	 * @param apiKey A Last.fm API key
	 * @return a paginated list of events
	 */
	public static PaginatedResult<Event> getPastEvents(String venueId, String apiKey) {
		return getPastEvents(venueId, false, -1, -1, apiKey);
	}

	/**
	 * Get a paginated list of all the events held at this venue in the past.
	 *
	 * @param venueId The id for the venue you would like to fetch event listings for
	 * @param festivalsOnly Whether only festivals should be returned, or all events.
	 * @param page The page of results to return
	 * @param limit The number of results to fetch per page.
	 * @param apiKey A Last.fm API key
	 * @return a paginated list of events
	 */
	public static PaginatedResult<Event> getPastEvents(String venueId, boolean festivalsOnly, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("venue", venueId);
		params.put("festivalsonly", festivalsOnly ? "1" : "0");
		MapUtilities.nullSafePut(params, "page", page);
		MapUtilities.nullSafePut(params, "limit", limit);
		Result result = Caller.getInstance().call("venue.getPastEvents", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Event.class);
	}

	private static class VenueFactory implements ItemFactory<Venue> {
		public Venue createItemFromElement(DomElement element) {
			Venue venue = new Venue();
			venue.id = element.getChildText("id");
			venue.name = element.getChildText("name");
			venue.url = element.getChildText("url");
			venue.phonenumber = element.getChildText("phonenumber");
			venue.website = element.getChildText("website");
			ImageHolder.loadImages(venue, element);
			DomElement l = element.getChild("location");
			venue.city = l.getChildText("city");
			venue.country = l.getChildText("country");
			venue.street = l.getChildText("street");
			venue.postal = l.getChildText("postalcode");
			venue.timezone = l.getChildText("timezone");
			DomElement p = l.getChild("geo:point");
			if (p.getChildText("geo:lat").length() != 0) { // some venues don't have geo information applied
				venue.latitude = Float.parseFloat(p.getChildText("geo:lat"));
				venue.longitude = Float.parseFloat(p.getChildText("geo:long"));
			}
			return venue;
		}
	}
}
