Rocon NFC Writer
================

First, enable NFC function in your smart phone and launch the ```Rocon NFC tags writer``` app. 
You can see the following items.

```
SSID: <wireless AP name>
Password: <wireless AP password>
Concert ROS Master URI
    Host: <concert ip>
    Port: <concert port>
    App hash: <4byte integer>
    Extra data: <2byte integer>
    App record: <app package name>
```    
The App hash is unique id allocated client.It is got by ```rocon_interaction```
Extra data is argument which is used when app launch.

The user type the all items, and taggint the NFC.
If recognizing NFC is successed, the message is chagnged to ```You Can write Nfc tag```
Now, you push the ```Write to NFC tag``` button with tagged NFC, and then message change to ``` Success to write NFC tag!``` but if failure, ```Fail to write NFC Tag!```

