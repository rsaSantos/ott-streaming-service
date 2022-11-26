package org.onode.control.starter;

import org.onode.utils.Triplet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface IStarter
{
    public final static int ERROR = 0;
    public final static int GOOD_DROP = 1;
    public final static int GOOD_KEEP = 2;
    public final static String OK = "OK";
    public final static String NOT_OK = "NOK";

}
