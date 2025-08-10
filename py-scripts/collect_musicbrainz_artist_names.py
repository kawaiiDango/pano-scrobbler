import argparse
import json

def main():
    parser = argparse.ArgumentParser(description="Collect artist names and aliases containing specific substrings.")
    parser.add_argument("input_file", help="Path to the MusicBrainz artist JSONL dump file")
    parser.add_argument("output_file", help="Path to output text file")
    args = parser.parse_args()

    substrings = [", ", "、 ", ";", " & ", " / "]
    names_set = set()

    with open(args.input_file, "r", encoding="utf-8") as infile:
        for line in infile:
            try:
                artist = json.loads(line)
            except Exception:
                continue
            # Check artist name
            name = artist.get("name", "")
            if any(sub in name for sub in substrings):
                names_set.add(name)
            # Check aliases
            for alias in artist.get("aliases", []):
                alias_name = alias.get("name", "")
                if any(sub in alias_name for sub in substrings):
                    names_set.add(alias_name)

    sorted_names = sorted(names_set)
    with open(args.output_file, "w", encoding="utf-8") as outfile:
        for name in sorted_names:
            outfile.write(name + "\n")

    # print the length of the collected names
    print(f"Collected {len(sorted_names)} artist names.")

if __name__ == "__main__":
    main()