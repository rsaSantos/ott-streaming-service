package org.server.streaming;

public class RTPpacket{

    //size of the RTP header:
    static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    public int Version;
    public int Padding;
    public int Extension;
    public int CC;
    public int Marker;
    public int PayloadType;
    public int SequenceNumber;
    public int TimeStamp;
    public int Ssrc;

    //Bitstream of the RTP header
    public byte[] header;

    //size of the RTP payload
    public int payload_size;
    //Bitstream of the RTP payload
    public byte[] payload;


    //--------------------------
    //Constructor of an RTPpacket object from header fields and payload bitstream
    //--------------------------
    public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length){
        //fill by default header fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        //fill changing header fields:
        SequenceNumber = Framenb;
        TimeStamp = Time;
        PayloadType = PType;

        //build the header bistream:
        //--------------------------
        header = new byte[HEADER_SIZE];

        //.............
        //TO COMPLETE
        //.............
        //fill the header array of byte with RTP header fields
        header[0] = (byte)(Version << 6 | CC);
        header[1] = (byte)(PayloadType & 0x000000FF);
        header[2] = (byte)(SequenceNumber >> 8);
        header[3] = (byte)(SequenceNumber & 0xFF);
        header[4] = (byte)(TimeStamp >> 24);
        header[5] = (byte)(TimeStamp >> 16);
        header[6] = (byte)(TimeStamp >> 8);
        header[7] = (byte)(TimeStamp & 0xFF);
        header[8] = (byte)(0);
        header[9] = (byte)(0);
        header[10] = (byte)(0);
        header[11] = (byte)(0);

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
                + ", Padding: " + Padding
                + ", Extension: " + Extension
                + ", CC: " + CC
                + ", Marker: " + Marker
                + ", PayloadType: " + PayloadType
                + ", SequenceNumber: " + SequenceNumber
                + ", TimeStamp: " + TimeStamp);
    }
}

