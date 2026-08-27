package com.hresources.hr.policy_assistant.config;

/**
 * Base runtime exception for application-level errors in the policy assistant.
 */
public class PolicyAssistantException extends RuntimeException {

    /**
     * Creates an application exception with a message.
     *
     * @param message exception message
     */
    public PolicyAssistantException(String message) {
        super(message);
    }

    /**
     * Creates an application exception with a message and root cause.
     *
     * @param message exception message
     * @param cause underlying exception cause
     */
    public PolicyAssistantException(String message, Throwable cause) {
        super(message, cause);
    }
}
