package com.soloproductions.wade.dto;

import com.soloproductions.wade.controller.ResponseStatus;

/**
 * Generic server response structure for an API request.
 */
public class StandardResponse<T>
{
    /** The status of the request. At the moment only "success" and "error" are supported. */
    private String status;

    /** The message associated with the response. */
    private String message;

    /** 
     * The data associated with the response. This could be of type {@link Object}, but 
     * for type safety and clarity, use a generic type parameter.
    */
    private T data;

    /**
     * Constructs a new StandardResponse with the given status, message, and data.
     * 
     * @param   status
     *          The status of the request.
     * @param   message
     *          The message associated with the response.
     * @param   data
     *          The data associated with the response.
     */
    public StandardResponse(String status, String message, T data)
    {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * Resolves the response status string from the given enum value. This is used to convert the 
     * internal enum representation of the response status to a string that can be sent in the API response.
     * 
     * @param   rs
     *          The response status enum value to resolve.
     * 
     * @return  The corresponding response status string.
     */
    public static String resolveResponseStatusFromEnum(ResponseStatus rs)
    {
        return switch (rs)
        {
            case FAIL -> "error";
            case SUCCESS -> "success";
        };
    }

    /**
     * Gets the status of the response. See {@link #status}.
     * 
     * @return   The status of the response.
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Sets the status of the response. See {@link #status}.
     * 
     * @param   status
     *          The status of the response.
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
    * Gets the message associated with the response.
    * 
    * @return   The message associated with the response.
    */
    public String getMessage()
    {
        return message;
    }

    /**
     * Sets the message associated with the response.
     * 
     * @param   message
     *          The message associated with the response.
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * Gets the data associated with the response.
     * 
     * @return   The data associated with the response.
     */
    public T getData()
    {
        return data;
    }

    /**
     * Sets the data associated with the response.
     * 
     * @param   data
     *          The data associated with the response.
     */
    public void setData(T data)
    {
        this.data = data;
    }
}
