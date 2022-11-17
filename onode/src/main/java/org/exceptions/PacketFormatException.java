package org.exceptions;

public class PacketFormatException extends Exception
{
    public PacketFormatException(String errorMessage)
    {
        super(errorMessage);
    }
}
