;
; Nullsoft Scriptable Install System
; Install script for JXM on Windows
;
; $Id: jxm.nsi,v 1.16 2004/06/27 16:58:27 nsayer Exp $

;Based on a template by Joost Verburg

;--------------------------------
;Include Modern UI

  !include "MUI.nsh"

;--------------------------------
;General

  ;Name and file
  Name "JXM 1.2"
  OutFile "c:\docume~1\nsayer\desktop\jxm.exe"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\JXM"
  
;--------------------------------
;Variables

  Var MUI_TEMP
  Var STARTMENU_FOLDER
  Var JAVA_HOME
  Var JAVA_VER

  !define GET_JAVA "http://www.java.com/"

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING
#  !define MUI_ICON "jxm.ico"
;--------------------------------
;Pages

#  Page custom CheckJRE
  !define MUI_PAGE_CUSTOMFUNCTION_LEAVE CheckJRE
  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE "COPYING.txt" # this is just ../COPYING with CRLF endings
  !insertmacro MUI_PAGE_DIRECTORY
  
  ;Start Menu Folder Page Configuration
  !define MUI_STARTMENUPAGE_DEFAULTFOLDER "JXM"
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\JXM" 
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"
  !define MUI_STARTMENUPAGE_NODISABLE "yes"

  !insertmacro MUI_PAGE_STARTMENU JXM $STARTMENU_FOLDER
  
  !insertmacro MUI_PAGE_INSTFILES
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Function CheckJRE

  Push $R0
  Push $R1
   ReadRegStr $JAVA_VER HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
   IfErrors +1 noErrors
     MessageBox MB_OK|MB_ICONSTOP "You do not appear to have a Java Runtime Environment installed."
     Exec '"explorer.exe" ${GET_JAVA}'
     Quit

noErrors:
   StrCpy $R0 $JAVA_VER 1 0
   StrCpy $R1 $JAVA_VER 1 2
   IntCmp 14 "$R0$R1" GoodJRE GoodJRE BadJRE

BadJRE:
     MessageBox MB_OK|MB_ICONSTOP "Your Java Runtime Environment is version $JAVA_VER. You must have version 1.4 or above to run JXM."
     Exec '"explorer.exe" ${GET_JAVA}'
     Quit

GoodJRE:
  ReadRegStr $JAVA_HOME HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$JAVA_VER" JavaHome
  Pop $R1
  Pop $R0

FunctionEnd

Section "Working Section" SecWork

  SetOutPath "$INSTDIR"
  File jxm.jar
  File jxm.ico
  File trayicon.jar
  File TrayIcon12.dll
  File registry.jar
  File ICE_JNIRegistry.dll
  File comm.jar
  File win32com.dll

  ; Stupid javax.comm!!! It *requires* this file be put in with the JRE.
  ; The jar merely needs to be in the classpath and the DLL merely needs
  ; to be in the windows %PATH%, but the stupid one-liner property file
  ; must be in $JAVA_HOME\lib. It's that sort of thing that causes unrest.
  SetOutPath "$JAVA_HOME\lib"
  File javax.comm.properties

  ; We used to put the DLL in with the JRE.
  ;SetOutPath "$JAVA_HOME\bin"
  ;SetOutPath "$JAVA_HOME\lib\ext"

  ;Store installation folder
#  WriteRegStr HKCU "Software\Modern UI Test" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\JXM" "DisplayName" "JXM" 
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\JXM" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\JXM" "URLInfoAbout" 'http://www.javaxm.com/'
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\JXM" "DisplayIcon" "$INSTDIR\jxm.ico"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\JXM" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\JXM" "NoRepair" 1

  !insertmacro MUI_STARTMENU_WRITE_BEGIN JXM
    
    ;Create shortcuts
    SetOutPath "$INSTDIR"
    CreateDirectory "$SMPROGRAMS\$STARTMENU_FOLDER"
    CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
    # This is a complex shortcut. We need to add all of our JARs to the
    # classpath, and run the main class. We used to use -jar to run
    # jxm.jar, but that conflicts with -cp. Oh, and the startup dir
    # must be $INSTDIR so that GetProperty("user.dir") (not to mention %PATH%)
    # is helpful.
    #
    # Don't forget to add any additional JARs *cough*systray*cough* to the
    # list.
    #
    # Oh, and this crap is why the start menu shortcut is NOT optional. :-)
    CreateShortCut "$SMPROGRAMS\$STARTMENU_FOLDER\JXM.lnk" '$JAVA_HOME\bin\javaw.exe' '-cp comm.jar;registry.jar;trayicon.jar;jxm.jar com.kfu.JXM.JXM' "$INSTDIR\jxm.ico" 0
  
  !insertmacro MUI_STARTMENU_WRITE_END

SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_SecWork ${LANG_ENGLISH} "The main installation section."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecWork} $(DESC_SecWork)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END
 
;--------------------------------
;Uninstaller Section

Section "Uninstall"

  !insertmacro MUI_STARTMENU_GETFOLDER JXM $MUI_TEMP
    
  Push $R0
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $JAVA_HOME HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" JavaHome
  Pop $R0

  Delete /REBOOTOK "$INSTDIR\Uninstall.exe"
  Delete /REBOOTOK "$INSTDIR\jxm.jar"
  Delete /REBOOTOK "$INSTDIR\jxm.ico"
  Delete /REBOOTOK "$INSTDIR\comm.jar"
  Delete /REBOOTOK "$INSTDIR\win32com.dll"
  Delete /REBOOTOK "$INSTDIR\trayicon.jar"
  Delete /REBOOTOK "$INSTDIR\TrayIcon12.dll"
  Delete /REBOOTOK "$INSTDIR\registry.jar"
  Delete /REBOOTOK "$INSTDIR\ICE_JNIRegistry.dll"
  Delete /REBOOTOK "$JAVA_HOME\lib\javax.comm.properties"

  RMDir "$INSTDIR"
  
  Delete "$SMPROGRAMS\$MUI_TEMP\Uninstall.lnk"
  Delete "$SMPROGRAMS\$MUI_TEMP\JXM.lnk"
  
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\JXM"

  ;Delete empty start menu parent diretories
  StrCpy $MUI_TEMP "$SMPROGRAMS\$MUI_TEMP"
 
  startMenuDeleteLoop:
    RMDir $MUI_TEMP
    GetFullPathName $MUI_TEMP "$MUI_TEMP\.."
    
    IfErrors startMenuDeleteLoopDone
  
    StrCmp $MUI_TEMP $SMPROGRAMS startMenuDeleteLoopDone startMenuDeleteLoop
  startMenuDeleteLoopDone:

#  DeleteRegKey /ifempty HKCU "Software\Modern UI Test"

SectionEnd
