# from https://everynoise.com/

import httpx
from bs4 import BeautifulSoup

tags = set()

response = httpx.get("https://everynoise.com/everynoise1d.cgi?scope=all")
soup = BeautifulSoup(response.text, "html.parser")
genres = soup.select("body table a:not(.note)")

for genreNode in genres:
    for g in genreNode.text.replace("-", " ").split():
        tags.add(g)
        
print("""package com.arn.scrobble

class AcceptableTags {
    private val tags = setOf(""")
for tag in tags:
    print(f'        "{tag}",')    
print("""
    )
    
    fun isAcceptable(lastfmTag: String) =
        lastfmTag.isNotEmpty() &&
                lastfmTag.lowercase().split(" ", "-").any { it in tags }
}
""")