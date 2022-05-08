import json
from lxml import etree, objectify
from lxml.etree import Element

m3palette_file = "material3-colors-generated-on-device-12L.json"
xml_file = "../app/src/main/res/values/mdcolors.xml"

colors_json = json.load(open(m3palette_file, 'r'))

xml_root = Element('resources')


def add_color(color_name: str, weight: str, color_hex: str):
    elem = Element("color", {"name": f"mdcolor_{color_name}_{weight}"})
    elem.text = color_hex
    xml_root.append(elem)


def add_color_names():
    elem_group = Element(
        "string-array", {"name": f"mdcolor_names", "translatable": "false"})
    xml_root.append(elem_group)

    for color_name in colors_json:
        elem = Element("item")
        elem.text = color_name
        elem_group.append(elem)


def main():
    add_color_names()

    for color_name in colors_json:
        for weight, color_hex in colors_json[color_name].items():
            add_color(color_name, weight, color_hex)

    objectify.deannotate(xml_root, cleanup_namespaces=True)
    etree.ElementTree(xml_root)\
        .write(xml_file, encoding='utf-8', pretty_print=True, xml_declaration=True)


if __name__ == "__main__":
    main()
