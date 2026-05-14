/** 
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
 * 
 */


package hwb1;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;
// import javacardx.annotations.*;
// import static hwb1.FirstAppletStrings.*;

/**
 * Applet class
 * 
 * @author <ismoiabd>
 */
// @StringPool(value = {
// 	    @StringDef(name = "Package", value = "hwb1"),
// 	    @StringDef(name = "AppletName", value = "FirstApplet")},
// 	    // Insert your strings here 
// 	name = "FirstAppletStrings")
public class FirstApplet extends Applet {

    OwnerPIN pin;
    AESKey enc_key, mac_key;
    Cipher enc;
    Signature mac; // pro vypocet a overeni MACu
    static final byte[] my_name = {'Z', 'A', 'K'}; // {'A', 'B', 'D', 'U','L','A','Z','I','Z'}
    byte[] user_data;
    short user_length;
    byte failed_attempts = 0;


    static final byte[] enc_key_data = {
       0x00, 0x01, 0x02, 0x03,
       0x00, 0x01, 0x02, 0x03,
       0x00, 0x01, 0x02, 0x03,
       0x00, 0x01, 0x02, 0x03
    };

    static final byte[] mac_key_data = {
        0x0a, 0x01, 0x02, 0x03,
        0x0b, 0x01, 0x02, 0x03,
        0x0c, 0x01, 0x02, 0x03,
        0x0d, 0x01, 0x02, 0x03
    };

    
    /**
     * Installs this applet.
     * 
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new FirstApplet(bArray, bOffset, bLength);
    }

    protected FirstApplet(byte[] bArray, short bOffset, byte bLength) {
        user_data = JCSystem.makeTransientByteArray((short)80, JCSystem.CLEAR_ON_RESET);
        
        byte iLen = bArray[bOffset]; // aid length
        bOffset = (short) (bOffset + iLen + 1);
        byte cLen = bArray[bOffset]; // info length
        bOffset = (short) (bOffset + cLen + 1);
        byte aLen = bArray[bOffset]; // applet data length
        bOffset = (short)(bOffset + 1);
        
        pin = new OwnerPIN((byte) 3, (byte) 4);  // 3 tries, 4-byte PIN
        pin.update(bArray, bOffset, (byte) 4); // PIN je 4B always

        enc_key = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        enc_key.setKey(enc_key_data, (short) 0);

        mac_key = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        mac_key.setKey(mac_key_data, (short) 0);

        enc = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        mac = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);

        register();
    }

    //@Override
    public boolean select() {
    if (failed_attempts >= (short)3) {
        return false;
    }
        return super.select();
    }


    public void process(APDU apdu) { 
        
       if (selectingApplet()) {
            ISOException.throwIt(ISO7816.SW_NO_ERROR);
        }

       byte[] buf = apdu.getBuffer();
       if (buf[ISO7816.OFFSET_CLA] != (byte)0x80) { 
           ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED); // 0x6E00
       }
        short len;

        switch (buf[ISO7816.OFFSET_INS]) {
            case 0x00: // Send my name
                len = apdu.setOutgoing();
                if (len > (short)my_name.length) { // Pokud muze byt odeslano voice dat nez mame, omezime
                    len = (short) my_name.length;
                }
                apdu.setOutgoingLength(len);
                apdu.sendBytesLong(my_name, (short) 0, len); //z pozice 0 do len
                break;

            case 0x02: // Receive data
                if (!pin.isValidated()) {
                    ISOException.throwIt((short)0x6301); // 0x6301 ISO7816.SW_PIN_VERIFICATION_REQUIRED
                }

                len = apdu.setIncomingAndReceive(); //prijme data a vraci delku dat
                if (len > (short)20) { // Max delka
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                user_length = len;
                Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, user_data, (short) 0, len);
                break;

            case 0x04: // Send stored data
                if (!pin.isValidated()) {
                	ISOException.throwIt((short)0x6301); // 0x6301 ISO7816.SW_PIN_VERIFICATION_REQUIRED
                }

                len = apdu.setOutgoing();
                if (len != (short) user_length) {
                    ISOException.throwIt((short) (ISO7816.SW_CORRECT_LENGTH_00 + user_length));//0x6Cxx  xx je spravny pocet bajtu
                }
                apdu.setOutgoingLength(user_length);
                apdu.sendBytesLong(user_data, (short) 0, user_length);
                break;

            case 0x20: // Verify PIN
                len = apdu.setIncomingAndReceive();
                if (len != (short)4) {
                    ISOException.throwIt((short) 0x6A00); // 0x6A00 SW_DATA_INVALID
                }
                
                if (!pin.check(buf, ISO7816.OFFSET_CDATA, (byte) len)) {
                    failed_attempts++;
                    
                    ISOException.throwIt((short)0x6300); //0x6300 ISO7816.SW_VERIFICATION_FAILED
                }else {
                	failed_attempts = (short)0; 
                }
                break;

            case 0x42: // Encrypt and sign with MAC
                if (!pin.isValidated()) {
                	ISOException.throwIt((short)0x6301); // 0x6301 ISO7816.SW_PIN_VERIFICATION_REQUIRED
                }
                
                len = apdu.setIncomingAndReceive();
                if (len % (short)16 != (short)0 || len > (short)64) { 
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
               
                try {
                	enc.init(enc_key, Cipher.MODE_ENCRYPT);  //Inicializuje AES SIFROVANI s klicem enc_key
                    short enc_len = enc.doFinal(buf, ISO7816.OFFSET_CDATA, len, user_data, (short) 0); //user_data - vystupni buffer
                    mac.init(mac_key, Signature.MODE_SIGN);
                    short mac_len = mac.sign(user_data, (short) 0, enc_len, user_data, enc_len); 
                    if ((short) (enc_len + mac_len) > user_data.length) {
                        ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                    }

                    apdu.setOutgoing();
                    apdu.setOutgoingLength((short) (enc_len + mac_len));
                    apdu.sendBytesLong(user_data, (short) 0, (short) (enc_len + mac_len));
                } catch (CryptoException e) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                break;

            case 0x44: //decrypt
                if (!pin.isValidated()) {
                    ISOException.throwIt((short) 0x6301); //  ISO7816.SW_PIN_VERIFICATION_REQUIRED
                }

                len = apdu.setIncomingAndReceive();
                if (len > (short)80 || len % (short)16 != (short)0) { // 
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }

                short enc_data_len = (short) (len - (short)16); // 16B for MAC
                mac.init(mac_key, Signature.MODE_VERIFY);

                if (!mac.verify(buf, ISO7816.OFFSET_CDATA, enc_data_len, buf, (short) (ISO7816.OFFSET_CDATA + enc_data_len), (short) 16)) {
                    ISOException.throwIt(ISO7816.SW_WRONG_DATA);
                }

                enc.init(enc_key, Cipher.MODE_DECRYPT);
                try {
                    short dec_len = enc.doFinal(buf, ISO7816.OFFSET_CDATA, enc_data_len, user_data, (short) 0);
                    apdu.setOutgoing();
                    apdu.setOutgoingLength(dec_len);
                    apdu.sendBytesLong(user_data, (short) 0, dec_len);
                } catch (CryptoException e) {
                    ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                }
                break;


            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }
        
    }
}
