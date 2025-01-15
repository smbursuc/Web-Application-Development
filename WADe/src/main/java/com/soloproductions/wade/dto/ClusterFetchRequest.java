package com.soloproductions.wade.dto;

public class ClusterFetchRequest
{
    String name;
    String sort;
    int range;
    int rangeStart;
    String sortType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSort()
    {
        return sort;
    }

    public void setSort(String sort)
    {
        this.sort = sort;
    }

    public int getRange()
    {
        return range;
    }

    public void setRange(int range)
    {
        this.range = range;
    }

    public int getRangeStart()
    {
        return rangeStart;
    }

    public void setRangeStart(int rangeStart)
    {
        this.rangeStart = rangeStart;
    }

    public String getSortType()
    {
        return sortType;
    }

    public void setSortType(String sortType)
    {
        this.sortType = sortType;
    }
}
