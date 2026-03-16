package com.soloproductions.wade.json;

import java.util.List;

/**
 * Interface for parsing raw model responses into simplified output rows.
 * This is an abandoned concept where there could be multiple parsers, but there was
 * never a need for another parser.
 * 
 * Used by the DeepDetect component.
 */
public interface ResponseParser
{
    /**
     * Parses a raw response payload and returns normalized result rows.
     *
     * @param response raw model response payload
     * @return parsed response rows
     */
    List<String> parseResponse(String response);
}
