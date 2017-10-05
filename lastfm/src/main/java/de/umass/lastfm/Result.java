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

import org.w3c.dom.Document;

import de.umass.xml.DomElement;

/**
 * The <code>Result</code> class contains the response sent by the server, i.e. the status (either ok or failed),
 * an error code and message if failed and the xml response sent by the server.
 *
 * @author Janni Kovacs
 */
public class Result {

	public enum Status {
		OK,
		FAILED
	}

	protected Status status;
	protected String errorMessage = null;
	protected int errorCode = -1;
	protected int httpErrorCode = -1;

	protected Document resultDocument;

	protected Result(Document resultDocument) {
		this.status = Status.OK;
		this.resultDocument = resultDocument;
	}

	protected Result(String errorMessage) {
		this.status = Status.FAILED;
		this.errorMessage = errorMessage;
	}

	static Result createOkResult(Document resultDocument) {
		return new Result(resultDocument);
	}

	static Result createHttpErrorResult(int httpErrorCode, String errorMessage) {
		Result r = new Result(errorMessage);
		r.httpErrorCode = httpErrorCode;
		return r;
	}

	static Result createRestErrorResult(int errorCode, String errorMessage) {
		Result r = new Result(errorMessage);
		r.errorCode = errorCode;
		return r;
	}

	/**
	 * Returns if the operation was successful. Same as <code>getStatus() == Status.OK</code>.
	 *
	 * @return <code>true</code> if the operation was successful
	 */
	public boolean isSuccessful() {
		return status == Status.OK;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public int getHttpErrorCode() {
		return httpErrorCode;
	}

	public Status getStatus() {
		return status;
	}

	public Document getResultDocument() {
		return resultDocument;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public DomElement getContentElement() {
		if (!isSuccessful())
			return null;
		return new DomElement(resultDocument.getDocumentElement()).getChild("*");
	}

	@Override
	public String toString() {
		return "Result[isSuccessful=" + isSuccessful() + ", errorCode=" + errorCode + ", httpErrorCode=" + httpErrorCode + ", errorMessage="
				+ errorMessage + ", status=" + status+"]";
	}
}
