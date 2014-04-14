Android Remocons
================

Remocons for pairing and concert modes as well as some simple apps for testing.

Installation
============

This will use the maven repos for the core rosjava and android repos.

```
> yujin_init_workspace -j5 ~/rocon_rosjava rocon-rosjava
> cd ~/rocon_rosjava
> yujin_init_build .
> yujin_make --install-rosdeps
> yujin_make
> . .bashrc
```

In another shell:

```
> yujin_init_workspace -j5 ~/android_interactions
> cd ~/android_interactions
> yujin_init_build --underlays="~/rocon_rosjava/devel" .
> cd ~/android_interactions/src
> wstool set android_remocons --git https://github.com/robotics-in-concert/android_remocons.git --version=master
> wstool set rocon_android_apps --git https://github.com/robotics-in-concert/rocon_android_apps.git --version=headless_launcher_update
> wstool update -j2
> yujin_make --install-rosdeps
> yujin_make
> . .bashrc
```

Launch the android studio, complie the ```rocon_nfc_writer``` and ```headless_launcher```

For Rocon NFC Writer Test
============

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


For Headless Launcher Test
============

### Connect network
1. Write the wireless network name you want to connect in NFC
2. Connect another wireless network
3. Tagging the nfc
4. Check the current wireless network whether or not

### Launching the App
1. Install the app you want to launch
2. Check the app hash using ```rocon_interaction``` in command line
3. Write the app hash in NFC 
4. Tagging the nfc
5. Check the app launcing whether or not






