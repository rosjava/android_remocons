cmake_minimum_required(VERSION 2.8.3)
project(rocon_android)

find_package(catkin REQUIRED rosjava_tools)

catkin_android_setup(assembleRelease)

catkin_package()

