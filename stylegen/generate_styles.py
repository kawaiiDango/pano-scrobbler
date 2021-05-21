import json
import colorsys
from lxml import etree, objectify
from lxml.etree import Element


# https://gist.github.com/kawanet/a880c83f06d6baf742e45ac9ac52af96
path_src = "material-colors.json"
path_dest_styles_xml = "../app/src/main/res/values/styles_colorpatch.xml"
path_dest_colors_xml = "../app/src/main/res/values/colors_colorpatch.xml"
path_dest_kt = "../app/src/main/java/com/arn/scrobble/themes/ColorPatchMap.kt"
primary_shade = "300"
noti_shade = "400"
secondary_shade = "100"
background_shade = "900"

jsondoc = json.load(open(path_src, 'r'))


def darken_color(color_hex: str, l: int) -> str:
    color_hex = color_hex.lstrip('#')
    rgb = tuple(int(color_hex[i:i+2], 16) for i in (0, 2, 4))
    rgb = tuple(x/255 for x in rgb)
    hls = colorsys.rgb_to_hls(*rgb)
    new_hls = (hls[0], l, hls[2])
    new_rgb = colorsys.hls_to_rgb(*new_hls)
    new_rgb = tuple(int(255*x) for x in new_rgb)
    return '#%02x%02x%02x' % new_rgb


styles_root = Element('resources')
style = Element("style", {"name": "ColorPatch"})
styles_root.append(style)

colors_root = Element('resources')

for color, shades in jsondoc.items():

    if primary_shade.lower() in shades:
        if color == 'pink':
            colorPrimary = "@color/pinkPrimary"
            colorNoti = "@color/pinkNoti"
        else:
            colorPrimary = shades[primary_shade.lower()]
            colorNoti = shades[noti_shade.lower()]
        prefix = f"ColorPatch.{color.capitalize()}"
        style = Element("style", {"name": prefix+"_Main"})
        styles_root.append(style)
        elem = Element("item", {"name": "colorPrimary"})
        elem.text = colorPrimary
        style.append(elem)
        elem = Element("item", {"name": "colorNoti"})
        elem.text = colorNoti
        style.append(elem)

    if secondary_shade.lower() in shades:
        if color == 'purple':
            colorSecondary = "@color/purpleSecondary"
        else:
            colorSecondary = shades[secondary_shade.lower()]
        prefix = f"ColorPatch.{color.capitalize()}"
        style = Element("style", {"name": prefix+"_Secondary"})
        styles_root.append(style)
        elem = Element("item", {"name": "colorSecondary"})
        elem.text = colorSecondary
        style.append(elem)

    if background_shade.lower() in shades:
        colorBackground = shades[background_shade.lower()]
        colorBackground = darken_color(colorBackground, 0.12)

        # android bug: colorBackground has to be a reference on some devices
        color_element_name = color.capitalize() + "_Background"
        elem = Element("color", {"name": color_element_name})
        elem.text = colorBackground
        colors_root.append(elem)

        style = Element("style", {"name": "ColorPatch." + color_element_name})
        styles_root.append(style)
        elem = Element("item", {"name": "android:colorBackground"})
        elem.text = f"@color/{color_element_name}"
        style.append(elem)
        elem = Element("item", {"name": "colorSurface"})
        elem.text = f"@color/{color_element_name}"
        style.append(elem)

color = "black"
prefix = f"ColorPatch.{color.capitalize()}"
style = Element("style", {"name": prefix+"_Background"})
styles_root.append(style)
elem = Element("item", {"name": "android:colorBackground"})
elem.text = "@android:color/black"
style.append(elem)
elem = Element("item", {"name": "colorSurface"})
elem.text = "#010101"
style.append(elem)

objectify.deannotate(styles_root, cleanup_namespaces=True)
etree.ElementTree(styles_root)\
    .write(path_dest_styles_xml, encoding='utf-8', pretty_print=True, xml_declaration=True)


objectify.deannotate(colors_root, cleanup_namespaces=True)
etree.ElementTree(colors_root)\
    .write(path_dest_colors_xml, encoding='utf-8', pretty_print=True, xml_declaration=True)


# kt

primaryStyles = []
secondaryStyles = []
backgroundStyles = []

for elem in styles_root.getchildren():
    name = elem.get("name")
    id = "R.style." + name.replace(".", "_")
    line = f"        \"{name.replace('ColorPatch.', '').split('_')[0]}\" to {id},\n"
    if name.endswith("_Main"):
        primaryStyles.append(line)
    if name.endswith("_Secondary"):
        secondaryStyles.append(line)
    if name.endswith("_Background"):
        backgroundStyles.append(line)

kt_template = f"""
package com.arn.scrobble.themes

import com.arn.scrobble.R

object ColorPatchMap {{
    val primaryStyles = mapOf(
{"".join(primaryStyles)}
    )
    val secondaryStyles = mapOf(
{"".join(secondaryStyles)}
    )
    val backgroundStyles = mapOf(
{"".join(backgroundStyles)}
    )
}}
"""

with open(path_dest_kt, "w+") as f:
    f.write(kt_template)
