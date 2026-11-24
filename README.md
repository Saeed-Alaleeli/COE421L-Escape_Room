---
Saeed Alaleeli: b00088679
Yousef Zein: b00104553
Dalia Zein: g00104551
Saif Alzarouni: b00091430
---
# COE421L-Escape_Room

# Project Description
Wireless Escape Room is a 6-level puzzle game built for COE 421L. Players solve physical challenges on a custom control panel built around an Arduino Mega: copying a light sequence on the buttons, tilting a platform, holding a hand at the right distance from an ultrasonic sensor, and covering a light sensor. Each solved level reveals one digit of a secret code. Players enter the code on the keypad and hit the big red button to escape before the 5-minute timer runs out.

The panel talks wirelessly to a Java desktop app over XBee radios. The app runs the game logic, timer and display, and uses the State and Observer design patterns with multithreading. It also includes a simulation mode, so the game can be demoed without the hardware.
# Device Photos
![Escape Room Game](Escape%20room%20game.jpg)
# Goal of the Project
The goal of this project was to design and build an interactive escape room game that combines physical hardware with a desktop application over a wireless link. The game uses an Arduino Mega with sensors, buttons, a keypad and an LCD, and talks to a Java application through XBee radios. Along the way, the project put into practice embedded programming, sensor interfacing, serial wireless communication, and object-oriented software design using the State and Observer patterns with multithreading.
# Wiring Diagram
here
# Video of the Project
https://youtu.be/YAmdNJeOvOo?si=M6fT9P_mE32V87FF
