package com.soloproductions.wade.dto;

import java.util.List;

public class ProcessImageRequest
{
    private List<String> imagePaths;
    private String responseType;

    public List<String> getImagePaths()
    {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths)
    {
        this.imagePaths = imagePaths;
    }

    public String getResponseType()
    {
        return responseType;
    }

    public void setResponseType(String responseType)
    {
        this.responseType = responseType;
    }
}
