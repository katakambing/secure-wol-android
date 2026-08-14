import os
import subprocess

desktop = os.path.join(os.environ['USERPROFILE'], 'Desktop')
lnk_path = os.path.join(desktop, 'Secure WOL Control Center.lnk')
bat_path = r'C:\Users\PC\.gemini\antigravity\scratch\SecureWolApp\windows-agent\Launch-PC-App.bat'
work_dir = r'C:\Users\PC\.gemini\antigravity\scratch\SecureWolApp\windows-agent'
ico_path = r'C:\Users\PC\.gemini\antigravity\scratch\SecureWolApp\windows-agent\app_logo.ico'

vbs = f'''Set oWS = WScript.CreateObject("WScript.Shell")
sLinkFile = "{lnk_path}"
Set oLink = oWS.CreateShortcut(sLinkFile)
oLink.TargetPath = "{bat_path}"
oLink.WorkingDirectory = "{work_dir}"
oLink.IconLocation = "{ico_path}"
oLink.Description = "Secure WOL Control Center"
oLink.Save
'''

vbs_file = os.path.join(work_dir, 'create_shortcut.vbs')
with open(vbs_file, 'w') as f:
    f.write(vbs)
subprocess.run(['cscript', '//nologo', vbs_file], check=True)
if os.path.exists(vbs_file):
    os.remove(vbs_file)
print('Desktop shortcut updated successfully with custom icon!')
