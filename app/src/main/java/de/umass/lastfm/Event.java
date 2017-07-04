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

import de.umass.util.MapUtilities;
import de.umass.xml.DomElement;

/**
 * Bean for Events.
 *
 * @author Janni Kovacs
 */
public class Event extends ImageHolder {

	static final ItemFactory<Event> FACTORY = new EventFactory();

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);

	private int id;
	private String title;
	private Collection<String> artists;
	private String headliner;
	private Collection<TicketSupplier> tickets;

	private Date startDate;
	private Date endDate;

	private String description;
	private String url;
	private String website;
	private int attendance;
	private int reviews;

	private Venue venue;
	private AttendanceStatus userAttendanceStatus;

	private Event() {
	}

	public Collection<String> getArtists() {
		return artists;
	}

	public int getAttendance() {
		return attendance;
	}

	public String getDescription() {
		return description;
	}

	public String getHeadliner() {
		return headliner;
	}

	public int getId() {
		return id;
	}

	public int getReviews() {
		return reviews;
	}

	/**
	 * Returns the start date and time of this event. Note that the time might not be correct, but instead a random time, if not set to a
	 * proper value on last.fm (happens often).
	 *
	 * @return start date
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * Returns the event's end date, or <code>null</code> if not available. End dates are only supplied for events such as festivals, which
	 * last longer than one day.
	 *
	 * @return end date
	 */
	public Date getEndDate() {
		return endDate;
	}

	public String getTitle() {
		return title;
	}

	/**
	 * Returns the last.fm event url, i.e. http://www.last.fm/event/event-id
	 *
	 * @return last.fm url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Returns the event website url, if available.
	 *
	 * @return event website url
	 */
	public String getWebsite() {
		return website;
	}

	public Collection<TicketSupplier> getTicketSuppliers() {
		return tickets;
	}

	public Venue getVenue() {
		return venue;
	}

	public AttendanceStatus getAttendanceStatus() {
		return this.userAttendanceStatus;
	}

	/**
	 * Get the metadata for an event on Last.fm. Includes attendance and lineup information.
	 *
	 * @param eventId The numeric last.fm event id
	 * @param apiKey A Last.fm API key.
	 * @return Event metadata
	 */
	public static Event getInfo(String eventId, String apiKey) {
		Result result = Caller.getInstance().call("event.getInfo", apiKey, "event", eventId);
		return ResponseBuilder.buildItem(result, Event.class);
	}

	/**
	 * Set a user's attendance status for an event.
	 *
	 * @param eventId The numeric last.fm event id
	 * @param status The attendance status
	 * @param session A Session instance
	 * @return the Result of the operation.
	 * @see de.umass.lastfm.Event.AttendanceStatus
	 * @see de.umass.lastfm.Authenticator
	 */
	public static Result attend(String eventId, AttendanceStatus status, Session session) {
		return Caller.getInstance().call("event.attend", session, "event", eventId, "status", String.valueOf(status.getId()));
	}

	/**
	 * Share an event with one or more Last.fm users or other friends.
	 *
	 * @param eventId An event ID
	 * @param recipients A comma delimited list of email addresses or Last.fm usernames. Maximum is 10.
	 * @param message An optional message to send with the recommendation.
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result share(String eventId, String recipients, String message, Session session) {
		return Caller.getInstance().call("event.share", session, "event", eventId, "recipient", recipients, "message", message);
	}

	/**
	 * Get a list of attendees for an event.
	 *
	 * @param eventId The numeric last.fm event id
	 * @param apiKey A Last.fm API key
	 * @return a list of users who attended the given event
	 */
	public static Collection<User> getAttendees(String eventId, String apiKey) {
		Result result = Caller.getInstance().call("event.getAttendees", apiKey, "event", eventId);
		return ResponseBuilder.buildCollection(result, User.class);
	}

	/**
	 * Get shouts for an event.
	 *
	 * @param eventId The numeric last.fm event id
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String eventId, String apiKey) {
		return getShouts(eventId, -1, -1, apiKey);
	}

	/**
	 * Get shouts for an event.
	 *
	 * @param eventId The numeric last.fm event id
	 * @param page The page number to fetch
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String eventId, int page, String apiKey) {
		return getShouts(eventId, page, -1, apiKey);
	}

	/**
	 * Get shouts for an event.
	 *
	 * @param eventId The numeric last.fm event id
	 * @param page The page number to fetch
	 * @param limit An integer used to limit the number of shouts returned per page or -1 for default
	 * @param apiKey A Last.fm API key.
	 * @return a page of <code>Shout</code>s
	 */
	public static PaginatedResult<Shout> getShouts(String eventId, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("event", eventId);
		MapUtilities.nullSafePut(params, "limit", limit);
		MapUtilities.nullSafePut(params, "page", page);
		Result result = Caller.getInstance().call("event.getShouts", apiKey, params);
		return ResponseBuilder.buildPaginatedResult(result, Shout.class);
	}

	/**
	 * Enumeration for the attendance status parameter of the <code>attend</code> operation.
	 */
	public static enum AttendanceStatus {

		ATTENDING(0),
		MAYBE_ATTENDING(1),
		NOT_ATTENDING(2);

		private int id;

		private AttendanceStatus(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public static AttendanceStatus getByID(int statusId) {
			for (AttendanceStatus status : AttendanceStatus.values()) {
				if(status.id == statusId)
					return status;
			}
			return null;
		}
	}

	public static class TicketSupplier {
		private String name;
		private String website;

		public TicketSupplier(String name, String website) {
			this.name = name;
			this.website = website;
		}

		public String getName() {
			return name;
		}

		public String getWebsite() {
			return website;
		}
	}

	private static class EventFactory implements ItemFactory<Event> {
		public Event createItemFromElement(DomElement element) {
//			if (element == null)
//				return null;
			Event event = new Event();
			ImageHolder.loadImages(event, element);
			event.id = Integer.parseInt(element.getChildText("id"));
			event.title = element.getChildText("title");
			event.description = element.getChildText("description");
			event.url = element.getChildText("url");
			if (element.hasChild("attendance"))
				event.attendance = Integer.parseInt(element.getChildText("attendance"));
			if (element.hasChild("reviews"))
				event.reviews = Integer.parseInt(element.getChildText("reviews"));
			try {
				event.startDate = DATE_FORMAT.parse(element.getChildText("startDate"));
				if (element.hasChild("endDate")) {
					event.endDate = DATE_FORMAT.parse(element.getChildText("endDate"));
				}
			} catch (ParseException e1) {
				// Date format not valid !?, should definitely not happen.
			}
			event.headliner = element.getChild("artists").getChildText("headliner");
			event.artists = new ArrayList<String>();
			for (DomElement artist : element.getChild("artists").getChildren("artist")) {
				event.artists.add(artist.getText());
			}
			event.website = element.getChildText("website");
			event.tickets = new ArrayList<TicketSupplier>();
			if (element.hasChild("tickets")) {
				for (DomElement ticket : element.getChild("tickets").getChildren("ticket")) {
					event.tickets.add(new TicketSupplier(ticket.getAttribute("supplier"), ticket.getText()));
				}
			}
			if(element.hasAttribute("status"))
				event.userAttendanceStatus = AttendanceStatus.getByID(Integer.parseInt(element.getAttribute("status")));
			if(element.hasChild("venue"))
				event.venue = ResponseBuilder.buildItem(element.getChild("venue"), Venue.class);
			return event;
		}
	}
}
