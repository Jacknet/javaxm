/*

 JXM - XMPCR control program for Java
 Copyright (C) 2003-2004 Nicholas W. Sayer

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 $Id: MacOSX_JNI.c,v 1.3 2004/03/20 18:55:30 nsayer Exp $

 Build with this command:

 gcc -dynamiclib -fPIC -I/Library/Java/Home/include -O -o libMacOSX.jnilib -framework CoreFoundation -framework IOKit MacOSX_JNI.c

 Put libMacOSX.jnilib in the Contents/Resources/Java directory of the bundle. For other platforms, it doesn't matter, since it
 won't load anyhow.

 */

#include <CoreFoundation/CoreFoundation.h>

#include <IOKit/IOKitLib.h>
#include <IOKit/serial/IOSerialKeys.h>
#include <IOKit/IOBSD.h>
#include <IOKit/IOCFBundle.h>

#include "MacOSX_JNI.h"

const char *getBundleNameForDevice(const char *fname) {
    static char retval[1024]; // that really ought to be enough. I'm too lazy to alloc and release.
    mach_port_t masterPort;
    io_iterator_t matchingServices;
    CFMutableDictionaryRef classesToMatch;
    kern_return_t kernResult;
    io_object_t device;
    CFStringRef CFfname;

    kernResult = IOMasterPort(MACH_PORT_NULL, &masterPort);
    if (KERN_SUCCESS != kernResult)
    {
        return NULL;
    }

    // We want to find the BSD serial device with the given name.
    classesToMatch = IOServiceMatching(kIOSerialBSDServiceValue);
    CFfname = CFStringCreateWithCString(NULL, fname, kCFStringEncodingASCII);
    CFDictionarySetValue(classesToMatch,
                         CFSTR(kIODialinDeviceKey),
                         CFfname);
    CFRelease(CFfname);
    
    kernResult = IOServiceGetMatchingServices(masterPort, classesToMatch, &matchingServices);
    if (KERN_SUCCESS != kernResult)
    {
        return NULL;
    }

    while((device = IOIteratorNext(matchingServices))) {
        CFTypeRef bundleIDAsCFString;
        Boolean result;
        io_object_t parent, superParent;

        // Now we go up two levels and look for the bundle ID.
        if (IORegistryEntryGetParentEntry(device, kIOServicePlane, &parent) != KERN_SUCCESS)
            continue;
        if (IORegistryEntryGetParentEntry(parent, kIOServicePlane, &superParent) != KERN_SUCCESS)
            continue;
        bundleIDAsCFString = IORegistryEntryCreateCFProperty(superParent,
                                                             kIOBundleIdentifierKey,
                                                             kCFAllocatorDefault,
                                                             0);
        
        if (bundleIDAsCFString == NULL)
            continue;
        
        result = CFStringGetCString(bundleIDAsCFString,
                                    retval,
                                    sizeof(retval) - 1,
                                    kCFStringEncodingASCII);
        
        CFRelease(bundleIDAsCFString);

        if (result)
            return retval;
    }

    return NULL;
}

JNIEXPORT jstring JNICALL Java_com_kfu_Platform_MacOSX_getBundleNameForDevice(JNIEnv *env, jclass _ignore, jstring filenameObj) {
    const char *fname, *ret;

    fname = (*env)->GetStringUTFChars(env, filenameObj, NULL);
    ret = getBundleNameForDevice(fname);
    (*env)->ReleaseStringUTFChars(env, filenameObj, fname);
    return (*env)->NewStringUTF(env, ret);    
}


