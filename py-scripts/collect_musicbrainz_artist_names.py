import argparse
import csv
import json
import xxhash

x = xxhash.xxh3_64()

def lookup_name(name: str, hashed_names_set: set) -> bytes | None:
    x.reset()
    x.update(name.lower().encode())
    h = x.digest()
    found = h in hashed_names_set
    return h if found else None

def collect_artist_names(
        artists_file: str,
        canonical_data_file: str,
        hashes_file: str,
        debug_file: str | None,
) -> None:

    substrings = [
        ", ",
        "、",  # Chinese, Japanese comma
        "، ",  # Urdu, Persian comma
        " و ",  # Arabic comma
        "፣ ",  # Amharic comma
        ";",
        " & ",
        "＆",  # Fullwidth ampersand used in Japanese
        " / ",
        # the rest is for youtube music
        " and ",
        " en ",  # Afrikaans
        " və ",  # Azerbaijani
        " dan ",  # Bahasa Indonesia, Bahasa Malaysia
        " i ",  # Bosnian, Catalan, Croatian, Polish, Belarusian, Ukrainian
        " a ",  # Czech, Slovenčina
        " og ",  # Danish, Icelandic, Norwegian
        " und ",  # German
        " ja ",  # Estonian, Finnish
        " y ",  # Spanish
        " eta ",  # Basque
        ", at ",  # Filipino
        " et ",  # French
        " e ",  # Galician, Portuguese
        ", ne-",  # Zulu
        " na ",  # Swahili
        " un ",  # Latvian
        " ir ",  # Lithuanian
        " és ",  # Hungarian
        " va ",  # Uzbek
        " dhe ",  # Albanian
        " și ",  # Romanian
        " in ",  # Slovenščina
        " och ",  # Swedish
        " và ",  # Vietnamese
        " ve ",  # Turkish
        " и ",  # Bulgarian, Macedonian, Russian, Serbian
        " жана ",  # Kyrgyz
        " και ",  # Greek
        " և ",  # Armenian
        " ו-",  # Hebrew
        " اور ",  # Urdu
        "، و ",  # Persian
        " र ",  # Nepali
        " आणि ",  # Marathi
        " और ",  # Hindi
        " আৰু ",  # Assamese
        " এবং ",  # Bengali
        " ਅਤੇ ",  # Punjabi
        " અને ",  # Gujarati
        ", ଓ ",  # Odia
        " மற்றும் ",  # Tamil
        " మరియు ",  # Telugu
        ", ಮತ್ತು ",  # Kannada
        " എന്നിവ",  # Malayalam (This comes at the end of the phrase)
        ", සහ ",  # Sinhala
        " และ",  # Thai
        " ແລະ ",  # Lao
        "နှင့် ",  # Burmese
        " და ",  # Georgian
        " እና ",  # Amharic
        " និង ",  # Khmer
        "和",  # Chinese (Simplified)
        "及",  # Chinese (Hong Kong)
        " 및 ",  # Korean
    ]

    names_set = set()
    credit_names_set = set()
    filtered_credit_names_set = set()
    hashed_names_set = set()

    with open(artists_file, encoding="utf-8") as jsonl_file:
        for line in jsonl_file:
            try:
                artist = json.loads(line)
            except Exception:
                continue
            # Check artist name
            name = artist.get("name", "").lower()
            if any(sub in name for sub in substrings):
                names_set.add(name)
            # Check aliases
            for alias in artist.get("aliases", []):
                alias_name = alias.get("name", "").lower()
                if any(sub in alias_name for sub in substrings):
                    names_set.add(alias_name)

    print(f"{len(names_set)} artist names and aliases")

    with open(canonical_data_file, newline="", encoding="utf-8") as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            artist_mbids = row.get("artist_mbids")
            # pick only single artists
            if not artist_mbids or "," in artist_mbids:
                continue
            artist_name = row.get("artist_credit_name")
            if artist_name and any(sub in artist_name for sub in substrings):
                credit_names_set.add(artist_name.lower())

    print(f"{len(credit_names_set)} artist credit names.")

    for name in credit_names_set:
        if (
            name not in names_set
        ):
            filtered_credit_names_set.add(name)

    print(f"{len(filtered_credit_names_set)} filtered artist credit names.")

    names_set.update(filtered_credit_names_set)

    print(f"{len(names_set)} artist names and aliases with equivalent names")

    if debug_file:
        with open(debug_file, "w", encoding="utf-8") as dbgfile:
            for name in sorted(filtered_credit_names_set):
                dbgfile.write(f"{name}\n")

    for name in names_set:
        x.reset()
        x.update(name.encode())
        h = x.digest()
        hashed_names_set.add(h)

    # sort the hashes to make output deterministic
    hashed_names_sorted = sorted(hashed_names_set)

    with open(hashes_file, "wb") as outfile:
        for h in hashed_names_sorted:
            outfile.write(h)

    # print the length of the collected names
    print(f"{len(hashed_names_sorted)} hashed names")

    # test
    test_strings = [
        "The Beatles",
        "Simon & Garfunkel",
        "Siouxsie and the Banshees",
        "Siouxsie & the Banshees",
        "AC/DC",
        "Guns N' Roses",
        "Earth, Wind & Fire",
        "Florence and the Machine",
        "Salt-N-Pepa",
        "Bell Biv DeVoe",
        "Crosby, Stills, Nash & Young",
        "Alice Schach and the Magic Orchestra",
        "Invent, Animate",
    ]
    
    for test_name in test_strings:
        test_hash = lookup_name(test_name, hashed_names_set)
        # print the hash as hex
        print(f"Hash for '{test_name}': {test_hash.hex() if test_hash else None}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Collect artist names and aliases containing specific substrings."
    )
    parser.add_argument(
        "--artists_file", help="Path to the MusicBrainz artist JSONL dump file", required=False
    )
    parser.add_argument(
        "--canonical_data_file",
        help="Path to the MusicBrainz canonical_musicbrainz_data csv file",
        required=False,
    )
    parser.add_argument("--hashes_file", help="Path to output hashes file", required=True)
    parser.add_argument("--debug_file", help="Path to output debug file", required=False)
    parser.add_argument("--test_string", help="Test string to look up", required=False)
    args = parser.parse_args()

    if args.test_string and args.hashes_file:
        # Load hashed names from output file
        hashed_names_set = set()
        with open(args.hashes_file, "rb") as infile:
            while True:
                h = infile.read(8)  # xxh3_64 produces 8-byte hashes
                if not h:
                    break
                hashed_names_set.add(h)
        test_hash = lookup_name(args.test_string, hashed_names_set)
        print(f"Hash for '{args.test_string}': {test_hash.hex() if test_hash else None}")
    elif args.artists_file and args.canonical_data_file and args.hashes_file:
        collect_artist_names(
            args.artists_file,
            args.canonical_data_file,
            args.hashes_file,
            args.debug_file
         )
    else:
        parser.print_help()
