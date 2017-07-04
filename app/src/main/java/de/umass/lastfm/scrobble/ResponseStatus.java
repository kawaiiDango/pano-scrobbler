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

package de.umass.lastfm.scrobble;

/**
 * Contains information about the result of a scrobbling operation and an optional error message.
 *
 * @author Janni Kovacs
 * @see de.umass.lastfm.scrobble.ScrobbleResult
 * @see de.umass.lastfm.Track#scrobble(ScrobbleData, de.umass.lastfm.Session)
 * @deprecated The 1.2.x scrobble protocol has now been deprecated in favour of the 2.0 protocol which is part of the Last.fm web services
 *             API.
 */
@Deprecated
public class ResponseStatus {

	public static final int OK = 0;
	public static final int BANNED = 1;
	public static final int BADAUTH = 2;
	public static final int BADTIME = 3;
	public static final int BADSESSION = 4;
	public static final int FAILED = 5;

	private int status;
	private String message;

	public ResponseStatus(int status) {
		this(status, null);
	}

	public ResponseStatus(int status, String message) {
		this.status = status;
		this.message = message;
	}

	/**
	 * Returns the optional error message, which is only available if <code>status</code> is <code>FAILED</code>, or
	 * <code>null</code>, if no message is available.
	 *
	 * @return the error message or <code>null</code>
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the result status code of the operation, which is one of the integer constants defined in this class.
	 *
	 * @return the status code
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Returns <code>true</code> if the operation was successful. Same as <code>getStatus() == ResponseStatus.OK</code>.
	 *
	 * @return <code>true</code> if status is OK
	 */
	public boolean ok() {
		return status == OK;
	}

	static int codeForStatus(String status) {
		if ("OK".equals(status))
			return OK;
		if (status.startsWith("FAILED"))
			return FAILED;
		if ("BADAUTH".equals(status))
			return BADAUTH;
		if ("BADSESSION".equals(status))
			return BADSESSION;
		if ("BANNED".equals(status))
			return BANNED;
		if ("BADTIME".equals(status))
			return BADTIME;
		return -1;
	}
}
