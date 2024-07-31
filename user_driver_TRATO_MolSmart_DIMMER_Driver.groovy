/**
 *  Hubitat - TCP MolSmart Dimmer Drivers by TRATO - BETA OK
 *
 *  Copyright 2024 VH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *        
 *       1.0 25/4/2024  - V.BETA 1
 *       1.1 13/6/2024  - Added User Guide
 *       1.2 18/6/2024  - Fixed index and length calc of network id. 
 *       1.3 31/07/2024 - Improved connection methods. Added feedback for Level Event not showing in Childs. Added Master on/off. Corrected Dimmer 4 level feedback. 
 *       1.4 31/07/2024 - Changed Feedback method. 
 */
metadata {
  definition (name: "MolSmart DIMMER Driver TCP v3 - by TRATO", namespace: "TRATO", author: "TRATO", vid: "generic-contact") {
        capability "Switch"  
        capability "Configuration"
        capability "Initialize"
        capability "Refresh"
		capability "SwitchLevel"
		capability "ChangeLevel"      
      
  }
    
  }

import groovy.json.JsonSlurper
import groovy.transform.Field
command "buscainputcount"
command "createchilds"
command "connectionCheck"
command "ManualKeepAlive"
command "verstatus"

    import groovy.transform.Field
    @Field static final String DRIVER = "by TRATO"
    @Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_MolSmart_Dimmer"


    String fmtHelpInfo(String str) {
    String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
    }


  preferences {
        input "device_IP_address", "text", title: "IP Address of MolSmart Dimmer"
        input "device_port", "number", title: "IP Port of Device", required: true, defaultValue: 502
        input name: "outputs", type: "string", title: "How many Relays " , defaultValue: 6      
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        //input name: "powerstatus", type: "string", title: "Power Status" 

    input 'logInfo', 'bool', title: 'Show Info Logs?',  required: false, defaultValue: true
    input 'logWarn', 'bool', title: 'Show Warning Logs?', required: false, defaultValue: true
    input 'logDebug', 'bool', title: 'Show Debug Logs?', description: 'Only leave on when required', required: false, defaultValue: true
    input 'logTrace', 'bool', title: 'Show Detailed Logs?', description: 'Only leave on when required', required: false, defaultValue: true

        //help guide
        input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver") 	  

    //attribute "powerstatus", "string"
    attribute "boardstatus", "string"      
    
      
  }   


@Field static String partialMessage = ''
@Field static Integer checkInterval = 600


def installed() {
    logTrace('installed()')
    state.childscreated = 0
    boardstatus = "offline"
    runIn(1800, logsOff)
} //OK

def uninstalled() {
    logTrace('uninstalled()')
    unschedule()
    interfaces.rawSocket.close()
} //OK

def updated() {
    logTrace('updated()')
    refresh()
}


def ManualKeepAlive (){
    logTrace('ManualKeepAlive()')
    interfaces.rawSocket.close();
    interfaces.rawSocket.close();
    unschedule()
    //state.clear()
    
    //Llama la busca de count de inputs+outputs
    buscainputcount()
    
    try {
        logTrace("ManualKeepAlive: Tentando conexão com o device no ${device_IP_address}...na porta ${device_port}");
        interfaces.rawSocket.connect(device_IP_address, (int) device_port);
        state.lastMessageReceivedAt = now();        
        runIn(checkInterval, "connectionCheck");
        if (boardstatus != "online") { 
            sendEvent(name: "boardstatus", value: "online", isStateChange: true)    
            boardstatus = "online"
        }
        refresh();  // se estava offline, preciso fazer um refresh
        
    }
    catch (e) {
        logError( "ManualKeepAlive: ${device_IP_address}  error: ${e.message}" )
        if (boardstatus != "offline") { 
            boardstatus = "offline"
            sendEvent(name: "boardstatus", value: "offline", isStateChange: true)
        }
        runIn(60, "initialize");
    }    
}


def initialize() {
    unschedule()
    logTrace('Run Initialize()')
    interfaces.rawSocket.close();
    interfaces.rawSocket.close();
    if (!device_IP_address) {
        logError 'IP do Device not configured'
        return
    }

    if (!device_port) {
        logError 'Porta do Device não configurada.'
        return
    }
    
    //Llama la busca de count de inputs+outputs via HTTP
    buscainputcount()
    
    try {
        logTrace("Initialize: Tentando conexão com o device no ${device_IP_address}...na porta configurada: ${device_port}");
        interfaces.rawSocket.connect(device_IP_address, (int) device_port);
        state.lastMessageReceivedAt = now();        
        if (boardstatus != "online") { 
            sendEvent(name: "boardstatus", value: "online", isStateChange: true)    
            boardstatus = "online"
        }
        boardstatus = "online"
        runIn(checkInterval, "connectionCheck");
        
    }
    catch (e) {
        logError( "Initialize: com ${device_IP_address} com um error: ${e.message}" )
        boardstatus = "offline"
        runIn(60, "initialize");
    }
    
    try{
         
          logTrace("Criando childs")
          createchilds()       
        
    }
    catch (e) {
        logError( "Error de Initialize: ${e.message}" )
    }
    runIn(10, "refresh");
}


def createchilds() {

    String thisId = device.id
	log.info "info thisid " + thisId
	
    def cd = getChildDevice("${thisId}-Switch")
    state.netids = "${thisId}-Switch-"
    
	if (state.childscreated == 0) {
    
	if (!cd) {
        log.info "inputcount = " + state.inputcount 
        for(int i = 1; i<=state.inputcount; i++) {        
        cd = addChildDevice("hubitat", "Generic Component Dimmer", "${thisId}-Switch-" + Integer.toString(i), [name: "${device.displayName} Switch-" + Integer.toString(i) , isComponent: true])
        log.info "added dimmer # " + i + " from " + state.inputcount            
        
    }
    }
      state.childscreated = 1   
    }
    else {
        log.info "Childs Dimmers já foram criados"
    }
  
}


def buscainputcount(){                              
            state.inputcount = 6  // deixar o numero de relays na memoria
            def ipmolsmart = settings.device_IP_address
            state.ipaddress = settings.device_IP_address    
}


def refresh() {
    logInfo('Refresh()')
    def msg = "REFRESH"    
    sendCommand(msg)
}


def verstatus() {
    
                 for(int i = 1; i<=state.inputcount; i++) {   
                 numerorelay = Integer.toString(i)
                 chdid = state.netids + numerorelay   
                 def cd = getChildDevice(chdid)
                 def switchstatus = cd.currentValue("level")
                 log.info "switchstatus  " + numerorelay + " = " + switchstatus                    
                }    
}


//Feedback e o tratamento 

 def fetchChild(String type, String name){
    String thisId = device.id
    def cd = getChildDevice("${thisId}-${type}_${name}")
    if (!cd) {
        cd = addChildDevice("hubitat", "Generic Component ${type}", "${thisId}-${type}_${name}", [name: "${name}", isComponent: true])
        cd.parse([[name:"switch", value:"off", descriptionText:"set initial switch value"]]) //TEST!!
    }
    return cd 
}


def parse(msg) {
    state.lastMessageReceived = new Date(now()).toString();
    state.lastMessageReceivedAt = now();

    def newmsg = hubitat.helper.HexUtils.hexStringToByteArray(msg) //na Mol, o resultado vem em HEX, então preciso converter para Array
    def newmsg2 = new String(newmsg) // Array para String  
    state.lastmessage = newmsg2

    if (newmsg2.contains("6")) {
        state.channels = 6
        log.debug "Placa Dimmer de 6 - IP - " + state.ipaddress
    }      

//NEW DEV 31.07.2024 vh - code com api
    
    log.info "****** New Block LOG Parse ********"
    log.info "Last Msg: " + newmsg2
    log.debug "Qde chars = " + newmsg2.length()   
    
    outputs_changed = newmsg2[28..33]  
    outputs_status = newmsg2[0..17]
    log.info "outputs_status: " + outputs_status
    log.info "outputs_changed: " + outputs_changed
    //log.info "state.updatemanual = " + state.updatemanual

    
     if ( (outputs_changed.contains("1"))  ) {   
         //&& (state.updatemanual == 0)
         relaychanged = outputs_changed.indexOf('1'); 
         log.debug ("Yes - change in Dimmer Status")
         log.debug "outputs_changed dimmer # " + relaychanged   
    
         numerorelay = Integer.toString(relaychanged+1)   
         chdid = state.netids + numerorelay               
         def cd = getChildDevice(chdid)   
         statusrelay = outputs_status.getAt(relaychanged)
      
             dim1 = newmsg2[0..2]
             dim2 = newmsg2[3..5]
             dim3 = newmsg2[6..8]
             dim4 = newmsg2[9..11]
             dim5 = newmsg2[12..14]
             dim6 = newmsg2[15..17]   
             
 
         switch(relaychanged) { 
                case 0:
                    logDebug ("Changes in Dimmer#" + 1) ;
                    valorsetlevel = dim1
                    break 
                case 1:
                    logDebug ("Changes in Dimmer#" + 2) ;
                    valorsetlevel = dim2             
                    break 
                case 2:
                    logDebug ("Changes in Dimmer#" + 3) ;
                    valorsetlevel = dim3                          
                    break 
                case 3:
                    logDebug ("Changes in Dimmer#" + 4) ;
                    valorsetlevel = dim4                          
                    break 
                case 4:
                    logDebug ("Changes in Dimmer#" + 5) ;
                    valorsetlevel = dim5                          
                    break              
                case 5:
                    logDebug ("Changes in Dimmer#" + 6) ;
                    valorsetlevel = dim6             
                    break              
                default:
                    log.info "nada";
                    break
         }
                 logDebug ( "valor do setlevel para enviar = " + valorsetlevel + " en el netid " + cd + " Dimmer(REAL)# " + relaychanged)
                 valorsetlevel = valorsetlevel as Integer  
                 if (valorsetlevel > 0) {
		                 getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on via ParseSetLevel > 0", isStateChange: true]])    
	                 } else {
	             getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off via ParseSetLevel < 0", isStateChange: true]])    
	                 } 
                 getChildDevice(cd.deviceNetworkId).parse([[name:"level", value: valorsetlevel, descriptionText:"${cd.displayName} was dimmered NEW VIA Parse"]])    
         
     }   else {
                     logDebug ( "no entrou, sem cambios ")
                     state.updatemanual = 0
         }
}



////////////////
////Commands 
////////////////

def on()
{
    logDebug("Master Power ON()")
    masteron()
}

def off()
{
    logDebug("Master Power OFF()")
    masteroff()
}


def masteron()
{
        log.info "MasterON() Executed"  
        for(int i = 1; i<=state.inputcount; i++) {        
                 numerorelay = Integer.toString(i)
                 chdid = state.netids + numerorelay               
                 def cd = getChildDevice(chdid)
                 getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on"]])    
                 logDebug ( "Dimmer " + cd + " turned ON")
                 on(cd)  
                 pauseExecution(300)     
       
        }
}

def masteroff()
{
        log.info "MasterOFF() Executed"
        for(int i = 1; i<=state.inputcount; i++) {        
                 numerorelay = Integer.toString(i)
                 chdid = state.netids + numerorelay               
                 def cd = getChildDevice(chdid)
                 getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off"]])    
                 logDebug ("Dimmer " + cd + " turned OFF")
                 off(cd)  
                 pauseExecution(300)     
       
        }
}



private sendCommand(s) {
    logDebug("sendCommand ${s}")
    interfaces.rawSocket.sendMessage(s)   
    logDebug ( "Sent Command to Board = " + s)
}




////////////////////////////
//// Connections Checks ////
////////////////////////////

def connectionCheck() {
    def now = now();
    
    if ( now - state.lastMessageReceivedAt > (checkInterval * 1000)) { 
        logError("ConnectionCheck:Sem mensagens desde ${(now - state.lastMessageReceivedAt)/60000} minutos, vamos tentar reconectar ...");
        if (boardstatus != "offline") { 
            sendEvent(name: "boardstatus", value: "offline", isStateChange: true)    
            boardstatus = "offline"
        }
        runIn(30, "connectionCheck");
        initialize();
    }
    else {
        logInfo("Connection Check: Status OK - Board Online");
        if (boardstatus != "online") { 
            sendEvent(name: "boardstatus", value: "online", isStateChange: true)    
            boardstatus = "online"
        }
        runIn(checkInterval, "connectionCheck");
    }
}


def socketStatus(String message) {
    if (message == "receive error: String index out of range: -1") {
        // This is some error condition that repeats every 15ms.

        interfaces.rawSocket.close();       
        logError( "socketStatus: ${message}");
        logError( "Closing connection to device" );
    }
    else if (message != "receive error: Read timed out") {
        logError( "socketStatus: ${message}")
    }
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Component Child
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

void componentRefresh(cd){
	if (logEnable) log.info "received refresh request from ${cd.displayName}"
	refresh()  
    
}

def componentOn(cd){
	if (logEnable) log.info "received on request from ${cd.displayName}"
    getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on via ComponentOn "]])       
    on(cd)  
    pauseExecution(200)
    state.updatemanual = 1
    
}

void componentOff(cd){
	if (logEnable) log.info "received off request from ${cd.displayName}"
    getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off via ComponentOFF "]])    
	off(cd)
    pauseExecution(200)
    state.updatemanual = 1

}

void componentSetLevel(cd,level) {
    if (logEnable) log.info "received set level dimmer from ${cd.displayName}"
    def valueaux = level as Integer
	def level2 = Math.max(Math.min(valueaux, 99), 0)
	if (level2 > 0) {
		getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on via ComponentSetLevel > 0", isStateChange: true]])    
	} else {
		getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off via ComponentSetLevel < 0", isStateChange: true]])    
	}
    SetLevel(cd,level)   
    getChildDevice(cd.deviceNetworkId).parse([[name:"level", value: level2, descriptionText:"${cd.displayName} was dimmered via Interface"]]) 
}


////// Driver Commands /////////

void SetLevel(cd,level) {

    def valueaux = level as Integer
	def level2 = Math.max(Math.min(valueaux, 99), 0)	
    
	ipdomodulo  = state.ipaddress
    lengthvar =  (cd.deviceNetworkId.length())
    int relay = 0
    
// Start verify of length 
      def substr1 = cd.deviceNetworkId.indexOf("-", cd.deviceNetworkId.indexOf("-") + 1);
      def result01 = lengthvar - substr1 
      if (result01 > 2  ) {
           def  substr2a = substr1 + 1
           def  substr2b = substr1 + 2
           def substr3 = cd.deviceNetworkId[substr2a..substr2b]
           numervalue1 = substr3

          
      }
      else {
          def substr3 = cd.deviceNetworkId[substr1+1]
          numervalue1 = substr3
        
           }

    def valor = ""
    valor = numervalue1 as Integer
    relay = valor   
    
     def stringrelay = relay
     def comando = "1" + stringrelay + "%" + level2
     interfaces.rawSocket.sendMessage(comando)   

    logDebug ( "Foi Alterado o Dimmer " + relay + " via TCP " + comando )
    state.update = 1  //variable to control update with board on parse  
    


}//SETLEVEL function



//SEND ON COMMAND IN CHILD BUTTON
void on(cd) {
if (logEnable) log.debug "Turn device ON "	
sendEvent(name: "switch", value: "on", isStateChange: true)

ipdomodulo  = state.ipaddress
lengthvar =  (cd.deviceNetworkId.length())
int relay = 0

// Start verify of length     
      def substr1 = cd.deviceNetworkId.indexOf("-", cd.deviceNetworkId.indexOf("-") + 1);
      def result01 = lengthvar - substr1 
      if (result01 > 2  ) {
           def  substr2a = substr1 + 1
           def  substr2b = substr1 + 2
           def substr3 = cd.deviceNetworkId[substr2a..substr2b]
           numervalue1 = substr3
          
      }
      else {
          def substr3 = cd.deviceNetworkId[substr1+1]
          numervalue1 = substr3
           }

    def valor = ""
    valor =   numervalue1 as Integer
    relay = valor   

///
     def stringrelay = relay
     def comando = "1" + stringrelay
     interfaces.rawSocket.sendMessage(comando)
     logDebug ( "Foi Ligado o Relay " + relay + " via TCP " + comando )
     state.updatemanual = 1  //variable to control update with board on parse

    
}


//SEND OFF COMMAND IN CHILD BUTTON 
void off(cd) {
if (logEnable) log.debug "Turn device OFF"	
sendEvent(name: "switch", value: "off", isStateChange: true)
    
ipdomodulo  = state.ipaddress
lengthvar =  (cd.deviceNetworkId.length())
int relay = 0

//Start verify length
      def substr1 = cd.deviceNetworkId.indexOf("-", cd.deviceNetworkId.indexOf("-") + 1);
      def result01 = lengthvar - substr1 
      if (result01 > 2  ) {
           def  substr2a = substr1 + 1
           def  substr2b = substr1 + 2
           def substr3 = cd.deviceNetworkId[substr2a..substr2b]
           numervalue1 = substr3
          
      }
      else {
          def substr3 = cd.deviceNetworkId[substr1+1]
          numervalue1 = substr3
           }

    def valor = ""
    valor =   numervalue1 as Integer
    relay = valor   

///
     def stringrelay = relay
     def comando = "2" + stringrelay
     interfaces.rawSocket.sendMessage(comando)
     logDebug ("Foi Desligado o Relay " + relay + " via TCP " + comando )
     state.updatemanual = 1  //variable to control update with board on parse
    
}



////////////////////////////////////////////////
////////LOGGING
///////////////////////////////////////////////


private processEvent( Variable, Value ) {
    if ( state."${ Variable }" != Value ) {
        state."${ Variable }" = Value
        logDebug( "Event: ${ Variable } = ${ Value }" )
        sendEvent( name: "${ Variable }", value: Value, isStateChanged: true )
    }
}



def logsOff() {
    log.warn 'logging disabled...'
    device.updateSetting('logInfo', [value:'false', type:'bool'])
    device.updateSetting('logWarn', [value:'false', type:'bool'])
    device.updateSetting('logDebug', [value:'false', type:'bool'])
    device.updateSetting('logTrace', [value:'false', type:'bool'])
}

void logDebug(String msg) {
    if ((Boolean)settings.logDebug != false) {
        log.debug "${drvThis}: ${msg}"
    }
}

void logInfo(String msg) {
    if ((Boolean)settings.logInfo != false) {
        log.info "${drvThis}: ${msg}"
    }
}

void logTrace(String msg) {
    if ((Boolean)settings.logTrace != false) {
        log.trace "${drvThis}: ${msg}"
    }
}

void logWarn(String msg, boolean force = false) {
    if (force || (Boolean)settings.logWarn != false) {
        log.warn "${drvThis}: ${msg}"
    }
}

void logError(String msg) {
    log.error "${drvThis}: ${msg}"
}
