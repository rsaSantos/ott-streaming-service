package org.client;


import java.nio.ByteBuffer;
import java.time.Instant;

public class RTPpacketClient {

    //size of the RTP header:
    static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    public int Version;
    public int CC;
    public int PayloadType;
    public int SequenceNumber;
    public long serverTimestamp;
    public long elapsedTime;

    //Bitstream of the RTP header
    public byte[] header;

    //size of the RTP payload
    public int payload_size;
    //Bitstream of the RTP payload
    public byte[] payload;

    //--------------------------
    //Constructor of an RTPpacketClient object from the packet bistream
    //--------------------------
        public RTPpacketClient(byte[] packet, int packet_size)
    {
        //fill default fields:
        Version = 2;
        CC = 0;

        //check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE)
        {
            //get the header bitsream:
            header = new byte[HEADER_SIZE];
            System.arraycopy(packet, 0, header, 0, HEADER_SIZE);

            //get the payload bitstream:
            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];
            if (packet_size - HEADER_SIZE >= 0)
                System.arraycopy(packet, HEADER_SIZE, payload, 0, packet_size - HEADER_SIZE);

            //interpret the changing fields of the header:
            PayloadType = header[1] & 127;
            SequenceNumber = unsigned_int(header[3]) + 256*unsigned_int(header[2]);

            serverTimestamp = ByteBuffer.wrap(header, 4, 8).getLong();
            elapsedTime = Instant.now().getEpochSecond() - serverTimestamp;
        }
    }

    //--------------------------
    //getpayload: return the payload bistream of the RTPpacketClient via pointer
    //--------------------------
    public void getpayload(byte[] data) {

        if (payload_size >= 0) System.arraycopy(payload, 0, data, 0, payload_size);

    }

    //--------------------------
    //getpayload_length: return the length of the payload
    //--------------------------
    public int getpayload_length() {
        return(payload_size);
    }

    //--------------------------
    //gettimestamp
    //--------------------------

    public long getelapsedTime() {
        return(elapsedTime);
    }

    //--------------------------
    //getsequencenumber
    //--------------------------
    public int getsequencenumber() {
        return(SequenceNumber);
    }

    //--------------------------
    //getpayloadtype
    //--------------------------
    public int getpayloadtype() {
        return(PayloadType);
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
                + ", ServerTimeStamp: " + serverTimestamp);
    }

    //return the unsigned value of 8-bit integer nb
    static int unsigned_int(int nb) {
        if (nb >= 0)
            return(nb);
        else
            return(256+nb);
    }

}