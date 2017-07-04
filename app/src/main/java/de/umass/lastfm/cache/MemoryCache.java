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

package de.umass.lastfm.cache;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is just for testing. You probably don't want to use it in production.
 *
 * @author Janni Kovacs
 */
public class MemoryCache extends Cache {

	private Map<String, String> data = new HashMap<String, String>();
	private Map<String, Long> expirations = new HashMap<String, Long>();

	public boolean contains(String cacheEntryName) {
		boolean contains = data.containsKey(cacheEntryName);
		System.out.println("MemoryCache.contains: " + cacheEntryName + " ? " + contains);
		return contains;
	}

	public InputStream load(String cacheEntryName) {
		System.out.println("MemoryCache.load: " + cacheEntryName);
		try {
			return new ByteArrayInputStream(data.get(cacheEntryName).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void remove(String cacheEntryName) {
		System.out.println("MemoryCache.remove: " + cacheEntryName);
		data.remove(cacheEntryName);
		expirations.remove(cacheEntryName);
	}

	public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
		System.out.println("MemoryCache.store: " + cacheEntryName + " Expires at: " + new Date(expirationDate));
		StringBuilder b = new StringBuilder();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			String l;
			while ((l = r.readLine()) != null) {
				b.append(l);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		data.put(cacheEntryName, b.toString());
		expirations.put(cacheEntryName, expirationDate);
	}

	public boolean isExpired(String cacheEntryName) {
		boolean exp = expirations.get(cacheEntryName) < System.currentTimeMillis();
		System.out.println("MemoryCache.isExpired: " + cacheEntryName + " ? " + exp);
		return exp;
	}

	public void clear() {
		data.clear();
		expirations.clear();
	}
}
