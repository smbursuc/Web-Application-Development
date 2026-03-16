package com.soloproductions.wade.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DeepDetect response status block.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Status
{
    /** Numeric status code returned by DeepDetect. */
    private int code;

    /** Readable status message. */
    private String msg;

    /**
     * Returns the numeric status code.
     *
     * @return status code
     */
    public int getCode()
    {
        return code;
    }

    /**
     * Sets the numeric status code.
     *
     * @param code status code
     */
    public void setCode(int code)
    {
        this.code = code;
    }

    /**
     * Returns the status message.
     *
     * @return status message
     */
    public String getMsg()
    {
        return msg;
    }

    /**
     * Sets the status message.
     *
     * @param msg status message
     */
    public void setMsg(String msg)
    {
        this.msg = msg;
    }
}

/**
 * Single predicted class entry from DeepDetect.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class PredictionClass
{
    /** Probability score for this class prediction. */
    private double prob;

    /** Category label returned by DeepDetect. */
    private String cat;

    /**
     * Returns the prediction probability.
     *
     * @return probability score
     */
    public double getProb()
    {
        return prob;
    }

    /**
     * Sets the prediction probability.
     *
     * @param prob probability score
     */
    public void setProb(double prob)
    {
        this.prob = prob;
    }

    /**
     * Returns the category label.
     *
     * @return category label
     */
    public String getCat()
    {
        return cat;
    }

    /**
     * Sets the category label.
     *
     * @param cat category label
     */
    public void setCat(String cat)
    {
        this.cat = cat;
    }
}

/**
 * Prediction block for one input item (for example one image URI).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Prediction
{
    /** Input URI associated with this prediction block. */
    private String uri;

    /** Candidate class predictions for the input URI. */
    private List<PredictionClass> classes;

    /**
     * Returns the input URI.
     *
     * @return input URI
     */
    public String getUri()
    {
        return uri;
    }

    /**
     * Sets the input URI.
     *
     * @param uri input URI
     */
    public void setUri(String uri)
    {
        this.uri = uri;
    }

    /**
     * Returns predicted classes.
     *
     * @return class prediction list
     */
    public List<PredictionClass> getClasses()
    {
        return classes;
    }

    /**
     * Sets predicted classes.
     *
     * @param classes class prediction list
     */
    public void setClasses(List<PredictionClass> classes)
    {
        this.classes = classes;
    }
}

/**
 * DeepDetect response body containing prediction entries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Body
{
    /** Prediction list returned by DeepDetect. */
    private List<Prediction> predictions;

    /**
     * Returns predictions from the response body.
     *
     * @return prediction list
     */
    public List<Prediction> getPredictions()
    {
        return predictions;
    }

    /**
     * Sets predictions on the response body.
     *
     * @param predictions prediction list
     */
    public void setPredictions(List<Prediction> predictions)
    {
        this.predictions = predictions;
    }
}

/**
 * Root DeepDetect response payload wrapper.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class JsonResponse
{
    /** Response status block. */
    private Status status;

    /** Response body block with predictions. */
    private Body body;

    /**
     * Returns the status block.
     *
     * @return status block
     */
    public Status getStatus()
    {
        return status;
    }

    /**
     * Sets the status block.
     *
     * @param status status block
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }

    /**
     * Returns the body block.
     *
     * @return body block
     */
    public Body getBody()
    {
        return body;
    }

    /**
     * Sets the body block.
     *
     * @param body body block
     */
    public void setBody(Body body)
    {
        this.body = body;
    }
}

