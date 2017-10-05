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
package de.umass.util;

import java.util.Map;

/**
 * Utility class to perform various operations on Maps.
 *
 * @author Adrian Woodhead
 */
public final class MapUtilities {

	private MapUtilities() {
	}

	/**
	 * Puts the passed key and value into the map only if the value is not null.
	 *
	 * @param map Map to add key and value to.
	 * @param key Map key.
	 * @param value Map value, if null will not be added to map.
	 */
	public static void nullSafePut(Map<String, String> map, String key, String value) {
		if (value != null) {
			map.put(key, value);
		}
	}

	/**
	 * Puts the passed key and value into the map only if the value is not null.
	 *
	 * @param map Map to add key and value to.
	 * @param key Map key.
	 * @param value Map value, if null will not be added to map.
	 */
	public static void nullSafePut(Map<String, String> map, String key, Integer value) {
		if (value != null) {
			map.put(key, value.toString());
		}
	}

	/**
	 * Puts the passed key and value into the map only if the value is not -1.
	 *
	 * @param map Map to add key and value to.
	 * @param key Map key.
	 * @param value Map value, if -1 will not be added to map.
	 */
	public static void nullSafePut(Map<String, String> map, String key, int value) {
		if (value != -1) {
			map.put(key, Integer.toString(value));
		}
	}

	/**
	 * Puts the passed key and value into the map only if the value is not -1.
	 *
	 * @param map Map to add key and value to.
	 * @param key Map key.
	 * @param value Map value, if -1 will not be added to map.
	 */
	public static void nullSafePut(Map<String, String> map, String key, double value) {
		if (value != -1) {
			map.put(key, Double.toString(value));
		}
	}
}
