package com.soloproductions.wade.pojo;

/**
 * Wrapper object for request body payload data used by dataset CRUD operations.
 */
public class DataEntry
{
    /** 
     * Raw payload object from the request body. 
     * 
     * this is object because Jackson complained about using DataEntity (abstract type)
     * TODO: restrict this?
     */
    private Object data;

    /**
     * Returns the raw payload data.
     *
     * @return  raw payload data
     */
    public Object getData()
    {
        return data;
    }

    /**
     * Sets the raw payload data.
     *
     * @param   data
     *          raw payload data
     */
    public void setData(Object data)
    {
        this.data = data;
    }
}
