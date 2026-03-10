package com.ntros.command.vo;

public sealed interface CommandResult permits Response, Deferred, Failure {

}
