#define APP_NAME "Pano Scrobbler"
#define APP_NAME_WITHOUT_SPACES "pano-scrobbler"
#define DEV_NAME "kawaiiDango"
#define EXE_NAME "pano-scrobbler.exe"
#define APP_GUID "85173f4e-ca52-4ec9-b77f-c2e0b1ff4209"
#define APP_AUMID "com.arn.scrobble"
#define NSIS_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall"
#define NSIS_APP_SOFTWARE_KEY "Software"
#define NSIS_LOCALSERVER32_KEY "SOFTWARE\Classes\CLSID"
#ifndef VERSION
  #define VERSION "4.28"
#endif
#ifndef OUT_DIR
  #define OUT_DIR "..\..\dist"
#endif
#ifndef APP_DIR
  #define APP_DIR "..\build\compose\native\windows-x64"
#endif
#ifndef ICON_FILE
  #define ICON_FILE "..\app-icons\pano-scrobbler.ico"
#endif

[Setup]
AppName={#APP_NAME}
AppVersion={#VERSION}
AppId={#APP_GUID}
WizardStyle=modern dynamic
DefaultDirName={autopf}\{#APP_NAME}
DefaultGroupName={#APP_NAME}
UninstallDisplayIcon={app}\{#EXE_NAME}
Compression=lzma2
SolidCompression=yes
OutputDir={#OUT_DIR}
OutputBaseFilename=pano-scrobbler-windows-x64
ChangesAssociations=no
UserInfoPage=no
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
SetupIconFile={#ICON_FILE}
VersionInfoProductName={#APP_NAME}
VersionInfoDescription={#APP_NAME}
AppPublisher={#DEV_NAME}
AppCopyright="(c) {#DEV_NAME}"
VersionInfoVersion={#VERSION}
VersionInfoProductVersion={#VERSION}
CloseApplications=yes
DisableProgramGroupPage=yes
DisableFinishedPage=yes
DisableReadyPage=yes
; AppMutex=pano-scrobbler-mutex
Uninstallable=not WizardIsTaskSelected('extractonly')

[Files]
Source: "{#APP_DIR}\*"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#APP_DIR}\bin\*"; DestDir: "{app}\bin"; Flags: ignoreversion

[Tasks]
Name: "runonstartup"; Description: "Run at startup"; Flags: exclusive; Check: IsFreshInstall
Name: "dontrunonstartup"; Description: "Don't run at startup"; Flags: exclusive unchecked; Check: IsFreshInstall
Name: "extractonly"; Description: "Extract only, no install"; Flags: exclusive unchecked; Check: IsFreshInstall
Name: "msvcneeded"; Description: "❗Install missing Visual C++ Redistributable, required by this app"; Check: NeedsVCRedist

[Icons]
Name: "{autoprograms}\{#APP_NAME}"; Filename: "{app}\{#EXE_NAME}"; WorkingDir: "{app}"; AppUserModelID: "{#APP_AUMID}"; AppUserModelToastActivatorCLSID: "{#APP_GUID}"; Tasks: not extractonly
Name: "{autostartup}\{#APP_NAME}"; Filename: "{app}\{#EXE_NAME}"; Parameters: "--minimized"; WorkingDir: "{app}"; Tasks: runonstartup

[Run]
; Run the main application after installation
Filename: "{app}\{#EXE_NAME}"; Description: "Run Application"; Flags: postinstall nowait skipifsilent; Tasks: not msvcneeded
; Open msvc url after installation if needed
Filename: "https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170#latest-supported-redistributable-version"; Description: "Download Visual C++ Redistributable"; Flags: postinstall shellexec skipifsilent; Tasks: msvcneeded

[UninstallRun]
; Kill the main application during uninstallation
Filename: "{app}\{#EXE_NAME}"; Parameters: "--quit"; Flags: skipifdoesntexist

[InstallDelete]
; leftover from nsis
Type: files; Name: "{app}\install.log"; Check: HasNsisLeftovers
Type: files; Name: "{app}\Uninstall.exe"; Check: HasNsisLeftovers

[Registry]
; leftover from nsis
Root: HKA; Subkey: "{#NSIS_APP_SOFTWARE_KEY}\{#APP_NAME}"; Flags: deletekey; Check: HasNsisLeftovers
Root: HKA; Subkey: "{#NSIS_UNINST_KEY}\{#APP_NAME}"; Flags: deletekey; Check: HasNsisLeftovers
Root: HKA; Subkey: "{#NSIS_LOCALSERVER32_KEY}\{#APP_GUID}"; Flags: deletekey; Check: HasNsisLeftovers

[Messages]
SelectDirLabel3=❗NOTE: [name] cannot be installed in non-ASCII paths.

[Code]
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  AppDataPath: String;
begin
  if CurUninstallStep = usPostUninstall then
  begin
    if UninstallSilent then
      Exit;
    if MsgBox('Delete user data (settings, cache, etc.)?', mbConfirmation, MB_YESNO or MB_DEFBUTTON2) = IDYES then
    begin
      AppDataPath := ExpandConstant('{userappdata}\{#APP_NAME_WITHOUT_SPACES}');
      if DirExists(AppDataPath) then
        DelTree(AppDataPath, True, True, True);
    end;
  end;
end;

function IsFreshInstall: Boolean;
begin
  { Treat as upgrade if app EXE already exists in selected install directory }
  Result := not FileExists(ExpandConstant('{app}\{#EXE_NAME}'));
end;

function HasNsisLeftovers: Boolean;
begin
  Result := FileExists(ExpandConstant('{app}\install.log'));
end;

function NeedsVCRedist: Boolean;
var
  Installed: Cardinal;
begin
  { Check for Visual C++ Redistributable (x64)}
  Result := not RegQueryDWordValue(
    HKLM,
    'SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64',
    'Installed',
    Installed
  ) or (Installed <> 1);
end;