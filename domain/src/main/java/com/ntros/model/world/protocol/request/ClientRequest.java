package com.ntros.model.world.protocol.request;

public sealed interface ClientRequest permits JoinRequest, MoveRequest, CreateRequest,
    RemoveRequest {

}
