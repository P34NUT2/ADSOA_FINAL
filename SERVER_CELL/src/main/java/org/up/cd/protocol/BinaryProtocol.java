package org.up.cd.protocol;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class BinaryProtocol {

    public static final int SERVICE_ACK = 0;

    private String originBusiness;
    private String originSubsystem;
    private String originEntity;
    private String destBusiness;
    private String destSubsystem;
    private String destEntity;
    private long eventId;
    private int serviceNumber;
    private byte[] data;

    public BinaryProtocol() {}

    public BinaryProtocol(String originBusiness, String originSubsystem, String originEntity,
                          String destBusiness, String destSubsystem, String destEntity,
                          long eventId, int serviceNumber, byte[] data) {
        this.originBusiness = originBusiness;
        this.originSubsystem = originSubsystem;
        this.originEntity = originEntity;
        this.destBusiness = destBusiness;
        this.destSubsystem = destSubsystem;
        this.destEntity = destEntity;
        this.eventId = eventId;
        this.serviceNumber = serviceNumber;
        this.data = data;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        writeFixed(dos, originBusiness);
        writeFixed(dos, originSubsystem);
        writeFixed(dos, originEntity);
        writeFixed(dos, destBusiness);
        writeFixed(dos, destSubsystem);
        writeFixed(dos, destEntity);
        dos.writeLong(eventId);
        dos.writeInt(serviceNumber);

        if (data == null || data.length == 0) {
            dos.writeInt(0);
        } else {
            dos.writeInt(data.length);
            dos.write(data);
        }

        return baos.toByteArray();
    }

    public static BinaryProtocol deserialize(byte[] raw) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(raw));
        BinaryProtocol msg = new BinaryProtocol();

        msg.originBusiness  = readFixed(dis);
        msg.originSubsystem = readFixed(dis);
        msg.originEntity    = readFixed(dis);
        msg.destBusiness    = readFixed(dis);
        msg.destSubsystem   = readFixed(dis);
        msg.destEntity      = readFixed(dis);
        msg.eventId         = dis.readLong();
        msg.serviceNumber   = dis.readInt();

        int len = dis.readInt();
        if (len > 0) {
            msg.data = new byte[len];
            dis.readFully(msg.data);
        } else {
            msg.data = new byte[0];
        }

        return msg;
    }

    private void writeFixed(DataOutputStream dos, String s) throws IOException {
        byte[] raw = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] fixed = new byte[16];
        System.arraycopy(raw, 0, fixed, 0, Math.min(raw.length, 16));
        dos.write(fixed);
    }

    private static String readFixed(DataInputStream dis) throws IOException {
        byte[] fixed = new byte[16];
        dis.readFully(fixed);
        return new String(fixed, StandardCharsets.UTF_8).trim();
    }

    public String getOriginBusiness()  { return originBusiness; }
    public String getOriginSubsystem() { return originSubsystem; }
    public String getOriginEntity()    { return originEntity; }
    public String getDestBusiness()    { return destBusiness; }
    public String getDestSubsystem()   { return destSubsystem; }
    public String getDestEntity()      { return destEntity; }
    public long   getEventId()         { return eventId; }
    public int    getServiceNumber()   { return serviceNumber; }
    public byte[] getData()            { return data; }
}
