/* ----------------------------------------------------------------------------- 
 * Pin layout:
 * Signal     Pin              Pin               Pin
 *            Arduino Uno      Arduino Mega      MFRC522 board
 * ------------------------------------------------------------
 * Reset      9                5                 RST
 * SPI SS     10               53                SDA
 * SPI MOSI   11               52                MOSI
 * SPI MISO   12               51                MISO
 * SPI SCK    13               50                SCK
 */

#include <SPI.h>
#include <MFRC522.h>

#define SS_PIN 10
#define RST_PIN 9
MFRC522 mfrc522(SS_PIN, RST_PIN);        // Create MFRC522 instance.

void setup() {
        Serial.begin(9600);        // Initialize serial communications with the PC
        SPI.begin();                // Init SPI bus
        mfrc522.PCD_Init();        // Init MFRC522 card
}

boolean readCardID() {
        if ( ! mfrc522.PICC_IsNewCardPresent()) {   
            return false;
        }

        if ( ! mfrc522.PICC_ReadCardSerial()) {
                return false;
        }

        for (byte i = 0; i < mfrc522.uid.size; i++) {
                Serial.print(mfrc522.uid.uidByte[i] < 0x10 ? "0" : "");
                Serial.print(mfrc522.uid.uidByte[i], HEX);
        } 

        Serial.println();
        Serial.flush();
        return true;
}

void loop() {
    if (Serial.available() > 0) {
        // Cakame na spravu ukoncenu znakom \n
        char buffer[10];
        int bytes = Serial.readBytesUntil('\n', buffer, 10);
        // Ak sa prijala sprava GC, odosleme cislo karty (ak karty niet, odosleme jeden znak X)
        if ((bytes == 2) && (buffer[0] == 'G') && (buffer[1] == 'C')) {
           if (!readCardID()) {
             delay(10);
             if (!readCardID()) {
               Serial.println("X");
               Serial.flush();
             }
           }
        }
    }   
}

