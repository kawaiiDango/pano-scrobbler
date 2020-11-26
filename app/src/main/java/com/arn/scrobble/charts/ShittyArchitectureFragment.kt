package com.arn.scrobble.charts

import androidx.fragment.app.Fragment


open class ShittyArchitectureFragment: Fragment() {
    lateinit var adapter: ChartsOverviewAdapter
    lateinit var viewModel: ChartsVM
}

class FakeArtistFragment: ShittyArchitectureFragment()
class FakeAlbumFragment: ShittyArchitectureFragment()
class FakeTrackFragment: ShittyArchitectureFragment()