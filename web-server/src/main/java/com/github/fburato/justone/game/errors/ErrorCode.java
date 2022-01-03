package com.github.fburato.justone.game.errors;

public enum ErrorCode {
    PAYLOAD_TYPE_MISMATCH(400_001),
    INVALID_PAYLOAD(400_002),

    NO_HOST(400_003),
    NOT_ENOUGH_PLAYERS(400_004),
    NO_ID(400_005),
    NOT_ENOUGH_WORDS(400_006),

    ILLEGAL_ACTION(401_001),

    UNRECOGNISED_STATE(500_001),
    UNKNOWN(999_999);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
