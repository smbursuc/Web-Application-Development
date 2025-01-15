package com.soloproductions.wade.dto;

public class StandardResponse<T>
{
    private String status; // "success" or "error"
    private String message;
    private T data;

    public StandardResponse(String status, String message, T data)
    {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public T getData()
    {
        return data;
    }

    public void setData(T data)
    {
        this.data = data;
    }
}
