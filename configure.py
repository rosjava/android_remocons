#!/usr/bin/env python

from __future__ import print_function
import os
import sys
import subprocess
import rospkg
import argparse
from argparse import RawTextHelpFormatter
import copy

def parse_arguments():
    overview = 'Auto-generates project.properties for each android project under this directory.'
    parser = argparse.ArgumentParser(description=overview, formatter_class=RawTextHelpFormatter)
    parser.add_argument('package', nargs='?', default=None, help='name of the package in which to find the test configuration')
    parser.add_argument('-t', '--target', action='store_true', default='android-17', help='android target version to pass to android update [android-17]')
    args = parser.parse_args()
    return args

def is_library(package):
    is_library_flag = rospack.get_manifest(package).get_export('android', 'library')
    return True if is_library_flag else False

if __name__ == '__main__':
    args = parse_arguments()
    cwd = os.getcwd()
    android_version = args.target
    properties_filename = 'project.properties'
    # Assumption is that each package is a an android project
    package_names = rospkg.list_by_path('manifest.xml', cwd, None)
    rospack = rospkg.RosPack()

    for package in package_names:
        print("  Package: %s" % package)
        package_relpath = os.path.relpath(rospack.get_path(package), cwd)
        package_depends = rospack.get_depends(package)
        print("    Path: %s" % package_relpath)
        # Remove project.properties and start fresh since we need relative 
        # pathnames to all the library dependencies.
        try:
            os.remove(os.path.join(package_relpath, properties_filename))
        except OSError:
            pass  # ignore missing file
        cmd = ['android', 'update', 'project', '--path', package_relpath, '--target', android_version ]
        print("    Command: %s" %cmd)
        subprocess.check_call(cmd)
        for library in package_depends:
            library_relpath = os.path.relpath(rospack.get_path(library), package_relpath)
            library_cmd = copy.deepcopy(cmd)
            library_cmd.extend(['--library', library_relpath])
            print("    Library Command: %s" % ' '.join(library_cmd))
            subprocess.check_call(library_cmd)
        if is_library(package):
            with open(os.path.join(package_relpath, properties_filename), "a") as f:
                f.write('android.library=true')
