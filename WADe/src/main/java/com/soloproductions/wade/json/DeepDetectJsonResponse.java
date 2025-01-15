package com.soloproductions.wade.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class Status
{
    private int code;
    private String msg;

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class PredictionClass
{
    private double prob;
    private String cat;

    public double getProb()
    {
        return prob;
    }

    public void setProb(double prob)
    {
        this.prob = prob;
    }

    public String getCat()
    {
        return cat;
    }

    public void setCat(String cat)
    {
        this.cat = cat;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Prediction
{
    private String uri;
    private List<PredictionClass> classes;

    public String getUri()
    {
        return uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public List<PredictionClass> getClasses()
    {
        return classes;
    }

    public void setClasses(List<PredictionClass> classes)
    {
        this.classes = classes;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Body
{
    private List<Prediction> predictions;

    public List<Prediction> getPredictions()
    {
        return predictions;
    }

    public void setPredictions(List<Prediction> predictions)
    {
        this.predictions = predictions;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class JsonResponse
{
    private Status status;
    private Body body;

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public Body getBody()
    {
        return body;
    }

    public void setBody(Body body)
    {
        this.body = body;
    }
}

