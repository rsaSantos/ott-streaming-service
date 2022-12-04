package org.server.streaming;

import java.time.Instant;

public class RTPpacketServer {

    //size of the RTP header:
    static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    public int Version;
    public int CC;
    public int PayloadType;
    public int SequenceNumber;
    public long TimeStamp;

    //Bitstream of the RTP header
    public byte[] header;

    //size of the RTP payload
    public int payload_size;
    //Bitstream of the RTP payload
    public byte[] payload;


    //--------------------------
    //Constructor of an RTPpacketServer object from header fields and payload bitstream
    //--------------------------
    public RTPpacketServer(int PType, int Framenb, byte[] data, int data_length){
        //fill by default header fields:
        Version = 2;
        CC = 0;

        //fill changing header fields:
        SequenceNumber = Framenb;
        TimeStamp = Instant.now().toEpochMilli();
        PayloadType = PType;

        //build the header bistream:
        //--------------------------
        header = new byte[HEADER_SIZE];

        //fill the header array of byte with RTP header fields
        header[0] = (byte)(Version << 6 | CC);
        header[1] = (byte)(PayloadType & 0x000000FF);
        header[2] = (byte)(SequenceNumber >> 8);
        header[3] = (byte)(SequenceNumber & 0xFF);

        // server timestamp
        header[4] = (byte)(TimeStamp >> 56);
        header[5] = (byte)(TimeStamp >> 48);
        header[6] = (byte)(TimeStamp >> 40);
        header[7] = (byte)(TimeStamp >> 32);
        header[8] = (byte)(TimeStamp >> 24);
        header[9] = (byte)(TimeStamp >> 16);
        header[10] = (byte)(TimeStamp >> 8);
        header[11] = (byte)(TimeStamp & 0xFF);

        //fill the payload bitstream:
        //--------------------------
        payload_size = data_length;
        payload = new byte[data_length];

        //fill payload array of byte from data (given in parameter of the constructor)
        //......
        System.arraycopy(data, 0, payload, 0, data_length);

        // ! Do not forget to uncomment method printheader() below !

    }

    //--------------------------
    //getlength: return the total length of the RTP packet
    //--------------------------
    public int getlength() {
        return(payload_size + HEADER_SIZE);
    }

    //--------------------------
    //getpacket: returns the packet bitstream via pointer
    //--------------------------
    public void getpacket(byte[] packet)
    {
        //construct the packet = header + payload
        if (HEADER_SIZE >= 0) System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        if (payload_size >= 0) System.arraycopy(payload, 0, packet, HEADER_SIZE, payload_size);
    }

    //--------------------------
    //print headers without the SSRC
    //--------------------------
    public void printheader()
    {
        System.out.print("[RTP-Header] ");
        System.out.println("Version: " + Version
                + ", CC: " + CC
                + ", PayloadType: " + PayloadType
                + ", SequenceNumber: " + SequenceNumber
                + ", TimeStamp: " + TimeStamp);
    }
}

