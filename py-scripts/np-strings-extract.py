#!/usr/bin/env python3

# adapted from https://github.com/gitanuj/nowplayinghistory/blob/master/script.py

import os
import sys

def dump_strings_file(filepath, line):
    file = open(filepath, "w")
    file.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    file.write("<resources>\n")
    file.write(line)
    file.write("</resources>\n")

def main(decompiledDir, stringNameToExtract, outputFileSuffix):
    # stringNameToExtract = "song_format_string" for pixel now playing
    # or "auto_shazam_now_playing" for Shazam

    fsSet = set()
    resDir = os.path.join(decompiledDir, "res")


    prevLang = "00"
    prevFs = ""

    for folder in os.listdir(resDir):
        path = os.path.join(resDir, folder)
        if not os.path.isdir(path):
            continue

        for file in os.listdir(path):
            if file != "strings.xml":
                continue

            filepath = os.path.join(path, file)
            for line in open(filepath):
                if stringNameToExtract in line:
                    i1 = line.find(">")
                    i2 = line.find("</")
                    fs = line[i1+1:i2]
                    fsSet.add(fs)

                    lang = folder.split('-')[0]
                    shouldBreak = False
                    if prevLang == lang and prevFs == fs:
                        shouldBreak = True

                    prevFs = fs
                    prevLang = lang

                    if shouldBreak:
                        break

                    outdir = os.path.join("../app/src/main/res", folder)
                    os.makedirs(outdir, exist_ok=True)
                    dump_strings_file(os.path.join(outdir, f"strings-{outputFileSuffix}.xml"), line)

                    break
    print("val fs = arrayOf(")
    for item in fsSet:
        print('"'+item+'",')
    print(")")

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python np-strings-extract.py <decompiled-dir> <string-name-to-extract> <output-file-suffix>")
        sys.exit(1)

    decompiledDir = sys.argv[1]
    stringNameToExtract = sys.argv[2]
    outputFileSuffix = sys.argv[3]
    main(decompiledDir, stringNameToExtract, outputFileSuffix)
