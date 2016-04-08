#!/bin/bash

# Automates building for screengrab, launching the emulators and taking screenshots
# for all 3 form factors
# Depends on https://github.com/fastlane/fastlane/tree/master/screengrab
# Currently (2016-04-06), screengrab fails like https://github.com/fastlane/fastlane/issues/2077
# Reverting to version 0.38.0 of fastlane_core worked:
# sudo gem install fastlane_core -v 0.38.0
# sudo gem uninstall fastlane_core -v 0.41.2
# Installed version of fastlane_core can be seen in the path of
# command_executor.rb in the stacktrace. 

# Names of AVD images for phone, 7-inch, 10-inch devices
avd_images=( "Nexus_5_API_23_x86" "Nexus_7_API_23" "Nexus_10_API_23" )
device_types=( "phone" "sevenInch" "tenInch" )

echo Killing all emulators first!
adb emu kill

./gradlew assembleScreengrabDebugAndroidTest assembleScreengrabDebug

for i in "${!avd_images[@]}"
do
  # start emulator
  /opt/android-sdk/tools/emulator -netdelay none -netspeed full -avd ${avd_images[$i]} &
  echo Emulator started, waiting for boot
  # wait until adb is connected to device, so that we can issue adb shell commands
  adb wait-for-device
  
  # wait until boot is completed (see http://ncona.com/2014/01/detect-when-android-emulator-is-ready/ )
  output=''
  while [[ ${output:0:7} != 'stopped' ]]; do
    output=`adb shell getprop init.svc.bootanim`
    sleep 1
    echo ...waiting
  done
  
  sleep 1 
  # unlock lockscreen
  adb shell input keyevent 82
  
  echo Device online, initiating screengrabs

  # now let the screengrab script run
  screengrab --device_type ${device_types[$i]}
  
  echo Done, killing this emulator now!
  # finally kill the emulator
  adb -s emulator-5554 emu kill
done

