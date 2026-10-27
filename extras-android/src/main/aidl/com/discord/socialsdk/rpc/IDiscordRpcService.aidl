package com.discord.socialsdk.rpc;

import com.discord.socialsdk.rpc.IDiscordRpcCallback;
import com.discord.socialsdk.rpc.IDiscordRpcConnection;


interface IDiscordRpcService {
    // returns null if the handshake is rejected
    IDiscordRpcConnection connect(long applicationId, String rpcVersion, IDiscordRpcCallback callback);
}
