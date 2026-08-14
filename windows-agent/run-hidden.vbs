Set WshShell = CreateObject("WScript.Shell")
strScriptPath = WshShell.CurrentDirectory & "\SecureWolAgent.py"
WshShell.Run "python """ & strScriptPath & """", 0, False
