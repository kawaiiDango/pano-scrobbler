from lxml import etree
import json
import colorsys
import sys

file_name = "Tonal Palettes.svg"
color_name = sys.argv[1]

xml_root = etree.parse(file_name).getroot()


def get_color(element) -> str:
    color: str = element.get("fill")
    if color == "white":
        color = "#FFFFFF"
    elif color == "black":
        color = "#000000"
    return color


def get_hls(color_hex: str) -> tuple:
    color_hex = color_hex.lstrip("#")
    rgb = tuple(int(color_hex[i:i+2], 16) for i in (0, 2, 4))
    rgb = tuple(x/255 for x in rgb)
    return colorsys.rgb_to_hls(*rgb)


def parse_palette() -> dict:
    shades = [0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]
    i = 0
    palette_dict = {color_name: {}}

    for element in xml_root.iter():
        if element.tag.endswith("rect"):
            color = get_color(element)
            palette_dict[color_name][str(shades[i])] = color
            i += 1
            if i == len(shades):
                break

    return palette_dict


def add_to_json_file(palette_dict: dict, path_src: str):
    jsondoc = json.load(open(path_src, 'r'))
    jsondoc[color_name] = palette_dict[color_name]
    with open(path_src, 'w') as f:
        json.dump(jsondoc, f, indent=2)


if __name__ == "__main__":
    palette_dict = parse_palette()
    add_to_json_file(palette_dict, "material3-colors-from-wallpaper-app.json")
