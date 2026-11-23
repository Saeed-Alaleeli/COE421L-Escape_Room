// ===== Escape Room - HARDWARE TEST (Arduino Mega) - COE 421L Group 4 =====
// Upload this, open Serial Monitor at 9600 baud, and test each part.
// No extra libraries needed (Wire is built into Arduino IDE).
//
// PIN MAP
//   Keypad holes 1-8 -> 22,24,...,36 (rows 32,34,36,24 / cols 22,26,28,30 in code)
//   LCD SDA 20, SCL 21
//   Buzzer S 8 | (old LilyPad LEDs 5,6,7: removed, still harmless in code)
//   HC-SR04 Trig 10, Echo 11
//   Buttons (NO): green 23, yellow 25, red 27, big red 2
//   Button lights: green 38, yellow 39, red 40, big red 41
//   Light sensor A0 | Accelerometer X A1, Y A2
//   XBee: Serial1 (TX1 18 -> divider -> XBee rx, XBee tx -> RX1 19)

#include <Wire.h>

// ---------- tiny built-in LCD driver (I2C backpack) ----------
class LCD {
  uint8_t addr = 0x27, bl = 0x08;
  void ew(uint8_t d) { Wire.beginTransmission(addr); Wire.write(d | bl); Wire.endTransmission(); }
  void pulse(uint8_t d) { ew(d | 0x04); delayMicroseconds(1); ew(d & ~0x04); delayMicroseconds(50); }
  void w4(uint8_t d) { ew(d); pulse(d); }
  void send(uint8_t v, uint8_t m) { w4((v & 0xF0) | m); w4(((v << 4) & 0xF0) | m); }
  void cmd(uint8_t c) { send(c, 0); }
public:
  void begin(uint8_t a) {
    addr = a; delay(50); ew(0); delay(100);
    w4(0x30); delayMicroseconds(4500);
    w4(0x30); delayMicroseconds(4500);
    w4(0x30); delayMicroseconds(150);
    w4(0x20);
    cmd(0x28); cmd(0x0C); cmd(0x01); delay(2); cmd(0x06);
  }
  void clear() { cmd(0x01); delay(2); }
  void line(uint8_t row, String s) {
    while (s.length() < 16) s += ' ';
    cmd(0x80 | (row ? 0x40 : 0));
    for (int i = 0; i < 16; i++) send(s[i], 1);
  }
};

LCD lcd;

const byte ROWS[4] = {32, 34, 36, 24};   // matches how our keypad is wired
const byte COLS[4] = {22, 26, 28, 30};
const char KEYS[4][4] = {
  {'1', '2', '3', 'A'},
  {'4', '5', '6', 'B'},
  {'7', '8', '9', 'C'},
  {'*', '0', '#', 'D'}
};
const byte LEDS[3] = {5, 6, 7};
const byte BTN[4] = {23, 25, 27, 2};          // green, yellow, red, big red
const byte BTN_LIGHT[4] = {38, 39, 40, 41};
const char* BTN_NAME[4] = {"GREEN", "YELLOW", "RED", "BIG RED"};
const byte BUZZ = 8, TRIG = 10, ECHO = 11;

char lastKey = 0;
bool lastBtn[4] = {false, false, false, false};
unsigned long lastPrint = 0;
bool lcdFound = false;

void beep(int ms) { digitalWrite(BUZZ, HIGH); delay(ms); digitalWrite(BUZZ, LOW); }

char readKeypad() {
  for (int r = 0; r < 4; r++) {
    digitalWrite(ROWS[r], LOW);
    for (int c = 0; c < 4; c++) {
      if (digitalRead(COLS[c]) == LOW) { digitalWrite(ROWS[r], HIGH); return KEYS[r][c]; }
    }
    digitalWrite(ROWS[r], HIGH);
  }
  return 0;
}

long readDistanceCm() {
  digitalWrite(TRIG, LOW); delayMicroseconds(2);
  digitalWrite(TRIG, HIGH); delayMicroseconds(10);
  digitalWrite(TRIG, LOW);
  long d = pulseIn(ECHO, HIGH, 30000);
  return d == 0 ? -1 : d / 58;
}

void setup() {
  Serial.begin(9600);
  Serial1.begin(9600);
  Wire.begin();

  // ---- find the LCD address ----
  Serial.println(F("\n=== HARDWARE TEST ==="));
  for (uint8_t a = 0x20; a <= 0x3F; a++) {
    Wire.beginTransmission(a);
    if (Wire.endTransmission() == 0) {
      Serial.print(F("LCD found at address 0x")); Serial.println(a, HEX);
      lcd.begin(a); lcdFound = true; break;
    }
  }
  if (!lcdFound) Serial.println(F("LCD NOT FOUND -> check SDA 20, SCL 21, VCC, GND"));
  else { lcd.line(0, "Hardware test"); lcd.line(1, "Press a key..."); }

  for (int r = 0; r < 4; r++) { pinMode(ROWS[r], OUTPUT); digitalWrite(ROWS[r], HIGH); }
  for (int c = 0; c < 4; c++) pinMode(COLS[c], INPUT_PULLUP);
  for (int i = 0; i < 3; i++) pinMode(LEDS[i], OUTPUT);
  for (int i = 0; i < 4; i++) { pinMode(BTN[i], INPUT_PULLUP); pinMode(BTN_LIGHT[i], OUTPUT); }
  pinMode(BUZZ, OUTPUT);
  pinMode(TRIG, OUTPUT);
  pinMode(ECHO, INPUT);


  // ---- startup light show: LEDs, then button lights, then a beep ----
  Serial.println(F("LEDs 1-2-3 should blink one by one..."));
  for (int i = 0; i < 3; i++) { digitalWrite(LEDS[i], HIGH); delay(400); digitalWrite(LEDS[i], LOW); }
  Serial.println(F("Button lights green-yellow-red-big should blink..."));
  for (int i = 0; i < 4; i++) { digitalWrite(BTN_LIGHT[i], HIGH); delay(400); digitalWrite(BTN_LIGHT[i], LOW); }
  Serial.println(F("Buzzer should beep once..."));
  beep(200);

  Serial.println(F("\nNOW: press keypad keys and buttons, cover the light sensor,"));
  Serial.println(F("tilt the accelerometer, wave a hand at the ultrasonic sensor.\n"));
  Serial1.println("HELLO FROM MEGA");
}

void loop() {
  // ---- keypad ----
  char k = readKeypad();
  if (k && k != lastKey) {
    Serial.print(F("KEY: ")); Serial.println(k);
    if (lcdFound) lcd.line(1, String("Key pressed: ") + k);
    Serial1.print("KEY "); Serial1.println(k);
    beep(40);
  }
  lastKey = k;

  // ---- buttons: pressing lights up that button + matching LED ----
  for (int i = 0; i < 4; i++) {
    bool pressed = digitalRead(BTN[i]) == LOW;
    digitalWrite(BTN_LIGHT[i], pressed);
    if (i < 3) digitalWrite(LEDS[i], pressed);
    if (pressed != lastBtn[i]) {
      Serial.print(BTN_NAME[i]); Serial.println(pressed ? F(" button PRESSED") : F(" button released"));
      if (pressed) {
        if (lcdFound) lcd.line(1, String(BTN_NAME[i]) + " pressed");
        Serial1.print("BTN "); Serial1.println(BTN_NAME[i]);
        beep(40);
      }
      lastBtn[i] = pressed;
    }
  }

  // ---- XBee: show anything the laptop sends ----
  while (Serial1.available()) {
    String msg = Serial1.readStringUntil('\n');
    Serial.print(F("XBEE RECEIVED: ")); Serial.println(msg);
    if (lcdFound) lcd.line(0, "XBee: " + msg);
  }

  // ---- sensors, printed twice a second ----
  if (millis() - lastPrint > 500) {
    lastPrint = millis();
    Serial.print(F("LIGHT A0=")); Serial.print(analogRead(A0));
    Serial.print(F(" | ACCEL X=")); Serial.print(analogRead(A1));
    Serial.print(F(" Y=")); Serial.print(analogRead(A2));
    long cm = readDistanceCm();
    Serial.print(F(" | DIST=")); if (cm < 0) Serial.println(F("--")); else { Serial.print(cm); Serial.println(F(" cm")); }
  }
}
