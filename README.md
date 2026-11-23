---
Saeed Alaleeli: b00088679
Yousef Zein: b00104553
Dalia Zein: g00104551
Saif Alzarouni: b00091430
---
# COE421L-Escape_Room

# Project Description
here------------
# Device Photos?
here----------??
# Goal of the Project
here------------
# Important Code
// =====================================================================
//  WIRELESS ESCAPE ROOM - GAME CODE (Arduino Mega) - COE 421L Group 4
// =====================================================================
//  The Java app (laptop) is the game master. It talks to this board over
//  the XBee (Serial1). You can also type the same commands in the Arduino
//  Serial Monitor (9600 baud, "Newline") to test without the Java app.
//
//  COMMANDS IN (from Java / Serial Monitor):
//    START      new game: makes a new secret code, waits for LEVEL 1
//    LEVEL n    start level n (1..6)
//    WIN        show "you escaped" celebration
//    LOSE       show "game over" (time ran out)
//    RESET      back to idle
//    DEMO       test mode: plays all levels in a row without the Java app
//    PING       replies PONG
//
//  MESSAGES OUT (to Java):
//    READY            board started
//    LEVEL n          level n started
//    SOLVED n d       levels 1-4 solved, d = revealed code digit
//    SOLVED 5         correct code entered
//    SOLVED 6         big red button pressed
//    WRONG n          wrong attempt in level n
//    SENSOR n value   live value for levels 2-4 (tilt / distance / light)
//    PONG
//
//  LEVELS
//    1  Copy the lights : 3 buttons light up in a pattern, repeat it
//    2  Tilt            : tilt the platform until it registers
//    3  Distance        : hold your hand 5-25 cm from the sensor
//    4  Darkness        : cover the light sensor
//    5  Keypad code     : type the 4 collected digits, A = enter, B = clear, C = delete
//    6  Escape          : press the big red button
// =====================================================================

#include <Wire.h>

// ------------------------- SETTINGS (tune here) -------------------------
const int  SIMON_LENGTH   = 4;      // how many steps in the light pattern
const int  TILT_DELTA     = 45;     // how far the accelerometer must move
const int  DIST_MIN       = 5;      // cm
const int  DIST_MAX       = 25;     // cm
const int  LIGHT_DELTA    = 2;      // how much the light reading must change
const unsigned long HOLD_MS = 1500; // how long a sensor must be held (ms)

// ------------------------------ PINS ------------------------------------
const byte ROWS[4] = {32, 34, 36, 24};   // keypad (matches your wiring)
const byte COLS[4] = {22, 26, 28, 30};
const char KEYS[4][4] = {
  {'1', '2', '3', 'A'},
  {'4', '5', '6', 'B'},
  {'7', '8', '9', 'C'},
  {'*', '0', '#', 'D'}
};
const byte LEDS[3]      = {5, 6, 7};        // small progress LEDs (removed, harmless)
const byte BTN[3]       = {23, 25, 27};     // green, yellow, red (switch NO)
const byte BTN_LIGHT[3] = {38, 39, 40};     // green, yellow, red lights
const byte BIG_BTN = 2, BIG_LIGHT = 41;
const byte BUZZ = 8, TRIG = 10, ECHO = 11;
const byte LIGHT_PIN = A0, ACC_X = A1, ACC_Y = A2;

// ------------------------- tiny LCD driver ------------------------------
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
    w4(0x30); delayMicroseconds(4500); w4(0x30); delayMicroseconds(4500);
    w4(0x30); delayMicroseconds(150);  w4(0x20);
    cmd(0x28); cmd(0x0C); cmd(0x01); delay(2); cmd(0x06);
  }
  void line(uint8_t row, String s) {
    while (s.length() < 16) s += ' ';
    cmd(0x80 | (row ? 0x40 : 0));
    for (int i = 0; i < 16; i++) send(s[i], 1);
  }
};
LCD lcd;
bool lcdOk = false;
void show(String top, String bottom) { if (lcdOk) { lcd.line(0, top); lcd.line(1, bottom); } }
void showBottom(String bottom) { if (lcdOk) lcd.line(1, bottom); }

// ------------------------------ STATE -----------------------------------
int  level = 0;              // 0 = idle / waiting
bool demoMode = false;       // auto-advance through levels (testing)
char code[5] = "0000";       // secret code
String entered = "";         // keypad input in level 5
String inUsb = "", inXbee = "";

// level 1 (copy the lights)
byte seq[8];
int  seqPos = 0;
bool needShow = true;
bool lastBtn[3] = {false, false, false};

// levels 2-4 (sensors)
int  baseline = 0;
unsigned long holdStart = 0;
unsigned long lastSensorMsg = 0;

char lastKey = 0;
bool lastBig = false;

// ----------------------------- HELPERS ----------------------------------
void sendMsg(String m) { Serial.println(m); Serial1.println(m); }

void beep(int ms) { digitalWrite(BUZZ, HIGH); delay(ms); digitalWrite(BUZZ, LOW); }
void goodBeep()  { beep(150); delay(80); beep(150); }
void badBeep()   { beep(900); }

void allLights(bool on) {
  for (int i = 0; i < 3; i++) { digitalWrite(BTN_LIGHT[i], on); digitalWrite(LEDS[i], on); }
  digitalWrite(BIG_LIGHT, on);
}

void progressLeds(int solved) {
  for (int i = 0; i < 3; i++) digitalWrite(LEDS[i], i < solved);
}

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

char newKeyPress() {                     // returns a key only once per press
  char k = readKeypad();
  char result = (k && k != lastKey) ? k : 0;
  lastKey = k;
  if (result) delay(20);
  return result;
}

long readDistanceCm() {
  digitalWrite(TRIG, LOW); delayMicroseconds(2);
  digitalWrite(TRIG, HIGH); delayMicroseconds(10);
  digitalWrite(TRIG, LOW);
  long d = pulseIn(ECHO, HIGH, 30000);
  return d == 0 ? -1 : d / 58;
}

int avgRead(byte pin) { long s = 0; for (int i = 0; i < 8; i++) { s += analogRead(pin); delay(2); } return s / 8; }

int baselineY = 0;
int tiltAmount() {                     // biggest change on X or Y since level start
  int dx = abs(avgRead(ACC_X) - baseline);
  int dy = abs(avgRead(ACC_Y) - baselineY);
  return max(dx, dy);
}

// --------------------------- LEVEL CONTROL ------------------------------
void startLevel(int n) {
  level = n;
  holdStart = 0;
  allLights(false);
  progressLeds(n >= 2 ? n - 1 : 0);
  sendMsg("LEVEL " + String(n));
  switch (n) {
    case 1:
      for (int i = 0; i < SIMON_LENGTH; i++) seq[i] = random(3);
      seqPos = 0; needShow = true;
      show("L1 COPY LIGHTS", "Watch...");
      delay(800);
      break;
    case 2:
      baseline = avgRead(ACC_X);
      baselineY = avgRead(ACC_Y);
      show("L2 TILT PLATFORM", "Tilt the disk!");
      break;
    case 3:
      show("L3 DISTANCE", "Hand 5-25cm");
      break;
    case 4:
      baseline = avgRead(LIGHT_PIN);
      show("L4 DARKNESS", "Block the light");
      break;
    case 5:
      entered = "";
      lastKey = readKeypad();
      show("L5 ENTER CODE", "Code: ____");
      progressLeds(3);
      break;
    case 6:
      lastBig = digitalRead(BIG_BTN) == LOW;
      show("L6 ESCAPE!", "Press BIG RED!");
      progressLeds(3);
      break;
  }
}

void levelSolved() {
  int n = level;
  level = 0;                                   // wait for the next LEVEL command
  goodBeep();
  if (n <= 4) {
    char d = code[n - 1];
    sendMsg("SOLVED " + String(n) + " " + d);
    show("Level " + String(n) + " SOLVED!", "Code digit: " + String(d));
  } else {
    sendMsg("SOLVED " + String(n));
    if (n == 5) show("CODE CORRECT!", "One more step...");
    else        show("ESCAPING...", "");
  }
  if (demoMode) {                              // testing without Java
    delay(2000);
    if (n < 6) startLevel(n + 1); else celebrate();
  }
}

void celebrate() {
  level = 0;
  show("*** YOU ***", "*** ESCAPED! ***");
  for (int i = 0; i < 6; i++) { allLights(true); beep(150); delay(80); allLights(false); delay(120); }
  allLights(true);
  demoMode = false;
}

void gameOver() {
  level = 0;
  allLights(false);
  show("TIME'S UP!", "GAME OVER");
  beep(2000);
  demoMode = false;
}

void newGame() {
  const char SAFE[] = "235689";                // never uses 1, 4, 7
  for (int i = 0; i < 4; i++) code[i] = SAFE[random(6)];
  code[4] = 0;
  level = 0;
  allLights(false);
  show("ESCAPE ROOM", "Get ready...");
  Serial.print(F("(debug) secret code = ")); Serial.println(code);
}

// ----------------------------- LEVELS -----------------------------------
void runLevel1() {                              // copy the lights
  if (needShow) {
    showBottom("Watch...");
    delay(500);
    for (int i = 0; i < SIMON_LENGTH; i++) {
      byte b = seq[i];
      digitalWrite(BTN_LIGHT[b], HIGH); digitalWrite(LEDS[b], HIGH); beep(120);
      delay(370);
      digitalWrite(BTN_LIGHT[b], LOW);  digitalWrite(LEDS[b], LOW);
      delay(250);
    }
    seqPos = 0; needShow = false;
    for (int i = 0; i < 3; i++) lastBtn[i] = digitalRead(BTN[i]) == LOW;
    showBottom("Your turn!");
  }
  for (int i = 0; i < 3; i++) {
    bool pressed = digitalRead(BTN[i]) == LOW;
    digitalWrite(BTN_LIGHT[i], pressed);
    digitalWrite(LEDS[i], pressed);
    if (pressed && !lastBtn[i]) {               // new press
      delay(30);
      if (i == seq[seqPos]) {
        beep(80);
        seqPos++;
        showBottom("Step " + String(seqPos) + "/" + String(SIMON_LENGTH));
        if (seqPos >= SIMON_LENGTH) { delay(300); allLights(false); levelSolved(); return; }
      } else {
        sendMsg("WRONG 1");
        allLights(false);
        showBottom("Wrong! Again...");
        badBeep();
        delay(700);
        needShow = true;
        return;
      }
    }
    lastBtn[i] = pressed;
  }
}

// shared "hold it for HOLD_MS" logic for levels 2-4
void holdCheck(bool ok, int value, int n) {
  if (millis() - lastSensorMsg > 400) { lastSensorMsg = millis(); sendMsg("SENSOR " + String(n) + " " + String(value)); }
  if (ok) {
    if (holdStart == 0) { holdStart = millis(); beep(60); }
    unsigned long held = millis() - holdStart;
    showBottom("Hold it... " + String((HOLD_MS - min(held, HOLD_MS)) / 100 / 10.0, 1) + "s");
    if (held >= HOLD_MS) levelSolved();
  } else {
    holdStart = 0;
  }
}

void runLevel2() {                              // tilt
  int amount = tiltAmount();
  if (holdStart == 0) showBottom("Tilt: " + String(amount) + "/" + String(TILT_DELTA));
  holdCheck(amount >= TILT_DELTA, amount, 2);
}

void runLevel3() {                              // distance
  long cm = readDistanceCm();
  if (holdStart == 0) {
    if (cm < 0) showBottom("Hand 5-25cm");
    else        showBottom("Distance: " + String(cm) + "cm");
  }
  holdCheck(cm >= DIST_MIN && cm <= DIST_MAX, (int)cm, 3);
  delay(40);
}

void runLevel4() {                              // darkness
  int change = abs(avgRead(LIGHT_PIN) - baseline);
  if (holdStart == 0) showBottom("Light: " + String(change) + "/" + String(LIGHT_DELTA));
  holdCheck(change >= LIGHT_DELTA, change, 4);
}

void runLevel5() {                              // keypad code
  char k = newKeyPress();
  if (!k) return;
  if (k >= '1' && k <= '9' && entered.length() < 4) {
    beep(80);
    entered += k;
  } else if (k == 'C') {                               // delete last digit
    beep(80);
    if (entered.length() > 0) entered.remove(entered.length() - 1);
  } else if (k == 'B' || k == '*' || k == 'D') {       // clear all
    beep(80);
    entered = "";
  } else if (k == 'A' || k == '#') {                   // enter
    if (entered == String(code)) { levelSolved(); return; }
    sendMsg("WRONG 5");
    showBottom("WRONG CODE!");
    badBeep();
    delay(800);
    entered = "";
  }
  String shown = entered; while (shown.length() < 4) shown += '_';
  showBottom("Code: " + shown + " A=OK");
}

void runLevel6() {                              // big red button
  digitalWrite(BIG_LIGHT, (millis() / 300) % 2);  // blink
  bool pressed = digitalRead(BIG_BTN) == LOW;
  if (pressed && !lastBig) { delay(30); digitalWrite(BIG_LIGHT, HIGH); levelSolved(); }
  lastBig = pressed;
}

// ---------------------------- COMMANDS ----------------------------------
void handleCommand(String c) {
  c.trim(); c.toUpperCase();
  if (c.length() == 0) return;
  if (c == "PING")        sendMsg("PONG");
  else if (c == "START")  { demoMode = false; newGame(); }
  else if (c == "DEMO")   { newGame(); demoMode = true; startLevel(1); }
  else if (c == "RESET")  { demoMode = false; level = 0; allLights(false); show("ESCAPE ROOM", "Waiting..."); }
  else if (c == "WIN")    celebrate();
  else if (c == "LOSE")   gameOver();
  else if (c.startsWith("LEVEL ")) {
    int n = c.substring(6).toInt();
    if (n >= 1 && n <= 6) startLevel(n);
  }
}

void readCommands() {
  while (Serial.available()) {
    char ch = Serial.read();
    if (ch == '\n' || ch == '\r') { handleCommand(inUsb); inUsb = ""; } else inUsb += ch;
  }
  while (Serial1.available()) {
    char ch = Serial1.read();
    if (ch == '\n' || ch == '\r') { handleCommand(inXbee); inXbee = ""; } else inXbee += ch;
  }
}

// ------------------------------ SETUP -----------------------------------
void setup() {
  Serial.begin(9600);
  Serial1.begin(9600);
  Wire.begin();
  for (uint8_t a = 0x20; a <= 0x3F; a++) {
    Wire.beginTransmission(a);
    if (Wire.endTransmission() == 0) { lcd.begin(a); lcdOk = true; break; }
  }
  for (int r = 0; r < 4; r++) { pinMode(ROWS[r], OUTPUT); digitalWrite(ROWS[r], HIGH); }
  for (int c = 0; c < 4; c++) pinMode(COLS[c], INPUT_PULLUP);
  for (int i = 0; i < 3; i++) {
    pinMode(LEDS[i], OUTPUT); pinMode(BTN[i], INPUT_PULLUP); pinMode(BTN_LIGHT[i], OUTPUT);
  }
  pinMode(BIG_BTN, INPUT_PULLUP); pinMode(BIG_LIGHT, OUTPUT);
  pinMode(BUZZ, OUTPUT); pinMode(TRIG, OUTPUT); pinMode(ECHO, INPUT);

  randomSeed(analogRead(A3) ^ (analogRead(A4) << 4) ^ micros());
  show("ESCAPE ROOM", "Waiting for PC");
  beep(200);
  sendMsg("READY");
}

// ------------------------------- LOOP -----------------------------------
void loop() {
  readCommands();
  switch (level) {
    case 1: runLevel1(); break;
    case 2: runLevel2(); break;
    case 3: runLevel3(); break;
    case 4: runLevel4(); break;
    case 5: runLevel5(); break;
    case 6: runLevel6(); break;
  }
}


# Video of the Project
(insert link here)
