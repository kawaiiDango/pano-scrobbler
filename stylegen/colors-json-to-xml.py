import json
from lxml import etree, objectify
from lxml.etree import Element

m3palette_file = "material3-colors-generated-on-device-12L.json"
xml_file = "../app/src/main/res/values/mdcolors.xml"

colors_json = json.load(open(m3palette_file, 'r'))

xml_root = Element('resources')
xml_array_dict = {}


def add_color(color_name: str, weight: str, color_hex: str):
    array_name = f"mdcolors_{weight}"
    array = xml_array_dict.get(array_name)
    if array is None:
        array = Element("array", {"name": array_name})
        xml_root.append(array)
        xml_array_dict[array_name] = array
    elem = Element("item", {"name": f"{color_name}_{weight}", "type": "color"})
    elem.text = color_hex
    array.append(elem)


for color_name in colors_json:
    for weight, color_hex in colors_json[color_name].items():
        add_color(color_name, weight, color_hex)

objectify.deannotate(xml_root, cleanup_namespaces=True)
etree.ElementTree(xml_root)\
    .write(xml_file, encoding='utf-8', pretty_print=True, xml_declaration=True)
