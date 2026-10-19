package com.hrgenius.exception;

/** Thrown when a self-registered account signs in before an administrator has approved it. */
public class AccountPendingApprovalException extends RuntimeException {
    public AccountPendingApprovalException() {
        super("Your account is waiting for administrator approval. You will be able to sign in once it is approved.");
    }
}
