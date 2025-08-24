import argparse
import csv
import json
import xxhash


def main():
    x = xxhash.xxh3_64()
    parser = argparse.ArgumentParser(
        description="Collect artist names and aliases containing specific substrings."
    )
    parser.add_argument(
        "artists_file", help="Path to the MusicBrainz artist JSONL dump file"
    )
    parser.add_argument(
        "canonical_data_file",
        help="Path to the MusicBrainz canonical_musicbrainz_data csv file",
    )
    parser.add_argument("output_file", help="Path to output text file")
    args = parser.parse_args()

    substrings = [
        ", ",
        "、",  # Chinese, Japanese comma
        "، ",  # Urdu, Persian comma
        " و ",  # Arabic comma
        "፣ ",  # Amharic comma
        ";",
        " & ",
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

    equivalent_separators = [
        (" & ", " and "),  # fixes "Siouxsie & the Banshees"
        # Add more pairs as needed
    ]

    names_set = set()
    credit_names_set = set()
    filtered_credit_names_set = set()
    hashed_names_set = set()

    with open(args.artists_file, encoding="utf-8") as jsonl_file:
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

    with open(args.canonical_data_file, newline="", encoding="utf-8") as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            artist_name = row.get("artist_credit_name")
            if artist_name:
                credit_names_set.add(artist_name.lower())

    print(f"{len(credit_names_set)} artist credit names.")

    for name in names_set:
        for sep1, sep2 in equivalent_separators:
            if sep1 in name:
                equivalent_name = name.replace(sep1, sep2)
                if (
                    equivalent_name not in names_set
                    and equivalent_name in credit_names_set
                ):
                    filtered_credit_names_set.add(equivalent_name)
            elif sep2 in name:
                equivalent_name = name.replace(sep2, sep1)
                if (
                    equivalent_name not in names_set
                    and equivalent_name in credit_names_set
                ):
                    filtered_credit_names_set.add(equivalent_name)

    print(f"{len(filtered_credit_names_set)} filtered artist credit names.")

    names_set.update(filtered_credit_names_set)

    print(f"{len(names_set)} artist names and aliases with equivalent names")

    with open(args.output_file, "wb") as outfile:
        for name in names_set:
            x.reset()
            x.update(name.encode())
            h = x.digest()
            hashed_names_set.add(h)
            outfile.write(h)

    # print the length of the collected names
    print(f"{len(hashed_names_set)} hashed names")

    # test
    test_name = "Alice Schach and the Magic Orchestra".lower()
    x.reset()
    x.update(test_name.encode())
    test_hash = x.digest()
    test_hash_found = test_hash in hashed_names_set
    # print the hash as hex
    print(f"Hash for '{test_name}': {test_hash.hex()}")
    print(f"Hash found in set: {test_hash_found}")


if __name__ == "__main__":
    main()
