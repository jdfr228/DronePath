/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */
         
// MESSAGE SIM_STATE PACKING
package com.MAVLink.common;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.ardupilotmega.CRC;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
* Status of simulation environment, if used
*/
public class msg_sim_state_test{

public static final int MAVLINK_MSG_ID_SIM_STATE = 108;
public static final int MAVLINK_MSG_LENGTH = 84;
private static final long serialVersionUID = MAVLINK_MSG_ID_SIM_STATE;

private Parser parser = new Parser();

public CRC generateCRC(byte[] packet){
    CRC crc = new CRC();
    for (int i = 1; i < packet.length - 2; i++) {
        crc.update_checksum(packet[i] & 0xFF);
    }
    crc.finish_checksum(MAVLINK_MSG_ID_SIM_STATE);
    return crc;
}

public byte[] generateTestPacket(){
    ByteBuffer payload = ByteBuffer.allocate(6 + MAVLINK_MSG_LENGTH + 2);
    payload.put((byte)MAVLinkPacket.MAVLINK_STX); //stx
    payload.put((byte)MAVLINK_MSG_LENGTH); //len
    payload.put((byte)0); //seq
    payload.put((byte)255); //sysid
    payload.put((byte)190); //comp id
    payload.put((byte)MAVLINK_MSG_ID_SIM_STATE); //msg id
    payload.putFloat((float)17.0); //q1
    payload.putFloat((float)45.0); //q2
    payload.putFloat((float)73.0); //q3
    payload.putFloat((float)101.0); //q4
    payload.putFloat((float)129.0); //roll
    payload.putFloat((float)157.0); //pitch
    payload.putFloat((float)185.0); //yaw
    payload.putFloat((float)213.0); //xacc
    payload.putFloat((float)241.0); //yacc
    payload.putFloat((float)269.0); //zacc
    payload.putFloat((float)297.0); //xgyro
    payload.putFloat((float)325.0); //ygyro
    payload.putFloat((float)353.0); //zgyro
    payload.putFloat((float)381.0); //lat
    payload.putFloat((float)409.0); //lon
    payload.putFloat((float)437.0); //alt
    payload.putFloat((float)465.0); //std_dev_horz
    payload.putFloat((float)493.0); //std_dev_vert
    payload.putFloat((float)521.0); //vn
    payload.putFloat((float)549.0); //ve
    payload.putFloat((float)577.0); //vd
    
    CRC crc = generateCRC(payload.array());
    payload.put((byte)crc.getLSB());
    payload.put((byte)crc.getMSB());
    return payload.array();
}

@Test
public void test(){
    byte[] packet = generateTestPacket();
    for(int i = 0; i < packet.length - 1; i++){
        parser.mavlink_parse_char(packet[i] & 0xFF);
    }
    MAVLinkPacket m = parser.mavlink_parse_char(packet[packet.length - 1] & 0xFF);
    byte[] processedPacket = m.encodePacket();
    assertArrayEquals("msg_sim_state", processedPacket, packet);
}
}
        