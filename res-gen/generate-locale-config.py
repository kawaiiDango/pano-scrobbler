import re
from lxml import etree, objectify
from lxml.etree import Element, SubElement, QName

path_dest_xml = "../app/src/main/res/xml/locales_config.xml"
path_src_kt = "../app/src/main/java/com/arn/scrobble/LocaleUtils.kt"


def extract_locale_codes() -> list[str]:
    with open(path_src_kt, 'r') as f:
        text = f.read()
        text = text[text.find("val localesSet"):text.find(".toSortedSet()")]
        matches = re.findall(r'"(.*?)"', text)
        matches.sort()
        return matches


def write_xml(locales: list[str]):
    root = Element('locale-config', nsmap={
        'android': "http://schemas.android.com/apk/res/android",
    })
    for locale in locales:
        locale_elem = SubElement(root, "locale", {
            QName("http://schemas.android.com/apk/res/android", 'name'): locale
        })
        root.append(locale_elem)

    objectify.deannotate(root, cleanup_namespaces=False)
    etree.ElementTree(root)\
        .write(path_dest_xml, encoding='utf-8', pretty_print=True, xml_declaration=True)


if __name__ == "__main__":
    locales = extract_locale_codes()
    write_xml(locales)
