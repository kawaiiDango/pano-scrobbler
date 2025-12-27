# from https://everynoise.com/


import httpx

from bs4 import BeautifulSoup

tag_fragments = set()


response = httpx.get("https://everynoise.com/everynoise1d.html")

soup = BeautifulSoup(response.text, "html.parser")

genres = soup.select("body table a:not(.note)")


for genreNode in genres:

    for g in genreNode.text.replace("-", " ").split():

        if len(g) > 1:
            tag_fragments.add(g)

print("\n".join(sorted(tag_fragments)), end='')
