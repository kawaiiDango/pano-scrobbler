import argparse
import json
import xxhash

def main():
    x = xxhash.xxh3_64()
    parser = argparse.ArgumentParser(
        description="Collect artist names and aliases containing specific substrings."
    )
    parser.add_argument(
        "input_file", help="Path to the MusicBrainz artist JSONL dump file"
    )
    parser.add_argument("output_file", help="Path to output text file")
    args = parser.parse_args()

    substrings = [
        ", ",
        "、", # Chinese, Japanese comma
        "، ", # Urdu, Persian comma
        " و ", # Arabic comma
        "፣ ", # Amharic comma
        ";",
        " & ",
        " / ",
        # the rest is for youtube music
        " and ",
        " en ", # Afrikaans
        " və ", # Azerbaijani
        " dan ", # Bahasa Indonesia, Bahasa Malaysia
        " i ", # Bosnian, Catalan, Croatian, Polish, Belarusian, Ukrainian
        " a ", # Czech, Slovenčina
        " og ", # Danish, Icelandic, Norwegian
        " und ", # German
        " ja ", # Estonian, Finnish
        " y ", # Spanish
        " eta ", # Basque
        ", at ", # Filipino
        " et ", # French
        " e ", # Galician, Portuguese
        ", ne-", # Zulu
        " na ", # Swahili
        " un ", # Latvian
        " ir ", # Lithuanian
        " és ", # Hungarian
        " va ", # Uzbek
        " dhe ", # Albanian
        " și ", # Romanian
        " in ", # Slovenščina
        " och ", # Swedish
        " và ", # Vietnamese
        " ve ", # Turkish
        " и ", # Bulgarian, Macedonian, Russian, Serbian
        " жана ", # Kyrgyz
        " και ", # Greek
        " և ", # Armenian
        " ו-", # Hebrew
        " اور ", # Urdu
        "، و ", # Persian
        " र ", # Nepali
        " आणि ", # Marathi
        " और ", # Hindi
        " আৰু ", # Assamese
        " এবং ", # Bengali
        " ਅਤੇ ", # Punjabi
        " અને ", # Gujarati
        ", ଓ ", # Odia
        " மற்றும் ", # Tamil
        " మరియు ", # Telugu
        ", ಮತ್ತು ", # Kannada
        " എന്നിവ", # Malayalam (This comes at the end of the phrase)
        ", සහ ", # Sinhala
        " และ", # Thai
        " ແລະ ", # Lao
        "နှင့် ", # Burmese
        " და ", # Georgian
        " እና ", # Amharic
        " និង ", # Khmer
        "和", # Chinese (Simplified)
        "及", # Chinese (Hong Kong)
        " 및 ", # Korean
    ]

    names_set = set()
    hashed_names_set = set()

    with open(args.input_file, "r", encoding="utf-8") as infile:
        for line in infile:
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

    print(f"Collected {len(names_set)} artist names.")

    with open(args.output_file, "wb") as outfile:
        for name in names_set:
            x.reset()
            x.update(name.encode())
            h = x.digest()
            hashed_names_set.add(h)
            outfile.write(h)

    # print the length of the collected names
    print(f"Length of hashed names: {len(hashed_names_set)}")

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
