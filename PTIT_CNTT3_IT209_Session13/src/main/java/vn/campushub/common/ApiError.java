package vn.campushub.common;

public record ApiError(int status, String code, String message) {
}
