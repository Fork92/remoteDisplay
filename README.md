#EGot - Emulated Graphiccard over tcp

EGot ist eine Grafikkarten Emulation welche sich über ein TCP-Netz steuern lässt.

##Aufruf

Starten lässt sich EGoT wie folgt:
```Bash
$java -jar egot.jar
```
Optionale argumente sind:
```Bash
-c [card]
-h [hostip]
-p [hostport]
```
##Command list:

|Befehl| Argumente              | Description |
|------|------------------------|-------------|
|   WRITE|[startaddr] [val1 ...]  | Überschreibt den Speicher oder das Register an [startaddr]. Es können mehrere values gleichzeitig übergeben werden. [val] ist ein Hexwert und entspricht einem Byte|
|   READ|[addr]                  | Zeigt die werte der Adresse [addr] an |
|  help|{[Befehl]}              | Zeigt die Hilfe für alle oder dem mitgegebenen befehl an |

##Implementierte Karten
###MDA
Die MDA Karte kann nur Text anzeigen. Die Auflösung beträgt
80x25 Characters, wobei jedes Character in 9x14px auflöst, dies macht eine Gesamt Auflösung von 720x280px.

Ein Character nutzt jeweils 2 Byte im Speicher, somit stehen 80x25x2(4K) Byte an speicher zur verfügung.
Das erste Byte repräsentiert jeweils den Character, das zweite Byte die Attribute.

|         |                                |
|---------|--------------------------------|
|width    | 80 Characters                  |
|height   | 25 Characters                  |
|modes    | only textmode                  |
|colors   | green on black                 |
|registers| Moderegister on address: 0x03B8|

####Character Statuscodes
|Flag | Description                       |
|-----|-----------------------------------|
|Bit 1| Underline                         |
|Bit 3| Highsensity - Brightgreen on Black|
|Bit 7| Blink                             |

Exception:

|Flag|Description                                    |
|----|-----------------------------------------------|
|0x00|display as Blackspace                          |
|0x08|display as Blackspace                          |
|0x80|display as Blackspace                          |
|0x88|display as Blackspace                          |
|0x70|display as Black on Green                      |
|0x78|display as Darkgreen on Green                  |
|0xF0|Blinking version of 0x70, is blinking activated|
|0xF8|Blinking version of 0x78, is blinking activated|

####Moderegister 0x03B8
|Flag | Description                      |
|-----|----------------------------------|
|Bit 0|Highres mode: Alway 1             |
|Bit 1|1 for Black and white, not used   |
|Bit 3|1 Enable Video output, 0 disable  |
|Bit 5|1 Enable Blink, 0 disable; let Character with bit 7 set blink, If not show high intensity background         |

###CGA
Die CGA Karte besitzt sowohl einen Textmodus, wie auch einen Grafikmodus. 
In beiden Modis gibt es einen High resolution und einen Low resolution mode.

Der High res Mode erlaubt 80x25 Characters im Textmodus. Wobei jeder Char in 8x8px auflöst, was eine Gesamt Auflösung von 640x200px ergibt.

Der Low res Mode erlaubt 40x25 Characters im Textmodus. Wobei jeder Char in 8x8px auflöst, was eine Gesamt Auflösung von 320x200px ergibt.

Im Textmodus benutzt jeder Character 2 Byte, wobei das erste den charcode enthält und das zweite die Farbwerte.
Das Color Byte teilt sich in zwei Nibbel auf, das erste ist für die Vordergrund und das zweite für die Hintergrundfarbe.
Wenn Blinking enabled ist wird dies mit dem 7. Bit aktiviert ansonsten ist dies für Highsensity background zuständig.

| Bit | Farbe                         |
|-----|-------------------------------|
| Bit0| Blue                          |
| Bit1| Green                         |
| Bit2| Red                           |
| Bit3| Bright Foreground             |
| Bit4| Blue                          |
| Bit5| Green                         |
| Bit6| Red                           |
| Bit7| Bright Background, or Blinking|


Die originale CGA Karte unterstützt Paging, so dass unterschiedliche Seiten auf dem Display angezeigt werden können.
Dies ist hier (noch) nicht implementiert.

Der Grafikmodus erlaubt im Highres Mode 640x200px auflösung mit 2 Farben.
wobei eine auf Black festgelegt ist. die 2. Kann über das Colorregister gesetzt werden.

|color | code |
|------|------|
|Black | 0    |
|White | 1    |

Im Low res Mode unterstütz die Karte 320x200px mit 4 Farben + highsensity wert.
Hier lässt sich die erste Farbe frei wählen.

####Colorpalette 0
|color  |highsensity  | code|
|-------|-------------|-----|
|Black  | Black       | 0   |
|Green  | Lightgreen  | 1   |
|Red    | Lightred    | 2   |
|Magenta| Lightmagenta| 3   |

####Colorpalette 1
|color    | Highsensity | code|
|---------|-------------|-----|
|Black    | Black       | 0   |
|Cyan     | Lightcyan   | 1   |
|Magenta  | Lightmagenta| 2   |
|Lightgray| White       | 3   |

####Moderegister 0x03D8

|Flag | Description                      |
|-----|----------------------------------|
|Bit 0|1 for Highres text mode           |
|Bit 1|1 for Graphic mode                |
|Bit 2|1 for Black and white, not used   |
|Bit 3|1 Enable Video output, 0 disable  |
|Bit 4|1 Enable Highres Graphic          |
|Bit 5|1 Enable Blink, 0 disable; only used in textmode|

####Colorregister 0x03D9

|Flag | Description         |
|-----|---------------------|
|Bit 0| Blue                |
|Bit 1| Green               |
|Bit 2| Red                 |
|Bit 3| Intensity           | 
|Bit 4| Bright FG           |
|Bit 5| select color palette|