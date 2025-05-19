!define APP_NAME "Pano Scrobbler"
!define DEV_NAME "kawaiiDango"
!define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"
!define EXE_NAME "pano-scrobbler.exe"
!define ICON_REL_PATH "app\resources\app_icon.ico"
!define APP_GUID "85173f4e-ca52-4ec9-b77f-c2e0b1ff4209"

!define MULTIUSER_EXECUTIONLEVEL Standard
!define UNINSTKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"
!define STARTUP_KEY "Software\Microsoft\Windows\CurrentVersion\Run"
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_KEY "${UNINSTKEY}"
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_VALUENAME "CurrentUser"
!define MULTIUSER_INSTALLMODE_INSTDIR "${APP_NAME}"
!define MULTIUSER_INSTALLMODE_COMMANDLINE
!define MULTIUSER_MUI
!define MULTIUSER_USE_PROGRAMFILES64

!include LogicLib.nsh
!include MultiUser.nsh
!include MUI2.nsh
!include FileFunc.nsh

!ifndef OUTFILE
  !error "You must define OUTFILE (e.g. /DOUTFILE=Installer.exe)"
!endif
!ifndef APPDIR
  !error "You must define APPDIR (e.g. /DAPPDIR=build\win-unpacked)"
!endif
!ifndef VERSION_CODE
  !error "You must define VERSION_CODE (e.g. /DVERSION_CODE=123)"
!endif
!ifndef VERSION_NAME
  !error "You must define VERSION_NAME (e.g. /DVERSION_NAME=1.2.3)"
!endif

Name "${APP_NAME} ${VERSION_NAME}"
OutFile "${OUTFILE}"

SetCompressor /SOLID BZIP2

!define MUI_ICON "${APPDIR}\${ICON_REL_PATH}"

Var StartWithWindows
Var CreateStartMenu
Var LaunchApp
Var VersionCodePrev
Var InstallModePrev
Var InstallLocationPrev
Var InstallMode

!macro KILL_IF_RUNNING
  ; Kill running app process before upgrade, up to 3 attempts
  StrCpy $1 0 ; attempt counter

  loop_kill:
    nsExec::ExecToStack 'taskkill /IM "${EXE_NAME}" /F /t'
    Pop $0 ; exit code (0 = success, 128 = not running, etc.)

    ${If} $0 == 128
      ; Process is gone, continue
      Goto done_kill
    ${ElseIf} $0 == 0
      IntOp $1 $1 + 1
      ${If} $1 < 3
        Sleep 500
        Goto loop_kill
      ${EndIf}
    ${Else}
      MessageBox MB_ICONSTOP|MB_OK "Failed to close the running application ($0)."
      Abort
    ${EndIf}

    ; If we reach here after 3 attempts and process is not gone
    MessageBox MB_ICONSTOP|MB_OK "Failed to close the running application after 3 attempts."
    Abort

  done_kill:
!macroend

!macro GET_PREVIOUS_INSTALL_MODE
  ReadRegStr $0 HKCU "${UNINSTKEY}" "InstallLocation"
  ${If} $0 != ""
    StrCpy $InstallModePrev 2
    StrCpy $InstallLocationPrev $0
  ${Else}
    ReadRegStr $0 HKLM "${UNINSTKEY}" "InstallLocation"
    ${If} $0 != ""
      StrCpy $InstallModePrev 1
      StrCpy $InstallLocationPrev $0
    ${EndIf}
  ${EndIf}
!macroend

;--------------------------------
; MUI PAGES

Page Custom PageModeSelect PageModeSelect_Leave

!define MUI_PAGE_CUSTOMFUNCTION_PRE SkipIfUpgrade

!insertmacro MUI_PAGE_DIRECTORY

Page Custom PageInstallOptions PageInstallOptions_Leave

!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Function .onInit
  SetRegView 64
  
  !insertmacro MULTIUSER_INIT

  !insertmacro GET_PREVIOUS_INSTALL_MODE

  ${If} $InstallModePrev == 1
    Call MultiUser.InstallMode.AllUsers
  ${ElseIf} $InstallModePrev == 2
    Call MultiUser.InstallMode.CurrentUser
  ${EndIf}

FunctionEnd

Function un.onInit
  SetRegView 64

  !insertmacro MULTIUSER_UNINIT

  !insertmacro GET_PREVIOUS_INSTALL_MODE

  ${If} $InstallModePrev == 1
    Call un.MultiUser.InstallMode.AllUsers

    UserInfo::GetAccountType
    Pop $0
    ${If} $0 != "admin"
      ; Relaunch as admin
      ExecShell "runas" "$EXEPATH"
      Quit
    ${Else}
      ; $INSTDIR gets set as AppData\Local\Temp\~nsu.tmp after run as admin, so manually set it
      StrCpy $INSTDIR $InstallLocationPrev
    ${EndIf}

  ${ElseIf} $InstallModePrev == 2
    Call un.MultiUser.InstallMode.CurrentUser
  ${EndIf}

FunctionEnd

Function SkipIfUpgrade
  ${If} $InstallModePrev != ""
  ${AndIf} $InstallMode > 0
    StrCpy $LaunchApp 1 ; launch app after upgrade
    Abort ; skip this page if upgrading
  ${EndIf}
FunctionEnd

;--------------------------------
; Page: Mode Select

Function PageModeSelect
  ; Parse command line for /InstallMode=
  ${GetParameters} $R0
  ${GetOptions} $R0 "/InstallMode=" $R1
  ${If} $R1 != "" 
    ${If} $R1 == 1
      Call ModeSelect_Install_AllUsers
    ${ElseIf} $R1 == 2
      Call ModeSelect_Install_CurrentUser
    ${ElseIf} $R1 == 0
      Call ModeSelect_Extract
    ${Else}
      MessageBox MB_ICONSTOP|MB_OK "Invalid installation mode. Please choose a valid option."
      Quit
    ${EndIf}

    Abort ; already set by command line
  ${EndIf}

  nsDialogs::Create 1018
  Pop $0

  !insertmacro MUI_HEADER_TEXT "Choose installation mode:" ""

  ${If} $InstallModePrev != ""

      ${NSD_CreateRadioButton} 0 16u 100% 12u "Upgrade existing installation"
      Pop $2

      ${If} $InstallMode != 0
        ${NSD_SetState} $2 1
        StrCpy $InstallMode $InstallModePrev
      ${EndIf}

      ${NSD_OnClick} $2 ModeSelect_Upgrade
      
  ${Else}
    
    ${If} $InstallMode == ""
      StrCpy $InstallMode 1 ; default to install for all users
    ${EndIf}

    ${NSD_CreateRadioButton} 0 16u 100% 12u "Install for all users"
    Pop $2

    ${If} $InstallMode == 1
      ${NSD_SetState} $2 1
    ${EndIf}

    ${NSD_OnClick} $2 ModeSelect_Install_AllUsers

    ${NSD_CreateRadioButton} 0 32u 100% 12u "Install for current user only"
    Pop $3

    ${If} $InstallMode == 2
      ${NSD_SetState} $3 1
    ${EndIf}
      
    ${NSD_OnClick} $3 ModeSelect_Install_CurrentUser

  ${EndIf}

  ${NSD_CreateRadioButton} 0 48u 100% 12u "Extract only (portable, no registry changes)"
  Pop $4

  ${If} $InstallMode == 0
    ${NSD_SetState} $4 1
  ${EndIf}

  ${NSD_OnClick} $4 ModeSelect_Extract


  nsDialogs::Show
FunctionEnd

Function ModeSelect_Install_AllUsers
  StrCpy $InstallMode 1
  Call MultiUser.InstallMode.AllUsers 
FunctionEnd

Function ModeSelect_Install_CurrentUser
  StrCpy $InstallMode 2
  Call MultiUser.InstallMode.CurrentUser
FunctionEnd

Function ModeSelect_Extract
  StrCpy $InstallMode 0
  Call MultiUser.InstallMode.CurrentUser
  StrCpy $INSTDIR "$EXEDir\${APP_NAME}"
FunctionEnd

Function ModeSelect_Upgrade
  StrCpy $InstallMode $InstallModePrev
FunctionEnd

Function PageModeSelect_Leave
  ${If} $InstallMode == 1
    UserInfo::GetAccountType
    Pop $0
    ${If} $0 != "admin"
      ; Relaunch as admin with mode parameter
      ExecShell "runas" "$EXEPATH" "/InstallMode=1"
      Quit
    ${EndIf}
  ${EndIf}

FunctionEnd

;--------------------------------
; Page: Install Options (only for Install mode)

Function PageInstallOptions
  ${If} $InstallMode >= 1
  ${AndIf} $InstallModePrev == ""
    nsDialogs::Create 1018
    Pop $0

    !insertmacro MUI_HEADER_TEXT "Additional options" ""

    ${NSD_CreateCheckbox} 0 0 100% 12u "Start ${APP_NAME} with Windows"
    Pop $1
    ${NSD_SetState} $1 1
    StrCpy $StartWithWindows 1
    ${NSD_OnClick} $1 InstallOpt_ToggleStartWithWindows

    ${NSD_CreateCheckbox} 0 16u 100% 12u "Create Start Menu shortcut"
    Pop $2
    ${NSD_SetState} $2 1
    StrCpy $CreateStartMenu 1
    ${NSD_OnClick} $2 InstallOpt_ToggleStartMenu

    ${NSD_CreateCheckbox} 0 32u 100% 12u "Launch ${APP_NAME} after installation"
    Pop $3
    ${NSD_SetState} $3 1
    StrCpy $LaunchApp 1
    ${NSD_OnClick} $3 InstallOpt_ToggleLaunchApp

    nsDialogs::Show
  ${Else}
    Abort ; skip this page in extract mode and upgrade
  ${EndIf}
FunctionEnd

Function InstallOpt_ToggleStartWithWindows
  ${NSD_GetState} $1 $StartWithWindows
FunctionEnd

Function InstallOpt_ToggleStartMenu
  ${NSD_GetState} $2 $CreateStartMenu
FunctionEnd

Function InstallOpt_ToggleLaunchApp
  ${NSD_GetState} $3 $LaunchApp
FunctionEnd

Function PageInstallOptions_Leave
  ; nothing needed, values already set
FunctionEnd

;--------------------------------
; Installer Section

Section "Install/Extract"
  SetOutPath "$INSTDIR"

  ; Version check (if install mode)
  ${If} $InstallMode >= 1
    ReadRegDWORD $VersionCodePrev ShCtx "${UNINST_KEY}" "VersionCode"
    ${If} $VersionCodePrev <> ""
      ${If} $VersionCodePrev > ${VERSION_CODE}
        MessageBox MB_ICONSTOP|MB_OK "A newer version is already installed. Downgrade is not allowed."
        Abort
      ${EndIf}
    ${EndIf}

    ; If upgrading, clear install dir
    !insertmacro KILL_IF_RUNNING

    ${If} $VersionCodePrev <> ""
      RMDir /r "$INSTDIR"
      CreateDirectory "$INSTDIR"
    ${EndIf}

  ${EndIf}


  ; Copy files
  File /r "${APPDIR}\*"

  ${If} $InstallMode >= 1
    ;Store installation folder
    WriteRegStr ShCtx "Software\${APP_NAME}" "" $INSTDIR

    ; Create uninstaller (install mode)
    WriteUninstaller "$INSTDIR\Uninstall.exe"

    ; Set icon for Add/Remove Programs
    WriteRegStr ShCtx "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\${ICON_REL_PATH}"
    WriteRegStr ShCtx "${UNINST_KEY}" "DisplayName" "${APP_NAME}"
    WriteRegStr ShCtx "${UNINST_KEY}" "Publisher" "${DEV_NAME}"
    WriteRegStr ShCtx "${UNINST_KEY}" "DisplayVersion" "${VERSION_NAME}"
    WriteRegStr ShCtx "${UNINST_KEY}" "UninstallString" "$INSTDIR\Uninstall.exe"
    WriteRegStr ShCtx "${UNINST_KEY}" "InstallLocation" "$INSTDIR"
    WriteRegDWORD ShCtx "${UNINST_KEY}" "VersionCode" ${VERSION_CODE}
    WriteRegStr ShCtx "${UNINST_KEY}" "AppGuid" "${APP_GUID}"
    ; Write MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_VALUENAME so the correct context can be detected in the uninstaller.
    WriteRegStr ShCtx "${UNINST_KEY}" $MultiUser.InstallMode 1

    ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
    IntFmt $0 "0x%08X" $0
    WriteRegDWORD ShCtx "${UNINST_KEY}" "EstimatedSize" "$0"
 

    ; Start with Windows (install mode)
    ${If} $StartWithWindows == 1
      WriteRegStr HKCU "${STARTUP_KEY}" "${APP_NAME}" '"$INSTDIR\${EXE_NAME}" --minimized'
    ${EndIf}

    ; Start Menu shortcut (install mode)
    ${If} $CreateStartMenu == 1
      CreateShortCut "$SMPROGRAMS\${APP_NAME}.lnk" "$INSTDIR\${EXE_NAME}" "" "$INSTDIR\${ICON_REL_PATH}"
    ${EndIf}
  ${EndIf}

SectionEnd

;--------------------------------
; Post-install: Launch app if checked

Section -Post
  ${If} $LaunchApp == 1
    Exec '"$INSTDIR\${EXE_NAME}"'
  ${EndIf}
SectionEnd

;--------------------------------
; Uninstaller Section

Section "Uninstall"

  !insertmacro KILL_IF_RUNNING

  ; Remove registry keys
  DeleteRegKey ShCtx "${UNINST_KEY}"
  DeleteRegValue HKCU "${STARTUP_KEY}" "${APP_NAME}"
  DeleteRegKey ShCtx "Software\${APP_NAME}"

  ; Remove Start Menu shortcut
  Delete "$SMPROGRAMS\${APP_NAME}.lnk"

  ; Remove installed files
  RMDir /r "$INSTDIR"

  ; Ask about user data
  MessageBox MB_YESNO|MB_ICONQUESTION "Remove user data?" IDYES RemoveUserData
  Goto SkipUserData
  RemoveUserData:
    RMDir /r "$APPDATA\pano-scrobbler"
  SkipUserData:

SectionEnd

;--------------------------------
; EOF