package org.client;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class StreamingClient implements Runnable
{
    //GUI
    //----
    private final JFrame jFrame;
    private final JButton refreshButton;
    private final JButton playButton;
    private final JButton pauseButton;
    private final JButton tearButton;
    private final JPanel mainPanel;
    private final JPanel buttonPanel;
    private final JLabel iconLabel;
    ImageIcon icon;


    //RTP variables:
    //----------------
    DatagramPacket rcvdp; //UDP packet received from the server (to receive)
    private final DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet

    Timer cTimer; //timer used to receive data from the UDP socket
    byte[] cBuf; //buffer used to store data received from the server

    private final String nodeAddress;
    private final String myAddress;
    private final int STREAMING_PORT;

    public StreamingClient(String myAddress, String nodeAddress, int STREAMING_PORT)
    {
        DatagramSocket RTPsocket1;
        this.myAddress = myAddress;
        this.nodeAddress = nodeAddress;
        this.STREAMING_PORT = STREAMING_PORT;
        this.jFrame = new JFrame("Cliente de Testes");
        this.refreshButton = new JButton("Refresh");
        this.playButton = new JButton("Play");
        this.pauseButton = new JButton("Pause");
        this.tearButton = new JButton("Teardown");
        this.mainPanel = new JPanel();
        this.buttonPanel = new JPanel();
        this.iconLabel = new JLabel();

        //init para a parte do cliente
        //--------------------------
        cTimer = new Timer(20, new clientTimerListener());
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000]; //allocate enough memory for the buffer used to receive data from the server

        try
        {
            // socket e video
            RTPsocket1 = new DatagramSocket(this.STREAMING_PORT); //init RTP socket (o mesmo para o cliente e servidor)
            RTPsocket1.setSoTimeout(5000); // setimeout to 5s
        }
        catch (SocketException e) {
            System.out.println("[" + LocalDateTime.now() + "]: Socket error: " + e.getMessage());
            RTPsocket1 = null;
            System.exit(1);
        }
        this.RTPsocket = RTPsocket1;
    }

    @Override
    public void run()
    {
        this.buildGUI();
    }

    private void buildGUI()
    {
        //build GUI
        //--------------------------

        //Frame
        this.jFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(refreshButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);

        // handlers... (so dois)
        this.playButton.addActionListener(new playButtonListener());
        this.tearButton.addActionListener(new tearButtonListener());
        this.refreshButton.addActionListener(new refreshButtonListener());


        //Image display label
        this.iconLabel.setIcon(null);

        //frame layout
        this.mainPanel.setLayout(null);
        this.mainPanel.add(this.iconLabel);
        this.mainPanel.add(this.buttonPanel);
        this.iconLabel.setBounds(0,0,380,280);
        this.buttonPanel.setBounds(0,280,380,50);

        this.jFrame.getContentPane().add(this.mainPanel, BorderLayout.CENTER);
        this.jFrame.setSize(new Dimension(390,370));
        this.jFrame.setVisible(true);
    }


    //------------------------------------
    //Handler for buttons
    //------------------------------------

    //Handler for refresh button
    // (disabled on latest version)
    //-----------------------
    class refreshButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e)
        {
            // Sending activate packet to node address
            try {
                Socket socket = new Socket(InetAddress.getByName(nodeAddress), Main.CONTROL_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("4;");
                dos.flush();
                dos.close();
                socket.close();
            }
            catch (IOException ex)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Failed to send activation packet to node [" + nodeAddress + "].");
            }

            System.out.println("[" + LocalDateTime.now() + "]: Refresh Button pressed !");
        }
    }

    //Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e)
        {
            // Sending activate packet to node address
            try {
                Socket socket = new Socket(InetAddress.getByName(nodeAddress), Main.CONTROL_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("2;" + myAddress);
                dos.flush();
                dos.close();
                socket.close();
            }
            catch (IOException ex)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Failed to send activation packet to node [" + nodeAddress + "].");
            }

            System.out.println("[" + LocalDateTime.now() + "]: Play Button pressed !");
            //start the timers ...
            cTimer.start();
        }
    }

    //Handler for tear button
    //-----------------------
    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e)
        {
            System.out.println("[" + LocalDateTime.now() + "]: Teardown Button pressed !");

            // Send DEACTIVATE_PACKET to node.
            try
            {
                Socket socket = new Socket(InetAddress.getByName(nodeAddress), Main.CONTROL_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("3;" + myAddress);
                dos.flush();
                dos.close();
                socket.close();
                System.out.println("[" + LocalDateTime.now() + "]: Sent deactivation packet to [" + nodeAddress + "].");
            }
            catch (IOException ex)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Error sending deactivation packet.");
            }

            //stop the timer
            cTimer.stop();
            //exit
            System.exit(0);
        }
    }

    //------------------------------------
    //Handler for timer (para cliente)
    //------------------------------------

    class clientTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(cBuf, cBuf.length);

            try{
                //receive the DP from the socket:
                RTPsocket.receive(rcvdp);

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                //display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            }
            catch (InterruptedIOException iioe){
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }
    }
}


