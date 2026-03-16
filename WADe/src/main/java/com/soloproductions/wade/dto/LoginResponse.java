package com.soloproductions.wade.dto;

/**
 * Authentication response payload returned after login.
 */
public class LoginResponse
{
    /** Authenticated username. */
    private String username;

    /** Human-readable authentication result message. */
    private String message;

    /**
     * Constructs a login response payload.
     *
     * @param   username
     *          authenticated username
     * @param   message
     *          result message to show to the client
     */
    public LoginResponse(String username, String message)
    {
        this.username = username;
        this.message = message;
    }

    /**
     * Returns the authenticated username.
     *
     * @return  username value
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Sets the authenticated username.
     *
     * @param   username
     *          username value
     */
    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Returns the result message.
     *
     * @return  message value
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Sets the result message.
     *
     * @param   message
     *          message value
     */
    public void setMessage(String message)
    {
        this.message = message;
    }
}
