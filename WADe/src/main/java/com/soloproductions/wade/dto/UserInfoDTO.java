package com.soloproductions.wade.dto;

/**
 * DTO for representing user information, including username and email.
 * Primarly used for authentication.
 */
public class UserInfoDTO 
{
    /** The username of the user. */
    private String username;

    /** The email of the user. */
    private String email;

    /**
     * Constructs a new UserInfoDTO with the given username and email.
     * 
     * @param   username    
     *          The username of the user.
     * @param   email       
     *          The email of the user.
     */
    public UserInfoDTO(String username, String email) 
    {
        this.username = username;
        this.email = email;
    }

    /**
     * Gets the username of the user.
     * 
     * @return   The username of the user.
     */
    public String getUsername() 
    {
        return username;
    }

    /**
     * Sets the username of the user.
     * 
     * @param   username    
     *          The username of the user.
     */
    public void setUsername(String username) 
    {
        this.username = username;
    }

    /**
     * Gets the email of the user.
     * 
     * @return   The email of the user.
     */
    public String getEmail() 
    {
        return email;
    }

    /**
     * Sets the email of the user.
     * 
     * @param   email       
     *          The email of the user.
     */
    public void setEmail(String email) 
    {
        this.email = email;
    }
}
