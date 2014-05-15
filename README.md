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

Other README's
==============

* rocon_nfc_writer/README.md
* headless_launcher/README.md

