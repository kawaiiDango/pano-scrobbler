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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import de.umass.lastfm.scrobble.Scrobbler;
import de.umass.lastfm.scrobble.SubmissionData;
import de.umass.util.StringUtilities;

/**
 * Standard {@link Cache} implementation which is used by default by the {@link de.umass.lastfm.Caller} class.
 * This implementation caches all responses in the file system. In addition to the raw responses it stores a
 * .meta file which contains the expiration date for the specified request.
 *
 * @author Janni Kovacs
 */
@SuppressWarnings({"ALL"})
public class FileSystemCache extends Cache implements ScrobbleCache {

	private static final String SUBMISSIONS_FILE = "submissions.txt";

	private File cacheDir;

	public FileSystemCache() {
		this(new File(System.getProperty("user.home") + "/.last.fm-cache"));
	}

	public FileSystemCache(File cacheDir) {
		this.cacheDir = cacheDir;
	}

	public boolean contains(String cacheEntryName) {
		return new File(cacheDir, cacheEntryName + ".xml").exists();
	}

	public void remove(String cacheEntryName) {
		new File(cacheDir, cacheEntryName + ".xml").delete();
		new File(cacheDir, cacheEntryName + ".meta").delete();
	}

	public boolean isExpired(String cacheEntryName) {
		File f = new File(cacheDir, cacheEntryName + ".meta");
		if (!f.exists())
			return false;
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(f));
			long expirationDate = Long.valueOf(p.getProperty("expiration-date"));
			return expirationDate < System.currentTimeMillis();
		} catch (IOException e) {
			return false;
		}
	}

	public void clear() {
		for (File file : cacheDir.listFiles()) {
			if (file.isFile()) {
				file.delete();
			}
		}
	}

	public InputStream load(String cacheEntryName) {
		try {
			return new FileInputStream(new File(cacheDir, cacheEntryName + ".xml"));
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
		createCache();
		File f = new File(cacheDir, cacheEntryName + ".xml");
		try {
			BufferedInputStream is = new BufferedInputStream(inputStream);
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f));
			int read;
			byte[] buffer = new byte[4096];
			while ((read = is.read(buffer)) != -1) {
				os.write(buffer, 0, read);
			}
			os.close();
			is.close();
			File fm = new File(cacheDir, cacheEntryName + ".meta");
			Properties p = new Properties();
			p.setProperty("expiration-date", Long.toString(expirationDate));
			p.store(new FileOutputStream(fm), null);
		} catch (IOException e) {
			// we ignore the exception. if something went wrong we just don't cache it.
		}
	}

	private void createCache() {
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
			if (!cacheDir.isDirectory()) {
				this.cacheDir = cacheDir.getParentFile();
			}
		}
	}

	public void cacheScrobble(Collection<SubmissionData> submissions) {
		cacheScrobble(submissions.toArray(new SubmissionData[submissions.size()]));
	}

	public void cacheScrobble(SubmissionData... submissions) {
		createCache();
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(new File(cacheDir, SUBMISSIONS_FILE), true));
			for (SubmissionData submission : submissions) {
				w.append(submission.toString());
				w.newLine();
			}
			w.close();
		} catch (IOException e) {
			// huh ?
			//	e.printStackTrace();
		}
	}

	public boolean isEmpty() {
		File file = new File(cacheDir, SUBMISSIONS_FILE);
		if (!file.exists())
			return true;
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line = r.readLine();
			r.close();
			return line == null || "".equals(line);
		} catch (IOException e) {
			// huh
			//	e.printStackTrace();
		}
		return true;
	}

	public void scrobble(Scrobbler scrobbler) throws IOException {
		File file = new File(cacheDir, SUBMISSIONS_FILE);
		if (file.exists()) {
			BufferedReader r = new BufferedReader(new FileReader(file));
			List<SubmissionData> list = new ArrayList<SubmissionData>(50);
			String line;
			while ((line = r.readLine()) != null) {
				SubmissionData d = new SubmissionData(line);
				list.add(d);
				if (list.size() == 50) {
					scrobbler.submit(list);
					list.clear();
				}
			}
			if (list.size() > 0)
				scrobbler.submit(list);
			r.close();
			FileWriter w = new FileWriter(file);
			w.close();
		}
	}

	public void clearScrobbleCache() {
		File file = new File(cacheDir, SUBMISSIONS_FILE);
		file.delete();
	}

	public void cacheScrobbles(Collection<ScrobbleData> scrobbles) {
		cacheScrobbles(scrobbles.toArray(new ScrobbleData[scrobbles.size()]));
	}

	public void cacheScrobbles(ScrobbleData... scrobbles) {
		createCache();
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(new File(cacheDir, SUBMISSIONS_FILE), true));
			for (ScrobbleData scrobble : scrobbles) {
				w.append(encodeScrobbleData(scrobble));
				w.newLine();
			}
			w.close();
		} catch (IOException e) {
			// huh ?
			//	e.printStackTrace();
		}
	}

	public List<ScrobbleResult> scrobble(Session session) throws IOException {
		File file = new File(cacheDir, SUBMISSIONS_FILE);
		List<ScrobbleResult> result = new ArrayList<ScrobbleResult>();
		if (file.exists()) {
			BufferedReader r = new BufferedReader(new FileReader(file));
			List<ScrobbleData> list = new ArrayList<ScrobbleData>(50);
			String line;
			while ((line = r.readLine()) != null) {
				ScrobbleData d = decodeScrobbleData(line);
				list.add(d);
				if (list.size() == 50) {
					result.addAll(Track.scrobble(list, session));
					list.clear();
				}
			}
			if (list.size() > 0)
				result.addAll(Track.scrobble(list, session));
			r.close();
			FileWriter w = new FileWriter(file);
			w.close();
		}
		return result;
	}

	private static String encodeScrobbleData(ScrobbleData d) {
		String artist = StringUtilities.encode(d.getArtist());
		String track = StringUtilities.encode(d.getTrack());
		String album = StringUtilities.encode(d.getAlbum());
		String albumArtist = StringUtilities.encode(d.getAlbumArtist());
		return String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s;%b", artist, track, d.getTimestamp(), d.getDuration(), album, albumArtist, d.getMusicBrainzId(),
				d.getTrackNumber(), d.getStreamId(), d.isChosenByUser());
	}

	private static ScrobbleData decodeScrobbleData(String s) {
		String[] parts = s.split(";", 10);
		return new ScrobbleData(StringUtilities.decode(parts[0]), StringUtilities.decode(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
				StringUtilities.decode(parts[4]), StringUtilities.decode(parts[5]), parts[6], Integer.parseInt(parts[7]), parts[8], Boolean.parseBoolean(parts[9]));
	}
}
