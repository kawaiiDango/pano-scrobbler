!define APP_NAME "Pano Scrobbler"
!define DEV_NAME "kawaiiDango"
!define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"
!define EXE_NAME "pano-scrobbler.exe"
!define APP_GUID "85173f4e-ca52-4ec9-b77f-c2e0b1ff4209"
!define APP_AUMID "com.arn.scrobble"

!define MULTIUSER_EXECUTIONLEVEL Highest
!define UNINSTKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"
!define LOCALSERVER32_KEY "SOFTWARE\Classes\CLSID\${APP_GUID}\LocalServer32"
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
!include shortcut-properties.nsh

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
!ifndef ICON_FILE
  !error "You must define ICON_FILE (e.g. /DICON_FILE=app-icon.ico)"
!endif

Name "${APP_NAME} ${VERSION_NAME}"
OutFile "${OUTFILE}"

SetCompressor /SOLID BZIP2

!define MUI_ICON "${ICON_FILE}"

Var StartWithWindows
Var LaunchApp
Var VersionCodePrev
Var UninstallRemoveUserData
Var IsExtractMode
Var IsMinimalInteractionMode

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

!macro GET_PREVIOUS_INSTALL_PROPS
  ReadRegDWORD $VersionCodePrev ShCtx "${UNINST_KEY}" "VersionCode"
!macroend

;--------------------------------
; MUI PAGES

Page Custom PageModeSelect PageModeSelect_Leave

!define MUI_PAGE_CUSTOMFUNCTION_PRE SkipIfUpgrade

!insertmacro MUI_PAGE_DIRECTORY

Page Custom PageInstallOptions PageInstallOptions_Leave

!insertmacro MUI_PAGE_INSTFILES

UninstPage Custom un.PageRemoveUserData un.PageRemoveUserData_Leave
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Function .onInit
  SetRegView 64
  
  !insertmacro MULTIUSER_INIT
  !insertmacro GET_PREVIOUS_INSTALL_PROPS

  StrCpy $LaunchApp 1
  StrCpy $StartWithWindows 1

  ; Check for minimal interaction mode

  ${GetFileName} $EXEPATH $R0
  ${If} $R0 == "pano-scrobbler-update.exe"
    StrCpy $IsMinimalInteractionMode 1
    SetAutoClose true
  ${Else}
    StrCpy $IsMinimalInteractionMode 0
  ${EndIf}
FunctionEnd

Function un.onInit
  SetRegView 64

  !insertmacro MULTIUSER_UNINIT
  !insertmacro GET_PREVIOUS_INSTALL_PROPS
FunctionEnd

Function SkipIfUpgrade
  ${If} $IsMinimalInteractionMode == 1
    Abort ; skip this page in minimal interaction mode
  ${ElseIf} $VersionCodePrev != ""
  ${AndIf} $IsExtractMode != 1
    Abort ; skip this page if upgrading
  ${EndIf}
FunctionEnd

;--------------------------------
; Page: Mode Select

Function PageModeSelect
  ${If} $IsMinimalInteractionMode == 1
    Abort ; skip this page in minimal interaction mode
  ${EndIf}

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

  ${If} $VersionCodePrev != ""

      ${NSD_CreateRadioButton} 0 16u 100% 12u "Upgrade existing installation"
      Pop $2

      ${If} $IsExtractMode != 1
        ${NSD_SetState} $2 1
      ${EndIf}

      ${NSD_OnClick} $2 ModeSelect_Upgrade
      
  ${Else}
    ${NSD_CreateRadioButton} 0 16u 100% 12u "Install for all users"
    Pop $2

    ${If} $MultiUser.InstallMode == "AllUsers"
    ${AndIf} $IsExtractMode != 1
      ${NSD_SetState} $2 1
    ${EndIf}

    ${NSD_OnClick} $2 ModeSelect_Install_AllUsers

    ${NSD_CreateRadioButton} 0 32u 100% 12u "Install for current user only"
    Pop $3

    ${If} $MultiUser.InstallMode == "CurrentUser"
    ${AndIf} $IsExtractMode != 1
      ${NSD_SetState} $3 1
    ${EndIf}
      
    ${NSD_OnClick} $3 ModeSelect_Install_CurrentUser

  ${EndIf}

  ${NSD_CreateRadioButton} 0 48u 100% 12u "Extract only (portable, no registry changes)"
  Pop $4

  ${If} $IsExtractMode == 1
    ${NSD_SetState} $4 1
  ${EndIf}

  ${NSD_OnClick} $4 ModeSelect_Extract

  ${NSD_CreateLabel} 0 80u 100% 12u "This application needs Microsoft Visual C++ Redistributable to run."
  Pop $5

  ${NSD_CreateButton} 0 96u 50% 12u "Download Visual C++ Redistributable"
  Pop $6

  ${NSD_OnClick} $6 OpenVcRedistLink

  nsDialogs::Show
FunctionEnd

Function ModeSelect_Install_AllUsers
  StrCpy $IsExtractMode 0
  Call MultiUser.InstallMode.AllUsers
FunctionEnd

Function ModeSelect_Install_CurrentUser
  StrCpy $IsExtractMode 0
  Call MultiUser.InstallMode.CurrentUser
FunctionEnd

Function ModeSelect_Extract
  StrCpy $IsExtractMode 1
  Call MultiUser.InstallMode.CurrentUser
  StrCpy $INSTDIR "$EXEDir\${APP_NAME}"
FunctionEnd

Function ModeSelect_Upgrade
  StrCpy $IsExtractMode 0
FunctionEnd

Function OpenVcRedistLink
  ExecShell "open" "https://aka.ms/vs/17/release/vc_redist.x64.exe"
FunctionEnd

Function PageModeSelect_Leave

FunctionEnd

;--------------------------------
; Page: Install Options (only for Install mode)

Function PageInstallOptions
  ${If} $IsMinimalInteractionMode == 1
    Abort ; skip this page in minimal interaction mode
  ${ElseIf} $IsExtractMode != 1
    nsDialogs::Create 1018
    Pop $0

    !insertmacro MUI_HEADER_TEXT "Additional options" ""

    ${NSD_CreateCheckbox} 0 0 100% 12u "Start ${APP_NAME} with Windows"
    Pop $1
    ${NSD_SetState} $1 $StartWithWindows
    ${NSD_OnClick} $1 InstallOpt_ToggleStartWithWindows

    ${NSD_CreateCheckbox} 0 32u 100% 12u "Launch ${APP_NAME} after installation"
    Pop $3
    ${NSD_SetState} $3 $LaunchApp
    ${NSD_OnClick} $3 InstallOpt_ToggleLaunchApp

    nsDialogs::Show
  ${Else}
    Abort ; skip this page in extract mode and upgrade
  ${EndIf}
FunctionEnd

Function InstallOpt_ToggleStartWithWindows
  ${NSD_GetState} $1 $StartWithWindows
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
  ${If} $IsExtractMode != 1
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

  ${If} $IsExtractMode != 1
    ;Store installation folder
    WriteRegStr ShCtx "Software\${APP_NAME}" "" $INSTDIR

    ; Create uninstaller (install mode)
    WriteUninstaller "$INSTDIR\Uninstall.exe"

    ; Set icon for Add/Remove Programs
    WriteRegStr ShCtx "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\${EXE_NAME}"
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


    ; Start Menu shortcut (install mode)
    ; $OUTDIR is used for the working directory

    CreateShortCut "$SMPROGRAMS\${APP_NAME}.lnk" "$INSTDIR\${EXE_NAME}"
    !insertmacro ShortcutSetToastProperties "$SMPROGRAMS\${APP_NAME}.lnk" "${APP_GUID}" "${APP_AUMID}"
    ; Add the needed Registry Key (https://docs.microsoft.com/en-us/windows/win32/com/localserver32)
    WriteRegStr ShCtx "${LOCALSERVER32_KEY}" "" "$INSTDIR\${EXE_NAME}"


    ; Start with Windows (install mode)
    ${If} $StartWithWindows == 1
      CreateShortCut "$SMSTARTUP\${APP_NAME}.lnk" "$INSTDIR\${EXE_NAME}" " --minimized"
    ${Else}
      Delete "$SMSTARTUP\${APP_NAME}.lnk"
    ${EndIf}
  ${EndIf}

SectionEnd

;--------------------------------
; Post-install: Launch app if checked

Section -Post
  ${If} $LaunchApp == 1
    ; explorer.exe makes it run as unelevated
    ExecWait 'explorer.exe "$INSTDIR\${EXE_NAME}"'
  ${EndIf}
SectionEnd

;--------------------------------
; Uninstaller Custom Page

Function un.PageRemoveUserData
  nsDialogs::Create 1018
  Pop $0

  !insertmacro MUI_HEADER_TEXT "Uninstall Options" ""

  ${NSD_CreateCheckbox} 0 0 100% 12u "Remove user data (settings, cache, etc.)"
  Pop $1
  ${NSD_SetState} $1 0
  StrCpy $UninstallRemoveUserData 0
  ${NSD_OnClick} $1 un.ToggleRemoveUserData

  nsDialogs::Show
FunctionEnd

Function un.ToggleRemoveUserData
  ${NSD_GetState} $1 $UninstallRemoveUserData
FunctionEnd

Function un.PageRemoveUserData_Leave
  ; Nothing needed, value is already set
FunctionEnd

;--------------------------------
; Uninstaller Section

Section "Uninstall"

  !insertmacro KILL_IF_RUNNING

  ; Remove registry keys
  DeleteRegKey ShCtx "${UNINST_KEY}"
  DeleteRegKey ShCtx "Software\${APP_NAME}"
  DeleteRegKey ShCtx "${LOCALSERVER32_KEY}"

  ; Remove Start Menu shortcut
  Delete "$SMPROGRAMS\${APP_NAME}.lnk"
  Delete "$SMSTARTUP\${APP_NAME}.lnk"

  ; Remove installed files
  RMDir /r "$INSTDIR"

  ; Remove user data if checked
  SetShellVarContext current  ; Otherwise, uses ProgramData
  ${If} $UninstallRemoveUserData == 1
    RMDir /r "$APPDATA\pano-scrobbler"
  ${EndIf}

SectionEnd

;--------------------------------
; EOF