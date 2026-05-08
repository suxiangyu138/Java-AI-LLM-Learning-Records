package com.qqchat.protocol;

public enum Command {
    // Authentication
    REGISTER,
    LOGIN,
    LOGOUT,
    AUTH_OK,
    AUTH_FAIL,

    // Friend Management
    ADD_FRIEND,
    DEL_FRIEND,
    FRIEND_LIST,
    FRIEND_LIST_RESP,
    FRIEND_REQUEST,
    FRIEND_ONLINE,
    FRIEND_OFFLINE,

    // Private Messaging
    SEND_MSG,
    RECV_MSG,
    MSG_DELIVERED,
    OFFLINE_MSGS,

    // Group Management
    CREATE_GROUP,
    JOIN_GROUP,
    LEAVE_GROUP,
    GROUP_LIST,
    GROUP_LIST_RESP,
    GROUP_INFO,
    GROUP_INFO_RESP,

    // Group Chat
    SEND_GROUP_MSG,
    RECV_GROUP_MSG,

    // Profile
    GET_PROFILE,
    PROFILE_RESP,
    UPDATE_PROFILE,
    SEARCH_USER,
    SEARCH_RESP,

    // System
    PING,
    PONG,
    ERROR,
    SERVER_SHUTDOWN
}
