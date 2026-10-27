package com.discord.socialsdk.rpc;

interface IDiscordRpcConnection {
    void sendFrame(String frame);
    void disconnect();
}
