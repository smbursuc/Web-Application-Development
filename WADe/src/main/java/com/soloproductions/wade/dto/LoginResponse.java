package com.soloproductions.wade.dto;

public class LoginResponse
{
    private String username;
    private String message;
    private String email;

    public LoginResponse(String username, String email, String message)
    {
        this.username = username;
        this.message = message;
        this.email = email;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
