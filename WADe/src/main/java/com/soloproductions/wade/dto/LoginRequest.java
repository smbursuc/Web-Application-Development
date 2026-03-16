package com.soloproductions.wade.dto;

/**
 * Authentication request payload for user login.
 */
public class LoginRequest
{
    /** Username used for authentication. */
    private String username;

    /** Plain-text password submitted for authentication. */
    private String password;

    /**
     * Returns the submitted username.
     *
     * @return  username value
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Sets the submitted username.
     *
     * @param   username
     *          username value
     */
    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Returns the submitted password.
     *
     * @return  password value
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the submitted password.
     *
     * @param   password
     *          password value
     */
    public void setPassword(String password)
    {
        this.password = password;
    }
}
