package com.arn.scrobble.scrobbleable

import com.arn.scrobble.Stuff
import com.arn.scrobble.Tokens

class Librefm : Lastfm() {
    override var apiRoot = Stuff.LIBREFM_API_ROOT
    override val apiKey = Stuff.LIBREFM_KEY
    override val secret = Stuff.LIBREFM_KEY
}