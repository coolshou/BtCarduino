//======================================
//    board: ATmega328p-pu
//    2016/11/27
//======================================
#include <Thread.h>
#include <ThreadController.h>
#include <SoftwareSerial.h>                                                                                                                                                               

#define rxPin 10     //digital pin10　接BT TXD                                                                                 
#define txPin 9     //digital pin9　接BT RXD through voltage divider 1K Ohm + 2K Ohm (GND)
SoftwareSerial btSerial(rxPin, txPin); 

//—uart ascII 　命令—————————————-
#define _CMDEND 0x3B // ; :end of command
#define _CMDSEP 0x3A // : :split of command
/*
#define _US 0x5553 //ACII　命令值(2byte)
#define _DS 0x4453
#define _LS 0x4C53
#define _RS 0x5253
#define _UL 0x554C
#define _DL 0x444C
#define _UR 0x5552
#define _DR 0x4452
#define _SS 0x5353
*/
#define LEFT_ENA 5  //digital pin5
#define LEFT_In1 7  //digital pin7
#define LEFT_In2 8  //digital pin8
#define RIGHT_In3 2 //digital pin2
#define RIGHT_In4 4 //digital pin4
#define RIGHT_ENB 6 //digital pin6

#define BASESPEED 50 //basic speed

#define LOWSPEED 80 //前後左右的速度，可以調整為其他值
#define HIGHSPEED 180 //轉彎的速度，可以調整為其他值
#define MOTORSTOP 0 //Stop motor

const int DELAY = 1000;

uint16_t value;
//uint8_t r_buffer[4];
char r_buffer[13];

uint8_t number;
uint8_t command;

String cmdFB="";
String cmdRL="";
//char* cmdFB;
//char* cmdRL;
int valFB=0;
int valRL=0;
int valStr=0;

//
// ThreadController that will controll all threads
ThreadController controll = ThreadController();

//bt Thread (as a pointer)
Thread* btCmdThread = new Thread();

// callback for btThread
void btCmdCallback(){
    if(btSerial.available())
    {
      String cmd  = btSerial.readStringUntil(_CMDEND);
      Serial.println(cmd);
      command = ParseSerialCMD(cmd);
    }
          
    if(command)  //　判斷命令
    {
       command=0;
       freeDrive(cmdFB, valFB, cmdRL, valRL, valStr);
    }//end  if(command)
}

void setup(){
    // define pin modes for tx, rx pins:                                                                  
    pinMode(rxPin, INPUT);                                                                                
    pinMode(txPin, OUTPUT); 
    btSerial.begin(9600);  //BT, HC-05 default 9600  
  
    pinMode(LEFT_ENA,OUTPUT);
    pinMode(RIGHT_ENB,OUTPUT);
    pinMode(LEFT_In1,OUTPUT);
    pinMode(LEFT_In2,OUTPUT);
    pinMode(RIGHT_In3,OUTPUT);
    pinMode(RIGHT_In4,OUTPUT);

    stop();
    Serial.begin(38400);    //arduino serial 38400;

    // Configure btCmdThread
    btCmdThread->onRun(btCmdCallback);
    btCmdThread->setInterval(500);
    // Adds both threads to the controller
    controll.add(btCmdThread);
  
    Serial.println("btCar Start");
}
void loop(){
  // run ThreadController
  // this will check every thread inside ThreadController,
  // if it should run. If yes, he will run it;
  controll.run();

}

void freeDrive(String FB,int FBval, String RL, int RLval, int strength){
  if ((FBval==0)&(RLval==0)&(strength==0)) {
    Serial.println("stop");
    stop();
  } else {
    Serial.println("FB:" + FB + FBval);
    Serial.println(" RL:" + RL + RLval);
    Serial.println(" S:" + String(strength));
    int val1=FBval+RLval+strength;
    int val2=FBval+strength;
    if (FB=="F"){
    //Serial.println("v1:"+String(val1));
    //Serial.println("v2:"+String(val2));     
        digitalWrite(LEFT_In1,HIGH);
        digitalWrite(LEFT_In2,LOW);
        digitalWrite(RIGHT_In3,HIGH);
        digitalWrite(RIGHT_In4,LOW);
        if (RL=="R") {
          analogWrite(LEFT_ENA,val1);
          analogWrite(RIGHT_ENB,val2);
        } else if (RL=="L" ) {
          analogWrite(LEFT_ENA,val2);
          analogWrite(RIGHT_ENB,val1);       
        }
    } else if (FB=="B") {
        digitalWrite(LEFT_In1,LOW);
        digitalWrite(LEFT_In2,HIGH);
        digitalWrite(RIGHT_In3,LOW);
        digitalWrite(RIGHT_In4,HIGH);
        if (RL=="R") {
          analogWrite(LEFT_ENA,val1);
          analogWrite(RIGHT_ENB,val2);         
        } else if (RL=="L" ) {
          analogWrite(LEFT_ENA,val2);
          analogWrite(RIGHT_ENB,val1);               
        }
    } else {
      Serial.println("unknown FB");
    }
    //delay(DELAY);
  }
}
void stop() {
  digitalWrite(LEFT_In1,LOW);;
  digitalWrite(LEFT_In2,LOW);;
  digitalWrite(RIGHT_In3,LOW);;
  digitalWrite(RIGHT_In4,LOW);;
  analogWrite(LEFT_ENA,MOTORSTOP);
  analogWrite(RIGHT_ENB,MOTORSTOP); 
}


int ParseSerialCMD(String sCmd)
{
  int commaIndex = sCmd.indexOf(':');
  //  Search for the next comma just after the first
  int secondCommaIndex = sCmd.indexOf(':', commaIndex+1);

  String firstValue = sCmd.substring(0, commaIndex);
  String secondValue = sCmd.substring(commaIndex+1, secondCommaIndex);
  String thirdValue = sCmd.substring(secondCommaIndex+1); // To the end of the string
  //Serial.println("1:"+firstValue);
  //Serial.println("2:"+secondValue);
  //Serial.println("3:"+thirdValue);
  String val;
  int len;
  if (thirdValue != NULL) {
    command = 1;
    cmdFB = String(firstValue[0]);
    len = firstValue.length();
    val = firstValue.substring(1);
    valFB = val.toInt();
    
    cmdRL = String(secondValue[0]);
    len = secondValue.length();
    val = secondValue.substring(1);
    valRL = val.toInt();
    
    valStr = thirdValue.toInt();
  } else {
    Serial.println("thirdValue is null");
    command = 0;
  }
  return command;
}

