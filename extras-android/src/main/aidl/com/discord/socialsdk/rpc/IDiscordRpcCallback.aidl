package com.discord.socialsdk.rpc;

interface IDiscordRpcCallback {
    void onFrame(String frame);
    void onClose(int code, String reason);
}
