package com.soloproductions.wade.dto;

import java.util.List;

/**
 * DTO for image preprocessing requests.
 */
public class ProcessImageRequest
{
    /**
     * List of source image paths on the server or mounted storage.
     */
    private List<String> imagePaths;
    
    /**
     * Response type for the image processing request.
     */
    private String responseType;

    /**
     * Returns the list of image paths.
     *
     * @return  list of source image paths
     */
    public List<String> getImagePaths()
    {
        return imagePaths;
    }

    /**
     * Sets the list of image paths.
     *
     * @param   imagePaths
     *          list of source image paths
     */
    public void setImagePaths(List<String> imagePaths)
    {
        this.imagePaths = imagePaths;
    }

    /**
     * Returns the response type.
     *
     * @return  response type
     */
    public String getResponseType()
    {
        return responseType;
    }

    /**
     * Sets the response type.
     *
     * @param   responseType
     *          response type
     */
    public void setResponseType(String responseType)
    {
        this.responseType = responseType;
    }
}
