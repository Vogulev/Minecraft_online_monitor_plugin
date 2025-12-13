package com.vogulev.online_monitor.utils;


import java.util.Collection;


public final class NumericUtils
{
    private NumericUtils()
    {
    }


    public static int parseIntOrDefault(final String value, final int defaultValue)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }


    public static double getMaxValue(final Collection<Double> values)
    {
        return values.stream().max(Double::compare).orElse(1.0);
    }
}
