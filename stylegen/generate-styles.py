import json
import colorsys
from lxml import etree, objectify
from lxml.etree import Element
import sys

path_dest_kt = "../app/src/main/java/com/arn/scrobble/themes/ColorPatchMap.kt"
path_src = "material3-colors-generated-on-device-12L.json"


def gen_styles(is_dark: bool):

    if is_dark:
        path_dest_styles_xml = "../app/src/main/res/values-night/styles_colorpatch.xml"
        path_dest_colors_xml = "../app/src/main/res/values-night/colors_colorpatch.xml"

        primary_shade = "200"
        secondary_shade = "200"
        tertiary_shade = "200"
        background_shade = "900"
        on_shade = "800"
        outline_shade = "100"
        container_shade = "700"
        on_background_shade = "50"
        primary_inverse_shade = "600"
    else:
        path_dest_styles_xml = "../app/src/main/res/values/styles_colorpatch.xml"
        path_dest_colors_xml = "../app/src/main/res/values/colors_colorpatch.xml"

        primary_shade = "600"
        secondary_shade = "600"
        tertiary_shade = "600"
        background_shade = "50"
        on_shade = "200"
        outline_shade = "700"
        container_shade = "300"
        on_background_shade = "900"
        primary_inverse_shade = "200"

    jsondoc = json.load(open(path_src, 'r'))

    def darken_color(colorName: str, color_hex: str, l: float, maxS: float) -> str:
        hls = get_hls(color_hex)
        new_hls = (hls[0], l, min(hls[2], maxS))
        # print(colorName, hls[2])
        new_rgb = colorsys.hls_to_rgb(*new_hls)
        new_rgb = tuple(int(255*x) for x in new_rgb)
        return ('#%02x%02x%02x' % new_rgb).upper()

    def cap_sat(color_hex: str, maxS: float) -> str:
        hls = get_hls(color_hex)
        new_hls = (hls[0], hls[1], min(hls[2], maxS))
        # print(colorName, hls[2])
        new_rgb = colorsys.hls_to_rgb(*new_hls)
        new_rgb = tuple(int(255*x) for x in new_rgb)
        return ('#%02x%02x%02x' % new_rgb).upper()

    def get_hls(color_hex: str) -> tuple:
        color_hex = color_hex.lstrip("#")
        rgb = tuple(int(color_hex[i:i+2], 16) for i in (0, 2, 4))
        rgb = tuple(x/255 for x in rgb)
        return colorsys.rgb_to_hls(*rgb)

    def add_transparency(alpha_hex: str, color_hex: str) -> str:
        return "#" + alpha_hex + color_hex[1:]

    styles_root = Element('resources')
    style = Element("style", {"name": "ColorPatch"})
    styles_root.append(style)

    colors_root = Element('resources')

    for color, shades in jsondoc.items():

        if primary_shade.lower() in shades:
            colorPrimary = shades[primary_shade]
            colorPrimaryContainer = shades[container_shade]
            colorOnPrimaryContainer = shades[outline_shade]
            colorOnPrimary = shades[on_shade]
            # colorNoti = shades[noti_shade]
            colorPrimaryInverse = shades[primary_inverse_shade]

            prefix = f"ColorPatch.{color.capitalize()}"
            style = Element("style", {"name": prefix+"_Main"})
            styles_root.append(style)
            elem = Element("item", {"name": "colorPrimary"})
            elem.text = colorPrimary
            style.append(elem)
            # elem = Element("item", {"name": "colorNoti"})
            # elem.text = colorNoti
            # style.append(elem)
            elem = Element("item", {"name": "colorPrimaryContainer"})
            elem.text = colorPrimaryContainer
            style.append(elem)
            elem = Element("item", {"name": "colorOnPrimary"})
            elem.text = colorOnPrimary
            style.append(elem)
            elem = Element("item", {"name": "colorOnPrimaryContainer"})
            elem.text = colorOnPrimaryContainer
            style.append(elem)
            elem = Element("item", {"name": "colorOnBackground"})
            elem.text = colorOnPrimaryContainer
            style.append(elem)
            elem = Element("item", {"name": "colorPrimaryInverse"})
            elem.text = colorPrimaryInverse
            style.append(elem)

        if secondary_shade.lower() in shades:
            colorSecondary = shades[secondary_shade]
            colorTertiary = cap_sat(shades[tertiary_shade], 0.45)
            colorOutline = add_transparency(
                "66", cap_sat(shades[outline_shade], 0.35))
            colorSecondaryContainer = shades[container_shade]
            colorOnSecondaryContainer = shades[outline_shade]
            colorOnSecondary = shades[on_shade]
            colorSurfaceVariant = add_transparency(
                "80",  cap_sat(shades[secondary_shade], 0.5))

            prefix = f"ColorPatch.{color.capitalize()}"
            style = Element("style", {"name": prefix+"_Secondary"})
            styles_root.append(style)
            elem = Element("item", {"name": "colorSecondary"})
            elem.text = colorSecondary
            style.append(elem)
            elem = Element("item", {"name": "colorTertiary"})
            elem.text = colorTertiary
            style.append(elem)
            elem = Element("item", {"name": "colorSurfaceVariant"})
            elem.text = colorSurfaceVariant
            style.append(elem)
            elem = Element("item", {"name": "colorOutline"})
            elem.text = colorOutline
            style.append(elem)
            elem = Element("item", {"name": "colorSecondaryContainer"})
            elem.text = colorSecondaryContainer
            style.append(elem)
            elem = Element("item", {"name": "colorOnSecondaryContainer"})
            elem.text = colorOnSecondaryContainer
            style.append(elem)
            elem = Element("item", {"name": "colorOnSecondary"})
            elem.text = colorOnSecondary
            style.append(elem)

        if background_shade.lower() in shades:

            if is_dark:
                colorBackground = darken_color(
                    color, shades[background_shade], 0.085, 0.13)
            else:
                colorBackground = darken_color(
                    color, shades[background_shade], 0.95, 0.5)
            # android bug: colorBackground has to be a reference on some devices
            color_element_name = color.capitalize() + "_Background"
            elem = Element("color", {"name": color_element_name})
            elem.text = colorBackground
            colors_root.append(elem)

            style = Element(
                "style", {"name": "ColorPatch." + color_element_name})
            styles_root.append(style)
            elem = Element("item", {"name": "android:colorBackground"})
            elem.text = f"@color/{color_element_name}"
            style.append(elem)
            elem = Element("item", {"name": "colorSurface"})
            elem.text = f"@color/{color_element_name}"
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
        style_id = "R.style." + name.replace(".", "_")
        line = f"        \"{name.replace('ColorPatch.', '').split('_')[0]}\" to {style_id},\n"
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


if __name__ == "__main__":
    gen_styles(False)
    gen_styles(True)
