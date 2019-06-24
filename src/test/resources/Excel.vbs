Set xl = CreateObject("Excel.Application")
Set wb = xl.Workbooks.Open(CreateObject("Scripting.FileSystemObject").GetFolder(".") & "\sheet.xlsx", 0, True) 
xl.DisplayAlerts = False

'' Can also use name of sheet:
'wb.Sheets("Feuil1").Range("A1").Value = 123
wb.Worksheets(1).Range("A1").Value = 123

'' In case a macro needs to be executed
' xl.Application.Run(f & "!macro") 

'' Not needed in general
'wb.RefreshAll
WScript.StdOut.WriteLine("z=" & wb.Worksheets(1).Range("A3").Value)

wb.Close False
xl.Quit
Set wb = Nothing
Set xl = Nothing
