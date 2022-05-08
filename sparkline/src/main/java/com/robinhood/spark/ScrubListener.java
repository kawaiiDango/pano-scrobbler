package com.robinhood.spark;

interface ScrubListener {
    void onScrubbed(float x, float y);
    void onScrubEnded();
}